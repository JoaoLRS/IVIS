# ADR-0009 — Backend como autoridade de negócio e segurança

- **Status:*Aprovado**
- **Data:*17/10/2026**
- **Decisores:** Equipe IVIS
- **Relacionados:** [ADR-0001](ADR-0001-arquitetura-modular.md), [ADR-0002](ADR-0002-limites-e-responsabilidades-dos-modulos.md), [ADR-0006](ADR-0006-frontend-web.md), [ADR-0008](ADR-0008-pagamento-simulado-para-demonstracao.md)

## Contexto

O navegador é executado em um dispositivo controlado pelo usuário. Qualquer dado enviado pelo frontend pode ser inspecionado e alterado; ocultar botões ou validar formulários no cliente não protege operações. O sistema precisa impedir que o cliente altere regras, privilégios ou estados como plano ativo e pagamento aprovado.

## Decisão

O backend é a **única autoridade** para regras de negócio e decisões de acesso. Toda operação deve autenticar e autorizar o ator, validar novamente as regras e os dados, e derivar do estado confiável do servidor os valores relevantes.

Em particular:

- preços, benefícios, capacidade de vagas, elegibilidade e modalidade de acesso são obtidos/validados no backend;
- o backend verifica propriedade/escopo institucional e plano vigente em cada operação protegida;
- mudanças de estado (contrato, vínculo, pagamento, histórico) só são aceitas após validação e persistência pelo backend;
- o cliente não pode enviar como autoridade o papel do usuário, valor final, resultado de pagamento ou flag de “simulação”; campos desse tipo são ignorados ou rejeitados quando não fizerem parte do contrato;
- validações do frontend são apenas conveniência/feedback de interface. Podem espelhar regras para usabilidade, mas nunca substituem validação no servidor;
- APIs devem rejeitar acesso não autorizado mesmo quando chamadas diretamente, sem passar pela interface.

Isso reduz a superfície de abuso, mas não é garantia isolada contra invasões. Autenticação, autorização, proteção de sessão, validação, gestão de segredos, limitação de abuso, auditoria e atualização de dependências continuam necessárias.

## Consequências

### Positivas

- Regras são consistentes entre diferentes clientes e não podem ser contornadas editando a interface.
- Cada endpoint pode aplicar autorização e validação no limite de confiança.
- O simulador de pagamento pode ser apresentado sem aceitar confirmação de compra fabricada no navegador.

### Custos e riscos

- Toda regra relevante precisa existir/testar-se no backend, aumentando seu escopo de implementação.
- O frontend pode precisar tratar erros de validação/autorização devolvidos pela API.
- Espelhar validações no cliente traz risco de divergência; validação oficial permanece sempre no servidor.

## Alternativas consideradas

- **Aplicar regras apenas na interface:** rejeitada porque chamadas podem ser feitas sem interface e dados do cliente podem ser adulterados.
- **Confiar em campos de papel, preço ou resultado enviados pelo cliente:** rejeitada porque o cliente não é uma fonte confiável.
