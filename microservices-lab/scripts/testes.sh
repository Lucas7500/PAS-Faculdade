#!/usr/bin/env bash
# Roteiro de testes do laboratório (executar a partir da pasta microservices-lab).
# Uso: ./scripts/testes.sh <etapa>
#   estoque | funcional | consistencia | falha | recuperacao | escala | incidente [id] | reprocessar-dlq | status
set -euo pipefail

PEDIDO=http://localhost:${PEDIDO_PORT:-8080}
ESTOQUE=http://localhost:8081

titulo() { printf '\n==== %s ====\n' "$1"; }

criar_pedido() { # produtoId quantidade [query]
  curl -s -i -X POST "$PEDIDO/pedidos${3:-}" -H 'Content-Type: application/json' \
       -d "{\"produtoId\": $1, \"quantidade\": $2}" | grep -Ei '^(HTTP|X-Correlation-Id)|^\{|^\['
  echo
}

# rabbitmqctl lê o estado atual da fila (a API HTTP de gerenciamento só atualiza a cada ~5 s)
filas() {
  docker compose exec -T rabbitmq rabbitmqctl -q list_queues \
    name messages_ready messages_unacknowledged consumers | column -t
}

pagamentos_db() {
  docker compose exec -T pagamento-db psql -U pagamento -d pagamento -c \
    "SELECT id, pedido_id, status FROM pagamento ORDER BY id;"
}

case "${1:-}" in
  estoque)
    titulo "GET /produtos";            curl -s "$ESTOQUE/produtos"; echo
    titulo "GET /produtos/1";          curl -s "$ESTOQUE/produtos/1"; echo
    titulo "Reserva OK (200)";         curl -s -i -X PUT "$ESTOQUE/produtos/2/reservar" -H 'Content-Type: application/json' -d '{"quantidade":1}' | grep -E '^HTTP|^\{'; echo
    titulo "Estoque insuficiente (409)"; curl -s -i -X PUT "$ESTOQUE/produtos/1/reservar" -H 'Content-Type: application/json' -d '{"quantidade":9999}' | grep -E '^HTTP|^\{'; echo
    titulo "Produto inexistente (404)";  curl -s -i -X PUT "$ESTOQUE/produtos/99/reservar" -H 'Content-Type: application/json' -d '{"quantidade":1}' | grep -E '^HTTP|^\{'; echo
    ;;
  funcional)
    titulo "Estoque antes";            curl -s "$ESTOQUE/produtos/1"; echo
    titulo "POST /pedidos";            criar_pedido 1 2
    sleep 3
    titulo "Estoque depois";           curl -s "$ESTOQUE/produtos/1"; echo
    titulo "Pedidos";                  curl -s "$PEDIDO/pedidos"; echo
    titulo "Pagamentos (banco do Pagamento)"; pagamentos_db
    titulo "Pedido sem estoque (409, sem pedido e sem evento)"; criar_pedido 1 9999
    ;;
  consistencia)
    titulo "Estoque antes";            curl -s "$ESTOQUE/produtos/3"; echo
    titulo "Falha SEM compensação";    criar_pedido 3 1 '?simularFalha=true'
    titulo "Estoque após falha sem compensação"; curl -s "$ESTOQUE/produtos/3"; echo
    titulo "Falha COM compensação";    criar_pedido 3 1 '?simularFalha=true&compensar=true'
    titulo "Estoque após falha com compensação"; curl -s "$ESTOQUE/produtos/3"; echo
    titulo "Pedidos (nenhum novo pedido criado)"; curl -s "$PEDIDO/pedidos"; echo
    ;;
  falha)
    docker compose stop pagamento-service
    for i in 1 2 3; do criar_pedido 2 1; done
    sleep 2
    titulo "Filas (mensagens acumuladas, 0 consumidores)"; filas
    ;;
  recuperacao)
    titulo "Filas antes";              filas
    docker compose start pagamento-service
    echo "Aguardando o Pagamento Service consumir as mensagens pendentes..."; sleep 25
    titulo "Filas depois";             filas
    titulo "Pagamentos";               pagamentos_db
    titulo "Pedidos";                  curl -s "$PEDIDO/pedidos"; echo
    ;;
  escala)
    docker compose up -d --scale pagamento-service=2
    sleep 25
    for i in $(seq 1 10); do criar_pedido 2 1 >/dev/null; done
    sleep 10
    titulo "Distribuição entre instâncias"
    docker compose logs pagamento-service | grep -E 'Pagamento (aprovado|rejeitado)' | tail -10
    ;;
  incidente)
    # Sobe o Pagamento com falha simulada para o pedido informado (padrão 17)
    ID=${2:-17}
    SIMULACAO_FALHA_PEDIDO_ID=$ID docker compose up -d pagamento-service
    echo "Pagamento configurado para falhar no pedido $ID. Crie pedidos até atingir o id $ID."
    ;;
  reprocessar-dlq)
    # Corrigida a causa (Pagamento sem falha simulada), devolve as mensagens da
    # pedido.criado.dlq para o exchange original, para serem processadas de novo.
    SIMULACAO_FALHA_PEDIDO_ID= docker compose up -d pagamento-service
    sleep 20
    while :; do
      MSG=$(docker compose exec -T rabbitmq rabbitmqadmin -f raw_json get queue=pedido.criado.dlq \
              ackmode=ack_requeue_false count=1 | python -c 'import json,sys; m=json.load(sys.stdin); print(m[0]["payload"] if m else "")')
      [ -z "$MSG" ] && break
      echo "Reprocessando: $MSG"
      docker compose exec -T rabbitmq rabbitmqadmin publish exchange=pedidos.exchange routing_key=pedido.criado \
        properties='{"content_type":"application/json","delivery_mode":2}' payload="$MSG"
    done
    sleep 5
    titulo "Filas"; filas
    ;;
  status)
    titulo "Pedidos";  curl -s "$PEDIDO/pedidos"; echo
    titulo "Filas";    filas
    ;;
  *)
    echo "Uso: $0 {estoque|funcional|consistencia|falha|recuperacao|escala|incidente [id]|reprocessar-dlq|status}"
    exit 1
    ;;
esac
