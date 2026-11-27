#!/bin/bash
# Envia o script para a EC2 via SSM, espera e mostra a saida com a conta mascarada.
# Uso: ssm-executar.sh <id-da-instancia> <arquivo-do-script> [argumentos...]
set -euo pipefail
ID_INSTANCIA="$1"
ARQUIVO="$2"
shift 2

# Os unicos argumentos aceitos pelo script remoto sao uma tag Git ou --tag-atual.
if [ "$#" -ne 1 ] || ! [[ "$1" =~ ^[0-9a-f]{7,40}$|^--tag-atual$ ]]; then
  echo "ERRO: argumento de implantacao invalido." >&2
  exit 2
fi

SCRIPT_B64="$(base64 -w0 "$ARQUIVO")"
COMANDO="f=\$(mktemp); echo ${SCRIPT_B64} | base64 -d > \"\$f\"; bash \"\$f\" $1; rc=\$?; rm -f \"\$f\"; exit \$rc"

JSON="$(mktemp)"
trap 'rm -f "$JSON"' EXIT
jq -n --arg id "$ID_INSTANCIA" --arg cmd "$COMANDO" \
  '{InstanceIds:[$id],DocumentName:"AWS-RunShellScript",Parameters:{commands:[$cmd],executionTimeout:["600"]}}' > "$JSON"
ID_COMANDO="$(aws ssm send-command --cli-input-json "file://$JSON" --query 'Command.CommandId' --output text)"

STATUS="Pending"
for _ in $(seq 1 120); do
  sleep 5
  STATUS="$(aws ssm get-command-invocation --command-id "$ID_COMANDO" --instance-id "$ID_INSTANCIA" --query 'Status' --output text 2>/dev/null || echo Pending)"
  case "$STATUS" in Success|Failed|Cancelled|TimedOut) break ;; esac
done

echo "Estado do comando SSM: ${STATUS}"
aws ssm get-command-invocation --command-id "$ID_COMANDO" --instance-id "$ID_INSTANCIA" --query 'StandardOutputContent' --output text | sed -E 's/[0-9]{12}/XXXXXXXXXXXX/g'
echo "--- erros ---"
aws ssm get-command-invocation --command-id "$ID_COMANDO" --instance-id "$ID_INSTANCIA" --query 'StandardErrorContent' --output text | sed -E 's/[0-9]{12}/XXXXXXXXXXXX/g'
[ "$STATUS" = "Success" ]
