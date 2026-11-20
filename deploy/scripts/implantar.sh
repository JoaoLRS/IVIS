#!/bin/bash
# Troca a versao do IVIS na EC2 app. Executado via SSM pelo deploy.yml.
# Uso: bash implantar.sh <tag> (ou: bash implantar.sh --tag-atual)
set -euo pipefail
export HOME=/root
cd /opt/ivis

if [ "${1:-}" = "--tag-atual" ]; then
  echo "TAG_ATUAL=$(grep '^IVIS_TAG=' .env | cut -d= -f2)"
  exit 0
fi

TAG="${1:-}"
if ! [[ "$TAG" =~ ^[0-9a-f]{7,40}$ ]]; then
  echo "ERRO: tag invalida (esperado o hash curto do Git)." >&2
  exit 2
fi

TAG_ATUAL="$(grep '^IVIS_TAG=' .env | cut -d= -f2)"
REGISTRO="$(grep '^IVIS_ECR_REGISTRO=' .env | cut -d= -f2)"
echo "Tag atual: ${TAG_ATUAL} | tag nova: ${TAG}"

aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin "$REGISTRO" 2>&1 | tail -n 3
IVIS_TAG="$TAG" docker compose -f docker-compose.prod.yml pull 2>&1 | tail -n 20

cp -p .env ".env.bak-${TAG_ATUAL}"
sed -i "s/^IVIS_TAG=.*/IVIS_TAG=${TAG}/" .env
grep '^IVIS_TAG=' .env
docker compose -f docker-compose.prod.yml up -d 2>&1 | tail -n 15

ESTADO="ausente"
for _ in $(seq 1 40); do
  ESTADO="$(docker inspect ivis-backend --format '{{.State.Health.Status}}' 2>/dev/null || echo ausente)"
  if [ "$ESTADO" = "healthy" ]; then break; fi
  sleep 5
done
echo "Estado do backend: ${ESTADO}"
docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}'
[ "$ESTADO" = "healthy" ]
