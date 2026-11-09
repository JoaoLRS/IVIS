# Como contribuir com o IVIS

Este repositório é desenvolvido por quatro integrantes. Toda mudança passa por uma solicitação de mesclagem revisada por outra pessoa, para que o histórico mostre a participação de todos e para que cada decisão tenha um segundo par de olhos.

## Regras gerais

- **Nunca** envie alterações diretamente ao ramo principal (`main`). Ele é protegido e só recebe mudanças por solicitação de mesclagem.
- Cada integrante usa a **própria conta do GitHub**. Não compartilhe contas.
- Configure sua identidade com o mesmo e-mail cadastrado no GitHub:
  ```bash
  git config user.name "Seu Nome"
  git config user.email "seu-email@exemplo.com"
  ```
- O repositório oficial é `lucifer-son/IVIS`. Ao abrir uma solicitação de mesclagem, confira que o **destino** é `lucifer-son/IVIS` (ramo `main`) e não outro repositório.

## Fluxo de trabalho

1. Atualize o ramo principal local: `git switch main` e `git pull`.
2. Crie um ramo curto a partir dele: `git switch -c tipo/assunto-curto`.
3. Faça registros de alteração pequenos e focados, com mensagem no formato `tipo: descrição no imperativo`.
4. Antes de enviar, rode `gitleaks git . --redact` e confirme que não há achados.
5. Envie o ramo (`git push -u origin tipo/assunto-curto`) e abra a solicitação de mesclagem preenchendo o modelo.
6. Peça revisão a **outro integrante**. O autor não aprova a própria solicitação. É necessária **1 aprovação**, e todas as conversas precisam estar resolvidas.
7. Depois da aprovação, o autor faz a mesclagem usando a opção que cria um registro de mesclagem. O ramo é apagado automaticamente.

Mantenha os ramos curtos (alguns dias, no máximo) e as solicitações pequenas, com um único propósito. Se o ramo principal avançar durante o seu trabalho, traga as mudanças para o seu ramo (`git merge main`) antes de pedir revisão.

## Nomes de ramos e prefixos de registro

Formato: `tipo/assunto-curto`, em minúsculas, sem acento, com hífen e com até 4 ou 5 palavras. O assunto descreve **o que muda**.

| Tipo | Quando usar | Exemplo de ramo |
|---|---|---|
| `recurso` | funcionalidade nova na aplicação | `recurso/metricas-prometheus` |
| `correcao` | correção de erro | `correcao/cors-origem-https` |
| `infra` | Terraform e recursos da AWS | `infra/rede-e-grupos-de-seguranca` |
| `pipeline` | automação de build, testes e implantação | `pipeline/testes-do-backend` |
| `doc` | documentação, ADRs, proposta | `doc/adr-0012-hospedagem-aws` |
| `teste` | testes automatizados | `teste/verificacao-pos-implantacao` |
| `manutencao` | ajustes sem efeito funcional | `manutencao/gitignore-terraform` |

Os mesmos tipos valem como prefixo das mensagens de registro, por exemplo `pipeline: adiciona etapa de testes do backend`.

## Segredos

- Nunca registre senhas, tokens, chaves ou certificados privados. Arquivos `.env` são ignorados pelo Git, e os `.env.example` ficam com valores vazios ou fictícios.
- Na infraestrutura, nunca registre `*.tfstate*` nem `*.tfvars`. O estado do Terraform fica em armazenamento remoto e os segredos ficam no gerenciador de segredos do projeto.
- Se um segredo vazar, **revogue-o primeiro** e avise o grupo. Não reescreva o histórico: o segredo já deve ser considerado exposto.

## Revisão

Quem revisa confere se a solicitação faz o que descreve, se não inclui segredos nem arquivos que não pertencem ao repositório, se a documentação acompanha a mudança e se os testes (quando houver automação) passam. Os comentários devem ser objetivos e justificados.