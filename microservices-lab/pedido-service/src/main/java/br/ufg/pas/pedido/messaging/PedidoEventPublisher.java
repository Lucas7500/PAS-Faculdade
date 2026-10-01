package br.ufg.pas.pedido.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PedidoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public PedidoEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publica no Exchange (e não diretamente na fila): o Pedido Service não sabe
     * quem consumirá o evento; o roteamento é responsabilidade do broker.
     */
    public void publicarPedidoCriado(PedidoEvento evento) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.PEDIDOS_EXCHANGE,
                RabbitConfig.PEDIDO_CRIADO_ROUTING_KEY,
                evento,
                message -> {
                    // correlationId também nas propriedades AMQP (visível na UI do RabbitMQ)
                    message.getMessageProperties().setCorrelationId(evento.correlationId());
                    message.getMessageProperties().setHeader("correlationId", evento.correlationId());
                    return message;
                });

        log.info("correlationId={} Evento publicado {}", evento.correlationId(), evento.pedidoId());
    }
}
