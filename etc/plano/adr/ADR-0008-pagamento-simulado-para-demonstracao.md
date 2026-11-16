# ADR-0008 — Pagamento simulado para demonstração

- **Status:*Aprovado**
- **Data:*17/10/2026**
- **Decisores:** Equipe IVIS
- **Relacionados:** [ADR-0002](ADR-0002-limites-e-responsabilidades-dos-modulos.md), [ADR-0003](ADR-0003-comunicacao-entre-modulos.md), [ADR-0004](ADR-0004-stack-do-backend-e-persistencia.md), [ADR-0009](ADR-0009-backend-como-autoridade-de-negocio.md)

## Contexto

O IVIS precisa demonstrar contratação de planos em uma atividade universitária. A apresentação pode ocorrer em uma instância publicada; portanto, não se pode depender de uma interface local nem realizar cobranças reais. O RF15 prevê o processamento do pagamento; o modo acadêmico usa somente dados fictícios no simulador do backend.

## Decisão

Implementar no backend um **simulador de pagamento** para o fluxo de apresentação. O cartão fictício padrão IVIS será `4242 4242 4242 4242`, validade `12/30` e CVV `123`; ele só é válido no simulador IVIS e nunca deve ser encaminhado a um processador real. O backend valida o cartão e determina o resultado simulado de aprovação ou recusa conforme as regras de demonstração. Nenhuma transação simulada autoriza uma cobrança real.

O backend seleciona e executa o modo de pagamento. A configuração do modo não pode ser ativada, desativada ou sobrescrita por parâmetros enviados pelo navegador. O frontend não escolhe o resultado nem pode declarar uma compra paga. O backend valida preço, plano, elegibilidade, valor e estado da contratação, registra a transação como **simulada** e só então aplica o estado de acesso correspondente.

O fluxo simulado estará disponível também na instância publicada para apresentação acadêmica. A interface deve identificar claramente ao usuário que a compra é simulada e que nenhum valor será cobrado. Os dados do cartão de teste são fictícios, usados somente para validar o fluxo, não persistidos como credenciais de pagamento e nunca aceitos por um processador real. Nenhum modo real de cobrança está decidido neste ADR; eventual gateway real exigirá configuração segura no servidor e decisão própria, sem fallback silencioso entre modos.

## Consequências

### Positivas

- A contratação pode ser apresentada ponta a ponta sem conta de adquirente nem cobrança financeira.
- O mesmo fluxo pode ser demonstrado em ambiente publicado, mantendo a decisão de pagamento no backend.
- A abstração de processamento deixa aberta a futura integração com gateway real, sem confundir simulação com confirmação financeira.

### Custos e riscos

- O sistema deve marcar claramente transações, pedidos e logs como simulados para não os confundir com pagamentos reais.
- A configuração publicada precisa impedir cobranças reais acidentais e impedir que o cliente altere o modo.
- O cartão de teste é dado público e fictício; o simulador não deve armazenar PAN/CVV nem encaminhar esses valores a serviços externos.
- Os cenários de aprovação/recusa devem ser determinados pelo backend e documentados para apresentação, sem confiar em resultado indicado pelo cliente.

## Alternativas consideradas

- **Usar cartão real ou cobrar valor simbólico:** rejeitada para apresentação universitária.
- **Permitir que o frontend marque a compra como aprovada:** rejeitada por permitir falsificação de estado e contornar validações.
- **Usar apenas o sandbox de um gateway:** deixado como alternativa futura; a simulação própria não dependerá de credenciais/conta de provedor para a apresentação.
