# microservices-lab

Plataforma de e-commerce com três microsserviços independentes (Estoque, Pedido e Pagamento),
comunicação síncrona via REST, assíncrona via RabbitMQ e um banco PostgreSQL por serviço.

O relatório de entrega, com diagrama e respostas de todas as etapas, está em [RELATORIO.md](RELATORIO.md).

## Estrutura

```
microservices-lab
├── estoque-service      Spring Boot · banco estoque-db   · porta 8081
├── pedido-service       Spring Boot · banco pedido-db    · porta 8080
├── pagamento-service    Spring Boot · banco pagamento-db · sem porta publicada (escalável)
├── scripts/testes.sh    roteiro de testes com cURL para cada etapa
└── docker-compose.yml
```

Tecnologias: Java 17+ (imagens com JDK 21), Spring Boot 3.3, Spring Data JPA, Spring AMQP,
PostgreSQL 16, RabbitMQ 3.13 e Docker Compose. Não é preciso ter Java ou Maven instalados:
o build acontece dentro do Docker.

## Como executar

```bash
docker compose up --build
```

| Recurso | Endereço |
|---|---|
| Pedido Service | http://localhost:8080 |
| Estoque Service | http://localhost:8081 |
| RabbitMQ (gerenciamento) | http://localhost:15672 (guest / guest) |

Se a porta 8080 já estiver em uso na sua máquina (por exemplo, por um Apache/XAMPP), defina outra:

```bash
PEDIDO_PORT=8090 docker compose up --build          # bash
$env:PEDIDO_PORT=8090; docker compose up --build    # PowerShell
```

Para recomeçar do zero (apaga os bancos e as filas):

```bash
docker compose down -v
```

## Endpoints

### Estoque Service (8081)

| Método | Rota | Resposta |
|---|---|---|
| GET | `/produtos` | lista de produtos |
| GET | `/produtos/{id}` | produto, ou 404 `{"mensagem":"Produto inexistente"}` |
| PUT | `/produtos/{id}/reservar` body `{"quantidade":2}` | 200 / 409 `Estoque insuficiente` / 404 `Produto inexistente` |
| PUT | `/produtos/{id}/liberar` body `{"quantidade":2}` | compensação: devolve uma reserva |

### Pedido Service (8080)

| Método | Rota | Resposta |
|---|---|---|
| POST | `/pedidos` body `{"produtoId":1,"quantidade":2}` | 201 com status `AGUARDANDO_PAGAMENTO` |
| GET | `/pedidos` | lista de pedidos |
| GET | `/pedidos/{id}` | pedido, ou 404 |
| POST | `/pedidos?simularFalha=true` | experimento da Etapa 3, falha sem compensação |
| POST | `/pedidos?simularFalha=true&compensar=true` | experimento da Etapa 3, falha com compensação |

Toda resposta do Pedido Service traz o cabeçalho `X-Correlation-Id`.

## Roteiro de testes

O script usa bash e cURL. No Windows, rode pelo Git Bash, a partir desta pasta.

```bash
./scripts/testes.sh estoque          # Etapa 1: 200, 409 e 404
./scripts/testes.sh funcional        # Etapas 3, 7 e 12: fluxo completo
./scripts/testes.sh consistencia     # Etapa 3: falha após reservar, com e sem compensação
./scripts/testes.sh falha            # Etapa 8: para o Pagamento e cria pedidos
./scripts/testes.sh recuperacao      # Etapa 9: sobe o Pagamento e confere o consumo
./scripts/testes.sh escala           # Etapa 10: duas instâncias do Pagamento
./scripts/testes.sh incidente 17     # Etapa 11: Pagamento passa a falhar no pedido 17
./scripts/testes.sh reprocessar-dlq  # Etapa 11: corrige e reprocessa a DLQ
./scripts/testes.sh status           # pedidos e filas
```

Comandos úteis:

```bash
docker compose logs -f pedido-service estoque-service pagamento-service
docker compose logs pedido-service | grep "<correlationId>"
docker compose exec rabbitmq rabbitmqctl list_queues name messages_ready consumers
docker compose exec pedido-db    psql -U pedido    -d pedido    -c "SELECT * FROM pedido ORDER BY id;"
docker compose exec estoque-db   psql -U estoque   -d estoque   -c "SELECT * FROM produto ORDER BY id;"
docker compose exec pagamento-db psql -U pagamento -d pagamento -c "SELECT * FROM pagamento ORDER BY id;"
```

## Variáveis de ambiente

| Variável | Serviço | Padrão | Uso |
|---|---|---|---|
| `PEDIDO_PORT` | compose | `8080` | porta do host para o Pedido Service |
| `SIMULACAO_FALHA_PEDIDO_ID` | pagamento | vazio | pedido cujo pagamento falha, para a Etapa 11 |
| `PAGAMENTO_TAXA_APROVACAO` | pagamento | `0.8` | probabilidade de aprovação |
| `PAGAMENTO_TEMPO_PROCESSAMENTO_MS` | pagamento | `500` | atraso artificial, deixa visível a divisão entre instâncias |
