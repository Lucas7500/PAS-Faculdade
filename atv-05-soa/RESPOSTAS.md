### Aluno: Lucas Moreira Iglesias
### Matrícula: 202400421

## 4. Escolha uma decisão arquitetural do seu diagrama e justifique-a.

Decisão Escolhida: usar comunicação assíncrona baseada em eventos entre os Serviços de Pedidos com os Serviços de Pagamento e de Entrega, em vez de chamadas síncronas via REST.

Justificativas:
- Baixo acoplamento temporal: O Serviço de Pedidos não precisa que o Serviço de Pagamentos esteja disponível no instante da criação do pedido, apenas que a mensagem seja entregue eventualmente. Isso é a vantagem da comunicação assíncrona (fila/pub-sub) em relação ao request/reply.
- Resiliência: Falha ou lentidão do Serviço de Pagamento não bloqueia a criação do pedido nem derruba o Gateway em cascata, além de evitar uma possível falha em cadeia que pode ocorrer em serviços com acoplamento forte.

Trade-off: Consistência eventual e complexidade adicionada pela adição de uma camada de broker para lidar com comunicação assíncrona.

## 5. Considere que o serviço de pagamento fique temporariamente indisponível. Explique o que acontece no seu projeto.

Por conta do baixo acoplamento, em vista da utilização da comunicação assíncrona entre serviços via broker, a indisponibilidade do serviço de pagamento faz com que o broker comece a acumular eventos de sucesso na realização de pedidos, porém não impede ou falha a criação de pedidos, somente os coloca em um estado temporário de "processando pagamento" até que o serviço de pagamentos volte a funcionar e comece a processar os pagamentos. Pensando mais na etapa de design detalhado, poderiam ser adicionados mecanismos adicionais para tratamento desse caso, como exponential backoff, dead-letter queue, time-out de negócio, etc.