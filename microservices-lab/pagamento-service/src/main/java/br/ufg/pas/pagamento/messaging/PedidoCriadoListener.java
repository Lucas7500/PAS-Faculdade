package br.ufg.pas.pagamento.messaging;

import br.ufg.pas.pagamento.service.PagamentoService;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor da fila pedido.criado. O processamento ocorre independentemente de o
 * Pedido Service estar disponível: a única dependência é o broker.
 */
@Component
public class PedidoCriadoListener {

    private final PagamentoService pagamentoService;

    public PedidoCriadoListener(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @RabbitListener(queues = RabbitConfig.PEDIDO_CRIADO_QUEUE)
    public void consumir(PedidoEvento evento) {
        MDC.put("correlationId", evento.correlationId());
        try {
            pagamentoService.processar(evento);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
