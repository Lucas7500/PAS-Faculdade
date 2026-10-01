package br.ufg.pas.pedido.service;

import br.ufg.pas.pedido.client.EstoqueClient;
import br.ufg.pas.pedido.dto.CriarPedidoRequest;
import br.ufg.pas.pedido.exception.FalhaCriacaoPedidoException;
import br.ufg.pas.pedido.exception.PedidoInexistenteException;
import br.ufg.pas.pedido.messaging.PagamentoProcessadoEvento;
import br.ufg.pas.pedido.messaging.PedidoEventPublisher;
import br.ufg.pas.pedido.messaging.PedidoEvento;
import br.ufg.pas.pedido.model.Pedido;
import br.ufg.pas.pedido.model.StatusPedido;
import br.ufg.pas.pedido.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final PedidoRepository repository;
    private final EstoqueClient estoqueClient;
    private final PedidoEventPublisher publisher;

    public PedidoService(PedidoRepository repository, EstoqueClient estoqueClient, PedidoEventPublisher publisher) {
        this.repository = repository;
        this.estoqueClient = estoqueClient;
        this.publisher = publisher;
    }

    /**
     * Fluxo da Etapa 3 (Saga orquestrada pelo Pedido Service):
     * 1. reservar estoque (REST, síncrono);
     * 2. criar o pedido com status AGUARDANDO_PAGAMENTO (banco do Pedido);
     * 3. publicar o evento pedido.criado (RabbitMQ, assíncrono).
     *
     * Se não houver estoque, o EstoqueClient lança exceção já no passo 1: o pedido
     * NÃO é criado, o evento NÃO é publicado e o erro é devolvido ao cliente.
     *
     * Este método propositalmente NÃO é @Transactional: não existe transação que
     * englobe o banco do Estoque e o do Pedido. Cada passo confirma localmente.
     *
     * @param simularFalha experimento de consistência: falha após reservar e antes de criar
     * @param compensar    no experimento, se a reserva deve ser desfeita (compensação)
     */
    public Pedido criar(CriarPedidoRequest request, boolean simularFalha, boolean compensar) {
        String correlationId = correlationIdAtual();
        log.info("correlationId={} Solicitação de pedido recebida: produto {} quantidade {}",
                correlationId, request.produtoId(), request.quantidade());

        // Passo 1 - reserva no Estoque Service
        estoqueClient.reservar(request.produtoId(), request.quantidade(), correlationId);

        // Passo 2 - criação do pedido no banco do Pedido Service
        Pedido pedido;
        try {
            if (simularFalha) {
                throw new IllegalStateException("Falha simulada após a reserva e antes da criação do pedido");
            }
            pedido = repository.save(new Pedido(request.produtoId(), request.quantidade(),
                    StatusPedido.AGUARDANDO_PAGAMENTO));
        } catch (RuntimeException e) {
            // Falhas reais sempre compensam. No experimento, a compensação é opcional
            // para que a inconsistência possa ser observada.
            boolean deveCompensar = !simularFalha || compensar;
            log.error("correlationId={} Falha ao criar pedido após reservar o produto {}: {}",
                    correlationId, request.produtoId(), e.getMessage());
            if (deveCompensar) {
                compensarReserva(request, correlationId);
            } else {
                log.warn("correlationId={} Compensação DESABILITADA: {} unidade(s) do produto {} "
                        + "continuam reservadas sem pedido correspondente",
                        correlationId, request.quantidade(), request.produtoId());
            }
            throw new FalhaCriacaoPedidoException(e.getMessage(), deveCompensar, e);
        }
        log.info("correlationId={} Pedido {} criado", correlationId, pedido.getId());

        // Passo 3 - publicação do evento
        PedidoEvento evento = new PedidoEvento(pedido.getId(), pedido.getProdutoId(),
                pedido.getQuantidade(), correlationId);
        try {
            publisher.publicarPedidoCriado(evento);
        } catch (AmqpException e) {
            // O pedido já foi gravado; o evento se perdeu (problema de "dual write").
            // Em produção resolveríamos com o padrão Transactional Outbox.
            log.error("correlationId={} Falha ao publicar evento do pedido {}: {}",
                    correlationId, pedido.getId(), e.getMessage());
        }
        return pedido;
    }

    private void compensarReserva(CriarPedidoRequest request, String correlationId) {
        try {
            estoqueClient.liberar(request.produtoId(), request.quantidade(), correlationId);
            log.info("correlationId={} Compensação executada: reserva do produto {} desfeita",
                    correlationId, request.produtoId());
        } catch (RuntimeException ex) {
            log.error("correlationId={} Compensação FALHOU para o produto {}: {}",
                    correlationId, request.produtoId(), ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<Pedido> listar() {
        return repository.findAll(Sort.by("id"));
    }

    @Transactional(readOnly = true)
    public Pedido buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new PedidoInexistenteException(id));
    }

    /** Etapa 12: aplica o resultado do pagamento (APROVADO -> PAGO, REJEITADO -> REJEITADO). */
    @Transactional
    public void atualizarStatusPagamento(PagamentoProcessadoEvento evento) {
        String correlationId = evento.correlationId();
        Pedido pedido = repository.findById(evento.pedidoId()).orElseThrow(() ->
                new AmqpRejectAndDontRequeueException("Pedido " + evento.pedidoId() + " inexistente"));

        StatusPedido novoStatus = switch (evento.status()) {
            case "APROVADO" -> StatusPedido.PAGO;
            case "REJEITADO" -> StatusPedido.REJEITADO;
            default -> throw new AmqpRejectAndDontRequeueException("Status desconhecido: " + evento.status());
        };

        if (!StatusPedido.AGUARDANDO_PAGAMENTO.name().equals(pedido.getStatus())) {
            // Idempotência: entrega duplicada (at-least-once) não altera um pedido já finalizado.
            log.warn("correlationId={} Pedido {} já está {}; evento ignorado",
                    correlationId, pedido.getId(), pedido.getStatus());
            return;
        }

        pedido.setStatus(novoStatus);
        log.info("correlationId={} Pedido {} atualizado para {} (pagamento {})",
                correlationId, pedido.getId(), novoStatus, evento.status());
    }

    private String correlationIdAtual() {
        String correlationId = MDC.get("correlationId");
        return correlationId != null ? correlationId : UUID.randomUUID().toString();
    }
}
