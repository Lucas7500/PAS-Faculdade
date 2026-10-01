package br.ufg.pas.pedido.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia do RabbitMQ. A declaração é idempotente e idêntica nos serviços que
 * usam cada fila, de modo que qualquer um deles pode subir primeiro.
 *
 * pedidos.exchange    --pedido.criado-->        pedido.criado        (consumida pelo Pagamento)
 * pagamentos.exchange --pagamento.processado--> pagamento.processado (consumida pelo Pedido)
 * dlx.exchange        --*.dlq-->                filas *.dlq          (mensagens que falharam definitivamente)
 */
@Configuration
public class RabbitConfig {

    public static final String PEDIDOS_EXCHANGE = "pedidos.exchange";
    public static final String PEDIDO_CRIADO_QUEUE = "pedido.criado";
    public static final String PEDIDO_CRIADO_ROUTING_KEY = "pedido.criado";

    public static final String PAGAMENTOS_EXCHANGE = "pagamentos.exchange";
    public static final String PAGAMENTO_PROCESSADO_QUEUE = "pagamento.processado";
    public static final String PAGAMENTO_PROCESSADO_ROUTING_KEY = "pagamento.processado";

    public static final String DLX_EXCHANGE = "dlx.exchange";

    // ---------- pedido.criado (publicado por este serviço) ----------

    @Bean
    DirectExchange pedidosExchange() {
        return new DirectExchange(PEDIDOS_EXCHANGE, true, false);
    }

    @Bean
    Queue pedidoCriadoQueue() {
        return QueueBuilder.durable(PEDIDO_CRIADO_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(PEDIDO_CRIADO_QUEUE + ".dlq")
                .build();
    }

    @Bean
    Binding pedidoCriadoBinding() {
        return BindingBuilder.bind(pedidoCriadoQueue()).to(pedidosExchange()).with(PEDIDO_CRIADO_ROUTING_KEY);
    }

    // ---------- pagamento.processado (consumido por este serviço) ----------

    @Bean
    DirectExchange pagamentosExchange() {
        return new DirectExchange(PAGAMENTOS_EXCHANGE, true, false);
    }

    @Bean
    Queue pagamentoProcessadoQueue() {
        return QueueBuilder.durable(PAGAMENTO_PROCESSADO_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(PAGAMENTO_PROCESSADO_QUEUE + ".dlq")
                .build();
    }

    @Bean
    Binding pagamentoProcessadoBinding() {
        return BindingBuilder.bind(pagamentoProcessadoQueue()).to(pagamentosExchange())
                .with(PAGAMENTO_PROCESSADO_ROUTING_KEY);
    }

    // ---------- Dead Letter ----------

    @Bean
    DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    Queue pedidoCriadoDlq() {
        return QueueBuilder.durable(PEDIDO_CRIADO_QUEUE + ".dlq").build();
    }

    @Bean
    Binding pedidoCriadoDlqBinding() {
        return BindingBuilder.bind(pedidoCriadoDlq()).to(dlxExchange()).with(PEDIDO_CRIADO_QUEUE + ".dlq");
    }

    @Bean
    Queue pagamentoProcessadoDlq() {
        return QueueBuilder.durable(PAGAMENTO_PROCESSADO_QUEUE + ".dlq").build();
    }

    @Bean
    Binding pagamentoProcessadoDlqBinding() {
        return BindingBuilder.bind(pagamentoProcessadoDlq()).to(dlxExchange())
                .with(PAGAMENTO_PROCESSADO_QUEUE + ".dlq");
    }

    // ---------- Serialização JSON ----------

    @Bean
    MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        // Ignora o cabeçalho __TypeId__ (nome da classe do produtor) e usa o tipo do
        // parâmetro do @RabbitListener: os serviços não compartilham classes Java.
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }
}
