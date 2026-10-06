# Laboratório Avaliativo Prático: Arquitetura de Microsserviços

**Disciplina:** Padrões de Arquitetura de Software

**Dupla**

| Nome | Matrícula |
|---|---|
| Lucas Moreira Igleisas | 202400421 |
| Libna Raffaely de Jesus Costa | 202302617 |

As evidências textuais deste relatório são saídas reais de execuções do sistema. Os ids de correlação e os horários mudam a cada execução.

---

## Parte 1. Diagrama da arquitetura
![](/docs/diagramas/diagrama_arquitetura.png)


**Fluxo de um pedido**
![](/docs/diagramas/diagrama_fluxo_pedido.png)


**Isolamento dos dados.** Cada serviço tem o seu próprio PostgreSQL, com usuário e senha próprios,
e só recebe a URL do próprio banco no `docker-compose.yml`. O Pedido conhece o Estoque apenas pela
URL HTTP e conhece o Pagamento apenas pelo nome do exchange. O Pagamento não conhece o Pedido: ele
devolve o resultado por um evento.

| Demonstração pedida | Como é garantida |
|---|---|
| Cada serviço tem seu banco | `estoque-db`, `pedido-db` e `pagamento-db`, com volumes separados |
| Pedido não acessa o banco do Estoque | o Pedido só recebe `ESTOQUE_SERVICE_URL`, e o banco do Estoque não tem porta publicada |
| Pagamento não acessa o banco do Pedido | o Pagamento só recebe credenciais do `pagamento-db` e atualiza o pedido via `pagamento.processado` |
| Comunicação só pelas interfaces definidas | REST entre Pedido e Estoque, RabbitMQ entre Pedido e Pagamento |

### Topologia do RabbitMQ

| Exchange (direct, durável) | Routing key | Fila durável | Produtor | Consumidor |
|---|---|---|---|---|
| `pedidos.exchange` | `pedido.criado` | `pedido.criado` | Pedido | Pagamento |
| `pagamentos.exchange` | `pagamento.processado` | `pagamento.processado` | Pagamento | Pedido |
| `dlx.exchange` | `pedido.criado.dlq` | `pedido.criado.dlq` | broker | operador |
| `dlx.exchange` | `pagamento.processado.dlq` | `pagamento.processado.dlq` | broker | operador |

### Modificações em relação ao enunciado

O enunciado permite modificações para cobrir lacunas. As adotadas foram estas:

1. **Endpoint de compensação** `PUT /produtos/{id}/liberar` no Estoque, usado pela Saga do Pedido.
2. **Parâmetros de experimento** `simularFalha` e `compensar` no `POST /pedidos`, para a Etapa 3.
3. **Reserva atômica.** A baixa usa `UPDATE ... WHERE quantidade >= :qtd`, então o estoque nunca fica negativo, mesmo com requisições concorrentes ou várias instâncias.
4. **Dead Letter Queue e retentativas.** Um consumidor tenta 3 vezes. Depois disso a mensagem vai para a fila `.dlq`, em vez de ser descartada ou reentregue sem fim.
5. **Idempotência.** O Pagamento tem índice único em `pedido_id` e não cobra duas vezes o mesmo pedido. O Pedido ignora resultados para pedidos que já saíram de `AGUARDANDO_PAGAMENTO`.
6. **`prefetch: 1`** nos consumidores, para dividir as mensagens de forma justa entre instâncias.
7. **Healthcheck do RabbitMQ.** O teste do enunciado às vezes derrubava o broker com `eacces` no `.erlang.cookie`, porque o healthcheck roda como root e podia criar o arquivo antes do servidor. Ele também aprovava o broker antes de a porta 5672 aceitar conexões. O novo teste espera o cookie existir e usa `check_port_connectivity`.
8. **`hostname` fixo e volume para o RabbitMQ.** Sem isso, recriar o container trocava o nome do nó e as mensagens pendentes sumiam.
9. **Porta do Pedido configurável** com `PEDIDO_PORT`, mantendo 8080 como padrão.
10. A mensagem de 404 foi padronizada como `Produto inexistente`. O `?` do enunciado foi tratado como erro de digitação.

## Parte 2. Código-fonte

