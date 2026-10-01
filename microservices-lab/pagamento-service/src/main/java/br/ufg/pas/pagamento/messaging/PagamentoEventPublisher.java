package br.ufg.pas.pagamento.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PagamentoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PagamentoEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public PagamentoEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** Etapa 12: informa o resultado sem acessar o banco do Pedido Service. */
    public void publicarPagamentoProcessado(PagamentoProcessadoEvento evento) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.PAGAMENTOS_EXCHANGE,
                RabbitConfig.PAGAMENTO_PROCESSADO_ROUTING_KEY,
                evento,
                message -> {
                    message.getMessageProperties().setCorrelationId(evento.correlationId());
                    message.getMessageProperties().setHeader("correlationId", evento.correlationId());
                    return message;
                });

        log.info("correlationId={} Evento pagamento.processado publicado {} ({})",
                evento.correlationId(), evento.pedidoId(), evento.status());
    }
}
