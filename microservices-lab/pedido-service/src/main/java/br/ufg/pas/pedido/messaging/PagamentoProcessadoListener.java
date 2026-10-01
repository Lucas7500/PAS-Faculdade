package br.ufg.pas.pedido.messaging;

import br.ufg.pas.pedido.service.PedidoService;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Etapa 12: consome pagamento.processado e atualiza o status do pedido.
 * O Pagamento Service nunca acessa o banco do Pedido; ele só publica um evento.
 */
@Component
public class PagamentoProcessadoListener {

    private final PedidoService pedidoService;

    public PagamentoProcessadoListener(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @RabbitListener(queues = RabbitConfig.PAGAMENTO_PROCESSADO_QUEUE)
    public void consumir(PagamentoProcessadoEvento evento) {
        MDC.put("correlationId", evento.correlationId());
        try {
            pedidoService.atualizarStatusPagamento(evento);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