| Serviço | Pasta | Principais classes |
|---|---|---|
| Estoque | `estoque-service/` | `ProdutoController`, `EstoqueService`, `ProdutoRepository`, `CorrelationIdFilter` |
| Pedido | `pedido-service/` | `PedidoController`, `PedidoService` (Saga), `EstoqueClient` (RestTemplate), `PedidoEventPublisher`, `PagamentoProcessadoListener`, `RabbitConfig`, `CorrelationIdFilter` |
| Pagamento | `pagamento-service/` | `PedidoCriadoListener` (`@RabbitListener`), `PagamentoService`, `PagamentoEventPublisher`, `RabbitConfig` |

Cada serviço tem o próprio `pom.xml`, `Dockerfile` (build multi-stage) e `schema.sql` com o modelo
de dados do enunciado. Os serviços não compartilham código: o contrato entre eles é o JSON dos
eventos e das requisições HTTP.

## Parte 3. docker-compose.yml

O arquivo completo está em `docker-compose.yml`, na raiz do projeto. Ele segue a configuração do
enunciado, com a indentação corrigida e os itens 7, 8 e 9 da lista de modificações.

## Parte 4. Prints

### Print 1: Criação do pedido
![](/docs/prints/1_criacao_pedidos.png)

### Print 2: Reserva de estoque

#### Antes:
![](/docs/prints/2_reserva_estoque_antes.png)

#### Depois:
![](/docs/prints/2_reserva_estoque_depois.png)

### Print 3: Publicação da mensagem
![](/docs/prints/3_publicacao_mensagem_rabbitmq.png)

### Print 4: Processamento do pagamento
![](/docs/prints/4_processamento_pagamento.png)

## Parte 5. Respostas

### Etapa 1. Estoque Service

Os três cenários da regra de reserva foram verificados:

```text
PUT /produtos/2/reservar  {"quantidade":1}     -> 200 {"id":2,"nome":"Mouse","quantidade":49}
PUT /produtos/1/reservar  {"quantidade":9999}  -> 409 {"mensagem":"Estoque insuficiente"}
PUT /produtos/99/reservar {"quantidade":1}     -> 404 {"mensagem":"Produto inexistente"}
```

Nos casos 409 e 404 o estoque não muda.

### Etapa 3. Integração REST e experimento de consistência

Um pedido sem estoque devolve 409, não cria pedido e não publica evento. O log mostra apenas
`Estoque insuficiente`, sem `Pedido criado` nem `Evento publicado`.

A falha foi simulada com `POST /pedidos?simularFalha=true`. O serviço lança uma exceção depois que
o Estoque confirma a reserva e antes de gravar o pedido.

```text
Teclado antes: 20
Falha SEM compensação -> HTTP 500 | Teclado: 19   pedidos novos: 0
Falha COM compensação -> HTTP 500 | Teclado: 19 -> 18 -> 19 (reserva desfeita)
```

**1. O que aconteceu com o estoque?**
A quantidade caiu de 20 para 19 e ficou assim. A reserva foi confirmada no banco do Estoque antes da falha, e nada a desfez.

**2. O pedido foi criado?**
Não. O cliente recebeu HTTP 500 e `GET /pedidos` não mostra pedido novo. O resultado é uma inconsistência: uma unidade reservada sem pedido correspondente.

**3. Existe uma transação única envolvendo os dois serviços?**
Não. Cada serviço tem o seu banco e faz commit da sua transação local. A chamada REST confirma a reserva no `estoque-db` de forma independente da gravação no `pedido-db`. Por isso o método `criar` do Pedido não é `@Transactional`: não existe transação que abranja os dois bancos. Uma transação distribuída, como o two-phase commit, acoplaria os serviços e reduziria a disponibilidade, por isso não é usada em microsserviços.

**4. Como o sistema poderia desfazer a reserva realizada?**
Executando uma operação de compensação, que é a ação inversa da reserva. Aqui ela é o `PUT /produtos/{id}/liberar`, que devolve a quantidade. O Pedido Service a chama sempre que a gravação do pedido falha depois da reserva. No experimento, ela é ativada com `compensar=true`, e o log registra `Compensação executada: reserva do produto 3 desfeita`.

