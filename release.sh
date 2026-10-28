#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

COMPOSE=(docker compose)
SERVICES=(ivis-banco ivis-mailpit ivis-api-ia ivis-backend ivis-frontend)
ACTION="${1:-up}"
STARTUP_TIMEOUT_SECONDS="${IVIS_RELEASE_TIMEOUT_SECONDS:-300}"

usage() {
  cat <<'EOF'
Uso: bash release.sh [up|status|logs|restart|down|deploy-vercel|help]

  up       Cria os .env de exemplo se faltarem, compila e inicia o sistema completo com Docker.
  status   Mostra o estado dos containers.
  logs     Acompanha os logs dos serviços do sistema.
  restart  Reinicia os serviços do sistema e aguarda a prontidão.
  down     Para e remove os containers, preservando o volume do banco.
  deploy-vercel Publica o frontend na Vercel (backend, IA e banco precisam estar publicados separadamente).
  help     Mostra esta ajuda.

Para aguardar mais tempo na inicialização, defina IVIS_RELEASE_TIMEOUT_SECONDS.
Para publicar na Vercel, instale e autentique o Vercel CLI e configure o projeto em frontend/.
EOF
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    printf 'Erro: comando obrigatório não encontrado: %s\n' "$1" >&2
    exit 1
  fi
}

require_docker_compose() {
  require_command docker
  if ! docker compose version >/dev/null 2>&1; then
    printf 'Erro: Docker Compose não está disponível. Atualize/inicie o Docker Desktop.\n' >&2
    exit 1
  fi
}

prepare_environment() {
  if [[ ! -f .env ]]; then
    cp .env.example .env
    printf 'Criei .env a partir de .env.example. Revise as configurações locais.\n'
  fi

  if [[ ! -f api-ia/.env ]]; then
    cp api-ia/.env.example api-ia/.env
    printf 'Criei api-ia/.env a partir de api-ia/.env.example. O stub fica ativo por padrão.\n'
  fi

  if ! grep -Eq '^IVIS_JWT_CHAVE_ASSINATURA=[^[:space:]]+' .env; then
    require_command openssl
    local jwt_key temporary_file line found=false
    jwt_key="$(openssl rand -base64 32 | tr -d '\r\n')"
    temporary_file="$(mktemp)"
    while IFS= read -r line || [[ -n "$line" ]]; do
      line="${line%$'\r'}"
      if [[ "$line" == IVIS_JWT_CHAVE_ASSINATURA=* ]]; then
        printf 'IVIS_JWT_CHAVE_ASSINATURA=%s\n' "$jwt_key" >> "$temporary_file"
        found=true
      else
        printf '%s\n' "$line" >> "$temporary_file"
      fi
    done < .env
    if [[ "$found" == false ]]; then
      printf 'IVIS_JWT_CHAVE_ASSINATURA=%s\n' "$jwt_key" >> "$temporary_file"
    fi
    mv "$temporary_file" .env
    printf 'Gerei uma chave JWT local em .env. O valor não será exibido.\n'
  fi
}

wait_for_url() {
  local label="$1"
  local url="$2"
  local attempt
  local attempts=$((STARTUP_TIMEOUT_SECONDS / 2))

  printf 'Aguardando %s em %s\n' "$label" "$url"
  for ((attempt = 1; attempt <= attempts; attempt++)); do
    if curl --silent --fail --max-time 3 "$url" >/dev/null 2>&1; then
      printf '%s está pronto.\n' "$label"
      return 0
    fi
    sleep 2
  done

  printf 'Erro: %s não ficou pronto em %s segundos.\n' "$label" "$STARTUP_TIMEOUT_SECONDS" >&2
  "${COMPOSE[@]}" ps
  "${COMPOSE[@]}" logs --tail=80 "${SERVICES[@]}" >&2
  return 1
}

wait_for_stack() {
  wait_for_url "API de IA" "http://127.0.0.1:8000/pronto"
  wait_for_url "backend e banco" "http://127.0.0.1:8081/api/v1/pronto"
  wait_for_url "frontend" "http://127.0.0.1:8080/"
  printf '\nAmbiente iniciado.\nFrontend: http://localhost:8080\nAPI de IA: http://localhost:8000\nBackend: http://localhost:8081/api\nMailpit: http://localhost:8025\n'
}

case "$ACTION" in
  up)
    require_docker_compose
    require_command curl
    prepare_environment
    "${COMPOSE[@]}" config --quiet
    "${COMPOSE[@]}" up --build -d "${SERVICES[@]}"
    wait_for_stack
    ;;
  status)
    require_docker_compose
    prepare_environment
    "${COMPOSE[@]}" ps
    ;;
  logs)
    require_docker_compose
    prepare_environment
    "${COMPOSE[@]}" logs -f --tail=100 "${SERVICES[@]}"
    ;;
  restart)
    require_docker_compose
    require_command curl
    prepare_environment
    "${COMPOSE[@]}" restart "${SERVICES[@]}"
    wait_for_stack
    ;;
  down)
    require_docker_compose
    prepare_environment
    "${COMPOSE[@]}" down
    printf 'Containers parados. O volume ivis-banco-dados foi preservado.\n'
    ;;
  deploy-vercel)
    require_command vercel
    vercel deploy --prod --cwd "$ROOT_DIR/frontend"
    ;;
  help|-h|--help)
    usage
    ;;
  *)
    usage >&2
    exit 2
    ;;
esac
