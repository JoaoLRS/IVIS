#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

COMPOSE=(docker compose)
SERVICES=(ivis-banco ivis-api-ia ivis-backend)
ACTION="${1:-up}"
STARTUP_TIMEOUT_SECONDS="${IVIS_RELEASE_TIMEOUT_SECONDS:-300}"

usage() {
  cat <<'EOF'
Uso: bash release.sh [up|status|logs|restart|down|help]

  up       Cria os .env de exemplo se faltarem, compila e inicia banco, API de IA e backend.
  status   Mostra o estado dos containers.
  logs     Acompanha os logs dos três serviços.
  restart  Reinicia os três serviços e aguarda a prontidão.
  down     Para e remove os containers, preservando o volume do banco.
  help     Mostra esta ajuda.

Para aguardar mais tempo na inicialização, defina IVIS_RELEASE_TIMEOUT_SECONDS.
EOF
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    printf 'Erro: comando obrigatório não encontrado: %s\n' "$1" >&2
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
  "${COMPOSE[@]}" logs --tail=80 ivis-banco ivis-api-ia ivis-backend >&2
  return 1
}

wait_for_stack() {
  wait_for_url "API de IA" "http://127.0.0.1:8000/pronto"
  wait_for_url "backend e banco" "http://127.0.0.1:8081/api/v1/pronto"
  printf '\nAmbiente iniciado. API de IA: http://localhost:8000\nBackend: http://localhost:8081/api\n'
}

require_command docker

case "$ACTION" in
  up)
    require_command curl
    prepare_environment
    "${COMPOSE[@]}" config --quiet
    "${COMPOSE[@]}" up --build -d "${SERVICES[@]}"
    wait_for_stack
    ;;
  status)
    prepare_environment
    "${COMPOSE[@]}" ps
    ;;
  logs)
    prepare_environment
    "${COMPOSE[@]}" logs -f --tail=100 "${SERVICES[@]}"
    ;;
  restart)
    require_command curl
    prepare_environment
    "${COMPOSE[@]}" restart "${SERVICES[@]}"
    wait_for_stack
    ;;
  down)
    prepare_environment
    "${COMPOSE[@]}" down
    printf 'Containers parados. O volume ivis-banco-dados foi preservado.\n'
    ;;
  help|-h|--help)
    usage
    ;;
  *)
    usage >&2
    exit 2
    ;;
esac