**5. Que mecanismo poderia ser utilizado para realizar essa compensação?**
O padrão **Saga**: uma sequência de transações locais em que cada passo tem uma transação compensatória. Há duas variantes:

- **Orquestração.** Um coordenador, aqui o Pedido Service, chama os passos e as compensações. É a variante implementada.
- **Coreografia.** Os serviços reagem a eventos. Por exemplo, um evento `pedido.falhou` faria o Estoque liberar a reserva.

Para a compensação não se perder se o próprio Pedido cair, ela pode ser registrada no banco e enviada pelo padrão **Transactional Outbox**. Outra proteção é dar prazo de expiração às reservas.

### Etapa 4. Docker Compose

**O Pedido consegue acessar o Estoque?**
Sim. O Pedido chama `http://estoque-service:8080`, nome resolvido pelo DNS da rede do Compose. A evidência é o mesmo correlationId nos dois logs:

```text
[pedido-service]  correlationId=968ce8a2-… Solicitação de pedido recebida: produto 1 quantidade 2
[estoque-service] correlationId=968ce8a2-… Produto 1 reservado
```

### Etapa 5. RabbitMQ

Na interface de gerenciamento aparecem o exchange `pedidos.exchange`, do tipo direct e durável, e a fila `pedido.criado` ligada a ele pela routing key `pedido.criado`. Com o sistema normal, a fila fica com 0 mensagens prontas e 1 consumidor. Com o Pagamento parado, ela acumula mensagens e mostra 0 consumidores. Com o Pagamento escalado, mostra 2 consumidores.

**1. Por que o Pedido Service publica em um Exchange em vez de enviar diretamente para uma Queue?**
Para desacoplar o produtor dos consumidores. O Pedido só conhece o exchange e a routing key. Quais filas recebem a mensagem é decidido pelos bindings no broker. Assim, um novo interessado, como um serviço de notificação ou de nota fiscal, pode criar a própria fila e ligá-la ao exchange sem nenhuma alteração no Pedido. O exchange também permite trocar a regra de roteamento, entregar a várias filas e usar dead-lettering.

**2. Qual é a diferença entre Exchange, Queue e Consumer?**

- **Exchange**: Ponto de entrada, recebe a mensagem e a encaminha para filas conforme o tipo e os bindings. Não armazena nada.
- **Queue**: Buffer, guarda as mensagens, de forma durável aqui, até que um consumidor as receba e confirme.
- **Consumer**: Aplicação que assina uma fila, processa cada mensagem e envia o ack. Aqui é o `@RabbitListener` do Pagamento.

**3. O Pedido Service sabe quem consumirá o evento?**
Não. Ele não conhece o Pagamento, nem quantas instâncias existem, nem se há alguém consumindo no momento. Isso fica evidente na Etapa 8: o Pedido publica normalmente com o Pagamento desligado.

### Etapa 7. Teste funcional

| Verificação | Evidência |
|---|---|
| Estoque atualizado | Notebook passou de 10 para 8 após pedido de 2 unidades |
| Pedido com `AGUARDANDO_PAGAMENTO` | resposta 201 `{"id":1,"produtoId":1,"quantidade":2,"status":"AGUARDANDO_PAGAMENTO"}` |
| Evento publicado | log `Evento publicado 1` no pedido-service |
| Evento consumido | log `Evento pedido.criado recebido 1` no pagamento-service |
| Pagamento no banco do Pagamento | `SELECT * FROM pagamento` mostra `1 | 1 | APROVADO` |
| Resultado no log | `Pagamento aprovado 1` |

### Etapa 8. Simulação de falha

Com `docker compose stop pagamento-service`, foram criados os pedidos 2, 3 e 4.

```text
POST /pedidos -> 201 {"id":2,...,"status":"AGUARDANDO_PAGAMENTO"}
POST /pedidos -> 201 {"id":3,...,"status":"AGUARDANDO_PAGAMENTO"}
POST /pedidos -> 201 {"id":4,...,"status":"AGUARDANDO_PAGAMENTO"}

rabbitmqctl list_queues
name            messages_ready  messages_unacknowledged  consumers
pedido.criado   3               0                        0
```

