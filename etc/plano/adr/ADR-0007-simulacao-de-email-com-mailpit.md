# ADR-0007 — Simulação de envio de e-mail com Mailpit

- **Status:** Em andamento
- **Data:**
- **Decisores:** Equipe IVIS
- **Relacionados:** [ADR-0002](ADR-0002-limites-e-responsabilidades-dos-modulos.md), [ADR-0004](ADR-0004-stack-do-backend-e-persistencia.md)

## Contexto

Fluxos como recuperação de senha precisam demonstrar o envio de mensagens, mas o IVIS é um projeto universitário de apresentação e não requer entrega real de e-mail para demonstrar esses fluxos. A aplicação também não deve depender de um serviço de envio externo para a apresentação.

## Decisão

Utilizar **Mailpit** como servidor SMTP de captura no ambiente de desenvolvimento e apresentação. O backend enviará as mensagens pelo SMTP configurado; o Mailpit as captura para consulta em sua interface web e não as encaminha a destinatários reais.

Mailpit é ferramenta de simulação/captura, não provedor de e-mail de produção. Se o produto vier a enviar e-mails reais, essa integração dependerá de configuração explícita no backend e de decisão posterior. Endereços, tokens e credenciais usados em demonstrações devem ser fictícios e descartáveis. O acesso à interface do Mailpit deve ser controlado no ambiente publicado.

## Consequências

### Positivas

- É possível demonstrar cadastro/recuperação e inspecionar mensagens sem enviar e-mails a pessoas reais.
- A aplicação usa uma integração SMTP simples em vez de lógica especial de envio no navegador.
- Mensagens podem ser verificadas durante a apresentação sem depender de caixa postal externa.

### Custos e riscos

- A equipe precisa incluir Mailpit na infraestrutura e configurar SMTP/URL da interface por ambiente.
- Mensagens capturadas podem conter dados pessoais ou tokens; a instância e a caixa de mensagens precisam ser protegidas e limpas.
- Publicar a interface de captura sem controle pode expor links de redefinição e conteúdo das mensagens.

## Alternativas consideradas

- **Enviar e-mail real durante a apresentação:** não escolhido para evitar dependência externa e mensagens acidentais.
- **Simular o envio apenas no frontend:** rejeitado, pois não exercita o fluxo servidor-a-servidor e permitiria à interface afirmar uma operação que não ocorreu.