**1. O pedido foi criado?**
Sim. Os três pedidos receberam 201 com status `AGUARDANDO_PAGAMENTO`.

**2. O estoque foi atualizado?**
Sim. O Mouse passou de 49 para 46, porque a reserva depende só do Estoque, que estava no ar.

**3. O sistema inteiro parou?**
Não. Apenas o processamento de pagamentos ficou pausado. Pedido e Estoque continuaram respondendo, porque a dependência do Pagamento é assíncrona.

**4. A mensagem foi perdida?**
Não. A fila `pedido.criado` mostra 3 mensagens prontas e 0 consumidores. O log do Pedido mostra `Evento publicado 2`, `3` e `4`, e o log do Pagamento não tem nenhuma linha para esses pedidos. A fila é durável e as mensagens são persistentes, então elas aguardam o consumidor.

### Etapa 9. Recuperação

Com `docker compose start pagamento-service`:

```text
Antes:  pedido.criado  prontas=3  consumidores=0
Depois: pedido.criado  prontas=0  consumidores=1

[pagamento-service] … Evento pedido.criado recebido 2 … Pagamento aprovado 2
[pagamento-service] … Evento pedido.criado recebido 3 … Pagamento aprovado 3
[pagamento-service] … Evento pedido.criado recebido 4 … Pagamento aprovado 4

pagamento: (2,APROVADO) (3,APROVADO) (4,APROVADO)
pedidos 2, 3 e 4: PAGO
```

As mensagens permaneceram na fila, foram consumidas na reinicialização, cada pedido foi processado
uma vez e os registros estão no banco do Pagamento.

**1. O processamento precisou ser repetido manualmente?**
Não. Ao reconectar, o consumidor recebeu sozinho as mensagens pendentes.

**2. O Pedido Service precisou aguardar o Pagamento Service?**
Não. Ele respondeu 201 na hora, mesmo com o Pagamento fora do ar. Esse é o desacoplamento temporal da comunicação assíncrona.

**3. O que aconteceu com as mensagens enquanto o consumidor estava indisponível?**
Ficaram armazenadas na fila durável `pedido.criado`, no estado Ready. Mensagens persistentes também sobrevivem a um reinício do broker. Como o ack só é enviado depois do processamento, uma mensagem só sai da fila quando o pagamento já foi gravado.

### Etapa 10. Escalabilidade

Com `docker compose up --scale pagamento-service=2` e 10 pedidos novos:

```text
pagamento-service-1 | 18:26:03.363 Pagamento aprovado 15
pagamento-service-2 | 18:26:03.487 Pagamento rejeitado 16
pagamento-service-1 | 18:26:03.899 Pagamento aprovado 17
pagamento-service-2 | 18:26:04.006 Pagamento aprovado 18
pagamento-service-1 | 18:26:04.421 Pagamento rejeitado 19
pagamento-service-2 | 18:26:04.521 Pagamento rejeitado 20
pagamento-service-1 | 18:26:04.939 Pagamento aprovado 21
pagamento-service-2 | 18:26:05.039 Pagamento aprovado 22
pagamento-service-1 | 18:26:05.458 Pagamento aprovado 23
pagamento-service-2 | 18:26:05.552 Pagamento aprovado 24

instância 1: 5 mensagens   instância 2: 5 mensagens
pagamento: 24 registros para 24 pedidos distintos
```

**As mensagens foram distribuídas?**
Sim. O RabbitMQ entregou em rodízio, alternando entre as instâncias, 5 para cada. O `prefetch: 1` faz cada instância receber uma mensagem por vez, o que mantém a divisão justa.

**Apenas uma instância processou cada mensagem?**
Sim. Os consumidores de uma mesma fila competem entre si, e cada mensagem é entregue a um só deles. O banco tem 24 pagamentos para 24 pedidos distintos, sem duplicatas. O índice único em `pedido_id` protege também contra reentregas.

**Que características permitem escalar só o Pagamento?**

- **Serviço sem estado**: Nenhuma instância guarda dados em memória entre mensagens. O estado fica no `pagamento-db`.
- **Comunicação assíncrona por fila**: As instâncias se conectam à mesma fila e o broker reparte o trabalho. Ninguém precisa saber quantas instâncias existem.
- **Implantação independente**: Cada serviço tem imagem, configuração e banco próprios, e escalar um não afeta os outros.
- **Sem porta publicada no host**: O Pagamento usa `expose` em vez de `ports`, então réplicas não disputam a mesma porta.
- **Idempotência e reserva atômica**: Instâncias concorrentes não corrompem os dados.

**Em quais circunstâncias o Estoque também precisaria ser escalado?**
Quando ele vira gargalo da parte síncrona. Cada `POST /pedidos` espera a resposta do Estoque, então um pico de pedidos, como em uma Black Friday, ou um aumento de consultas a `/produtos` aumenta a latência do Estoque e pode estourar o timeout do Pedido. Isso também acontece quando o Pedido é escalado, porque a carga sobre o Estoque cresce na mesma proporção. Outro motivo é disponibilidade: com uma única instância, a queda do Estoque impede a criação de pedidos. A reserva é atômica no banco, então várias instâncias do Estoque são seguras. Seriam necessários um balanceador de carga e a remoção da porta fixa.

### Etapa 11. Observabilidade e investigação de incidente

O Pedido Service gera um `UUID.randomUUID()` por requisição. Ele vai no cabeçalho `X-Correlation-Id` para o Estoque e no campo `correlationId` dos eventos para o Pagamento, que o repassa no evento `pagamento.processado`. Todos os logs pedidos no enunciado foram implementados no formato `correlationId={} …`.

**Reprodução do incidente.** O Pagamento foi iniciado com `SIMULACAO_FALHA_PEDIDO_ID=17`, que simula falha do gateway de pagamento para esse pedido, e foram criados 18 pedidos. O usuário relata que o Pedido #17 não foi concluído:

```text
GET /pedidos/17 -> {"id":17,"produtoId":2,"quantidade":1,"status":"AGUARDANDO_PAGAMENTO"}
```

O correlationId foi encontrado pelo pedidoId no log do Pedido e depois buscado nos outros serviços:

```text
$ docker compose logs pedido-service | grep "Pedido 17 criado"
18:56:49.051 [pedido-service] correlationId=b580787a-2482-4ca7-a4db-35ceddeda7be Pedido 17 criado

--- pedido-service
18:56:49.014 INFO  correlationId=b580787a-… Solicitação de pedido recebida: produto 2 quantidade 1
18:56:49.051 INFO  correlationId=b580787a-… Pedido 17 criado
18:56:49.053 INFO  correlationId=b580787a-… Evento publicado 17
--- estoque-service
18:56:49.032 INFO  correlationId=b580787a-… Produto 2 reservado
--- pagamento-service
18:56:56.728 INFO  correlationId=b580787a-… Evento pedido.criado recebido 17 (produto 2, quantidade 1)
18:56:56.729 ERROR correlationId=b580787a-… Falha ao processar pagamento 17: gateway de pagamento indisponível (simulação)
18:56:57.730 INFO  correlationId=b580787a-… Evento pedido.criado recebido 17 (produto 2, quantidade 1)
18:56:57.730 ERROR correlationId=b580787a-… Falha ao processar pagamento 17: gateway de pagamento indisponível (simulação)
18:56:58.727 INFO  correlationId=b580787a-… Evento pedido.criado recebido 17 (produto 2, quantidade 1)
18:56:58.728 ERROR correlationId=b580787a-… Falha ao processar pagamento 17: gateway de pagamento indisponível (simulação)
18:56:58.741 WARN  RejectAndDontRequeueRecoverer - Retries exhausted for message … correlationId=b580787a-…

$ rabbitmqctl list_queues
pedido.criado.dlq  1  0  0

$ rabbitmqadmin get queue=pedido.criado.dlq
routing_key=pedido.criado.dlq  exchange=dlx.exchange  redelivered=True
payload={"pedidoId":17,"produtoId":2,"quantidade":1,"correlationId":"b580787a-2482-4ca7-a4db-35ceddeda7be"}

$ SELECT count(*) FROM pagamento WHERE pedido_id = 17;
0
```

**1. O pedido foi criado?**
Sim. O log tem `Pedido 17 criado` e `GET /pedidos/17` devolve o pedido com `AGUARDANDO_PAGAMENTO`.

**2. O estoque foi reservado?**
Sim. O log do estoque-service tem `Produto 2 reservado` com o mesmo correlationId, 19 ms antes da criação do pedido.

**3. O evento pedido.criado foi publicado?**
Sim. O log tem `Evento publicado 17` no pedido-service.

**4. O Pagamento Service recebeu o evento?**
Sim, três vezes. São a entrega original e duas retentativas, todas com `Evento pedido.criado recebido 17`.

**5. O pagamento foi processado?**
Não. Não existe `Pagamento aprovado 17` nem `Pagamento rejeitado 17`, não há linha na tabela `pagamento` e o evento `pagamento.processado` não foi publicado. Por isso o Pedido nunca registrou `Pedido 17 atualizado`.

**6. Em qual etapa ocorreu o problema?**
No processamento do pagamento, dentro do Pagamento Service, depois de receber o evento e antes de gravar o pagamento. Criação do pedido, reserva e publicação funcionaram.

**7. Qual evidência nos logs permite identificar a etapa da falha?**
A última linha de sucesso da cadeia é `Evento pedido.criado recebido 17`. Logo depois vêm três linhas `ERROR … Falha ao processar pagamento 17` e o aviso `Retries exhausted`. No Pedido, a última linha é `Evento publicado 17`, e a atualização de status nunca aparece. Seguir o correlationId pelos três serviços mostra exatamente onde a cadeia parou.

**8. O que aconteceu com a mensagem no RabbitMQ?**
Ela não se perdeu. Depois de 3 tentativas, o consumidor a rejeitou sem recolocá-la na fila, e o broker a desviou via `dlx.exchange` para a `pedido.criado.dlq`, com o conteúdo e o correlationId intactos. A fila principal ficou livre, então os pedidos 18 em diante foram processados
normalmente.

**Resolução.** Corrigida a causa, isto é, com o Pagamento reiniciado sem a simulação, a mensagem foi
devolvida da DLQ ao exchange original com `./scripts/testes.sh reprocessar-dlq`:

```text
Reprocessando: {"pedidoId":17,"produtoId":2,"quantidade":1,"correlationId":"df7c2e89-…"}
[pagamento-service] correlationId=df7c2e89-… Evento pedido.criado recebido 17
[pagamento-service] correlationId=df7c2e89-… Pagamento aprovado 17
[pedido-service]    correlationId=df7c2e89-… Pedido 17 atualizado para PAGO (pagamento APROVADO)
GET /pedidos/17 -> {"id":17,...,"status":"PAGO"}
```

Esse trecho é de uma segunda execução, por isso o correlationId é outro.

### Etapa 12. Atualização assíncrona do pedido

O Pagamento publica `pagamento.processado` com `pedidoId`, `status` e `correlationId`. O Pedido consome o evento e atualiza o próprio banco: `APROVADO` vira `PAGO` e `REJEITADO` vira `REJEITADO`. O Pagamento nunca acessa o `pedido-db`.

Resultado com pagamentos aprovados e rejeitados, consultando cada banco pelo seu próprio serviço:

| pedido_id | pagamento-db | pedido-db |
|---|---|---|
| 15 | APROVADO | PAGO |
| 16 | REJEITADO | REJEITADO |
| 17 | APROVADO | PAGO |
| 18 | APROVADO | PAGO |
| 19 | REJEITADO | REJEITADO |
| 20 | REJEITADO | REJEITADO |

Depois do processamento, nenhum pedido ficou em `AGUARDANDO_PAGAMENTO`.

---

## Limitações conhecidas

- **Dupla escrita no Pedido.** O pedido é gravado e depois o evento é publicado. Se o RabbitMQ cair exatamente entre os dois passos, o pedido fica gravado sem evento, e o log registra `Falha ao publicar evento`. A solução de produção é o padrão Transactional Outbox.
- **Compensação síncrona.** Se o Estoque estiver fora do ar no momento da compensação, ela falha e fica registrada em log, sem nova tentativa automática. Uma Saga com outbox ou fila de compensação resolveria.