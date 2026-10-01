package br.ufg.pas.pagamento.service;

import br.ufg.pas.pagamento.messaging.PagamentoEventPublisher;
import br.ufg.pas.pagamento.messaging.PagamentoProcessadoEvento;
import br.ufg.pas.pagamento.messaging.PedidoEvento;
import br.ufg.pas.pagamento.model.Pagamento;
import br.ufg.pas.pagamento.model.StatusPagamento;
import br.ufg.pas.pagamento.repository.PagamentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PagamentoService {

    private static final Logger log = LoggerFactory.getLogger(PagamentoService.class);

    private final PagamentoRepository repository;
    private final PagamentoEventPublisher publisher;
    private final double taxaAprovacao;
    private final long tempoProcessamentoMs;
    private final String falharPedidoId;

    public PagamentoService(PagamentoRepository repository,
                            PagamentoEventPublisher publisher,
                            @Value("${pagamento.taxa-aprovacao}") double taxaAprovacao,
                            @Value("${pagamento.tempo-processamento-ms}") long tempoProcessamentoMs,
                            @Value("${pagamento.simulacao.falhar-pedido-id}") String falharPedidoId) {
        this.repository = repository;
        this.publisher = publisher;
        this.taxaAprovacao = taxaAprovacao;
        this.tempoProcessamentoMs = tempoProcessamentoMs;
        this.falharPedidoId = falharPedidoId == null ? "" : falharPedidoId.trim();
    }

    /**
     * Ao receber pedido.criado:
     * 1. registra o pagamento associado ao pedido;
     * 2. sorteia aprovação (~80%) ou rejeição (~20%);
     * 3. persiste o resultado no banco do Pagamento;
     * 4. registra a operação no log;
     * 5. (Etapa 12) publica pagamento.processado.
     */
    public void processar(PedidoEvento evento) {
        String correlationId = evento.correlationId();
        log.info("correlationId={} Evento pedido.criado recebido {} (produto {}, quantidade {})",
                correlationId, evento.pedidoId(), evento.produtoId(), evento.quantidade());

        if (falharPedidoId.equals(String.valueOf(evento.pedidoId()))) {
            log.error("correlationId={} Falha ao processar pagamento {}: gateway de pagamento indisponível (simulação)",
                    correlationId, evento.pedidoId());
            throw new IllegalStateException("Falha simulada no processamento do pagamento " + evento.pedidoId());
        }

        // Idempotência: a entrega é "at-least-once"; uma reentrega não gera novo pagamento.
        Optional<Pagamento> existente = repository.findByPedidoId(evento.pedidoId());
        if (existente.isPresent()) {
            log.warn("correlationId={} Pagamento do pedido {} já registrado ({}); reenviando resultado",
                    correlationId, evento.pedidoId(), existente.get().getStatus());
            publisher.publicarPagamentoProcessado(new PagamentoProcessadoEvento(
                    evento.pedidoId(), existente.get().getStatus(), correlationId));
            return;
        }

        simularTempoDeProcessamento();

        StatusPagamento status = ThreadLocalRandom.current().nextDouble() < taxaAprovacao
                ? StatusPagamento.APROVADO
                : StatusPagamento.REJEITADO;

        Pagamento pagamento = repository.save(new Pagamento(evento.pedidoId(), status));

        if (status == StatusPagamento.APROVADO) {
            log.info("correlationId={} Pagamento aprovado {}", correlationId, evento.pedidoId());
        } else {
            log.info("correlationId={} Pagamento rejeitado {}", correlationId, evento.pedidoId());
        }
        log.debug("Pagamento {} persistido", pagamento.getId());

        publisher.publicarPagamentoProcessado(
                new PagamentoProcessadoEvento(evento.pedidoId(), status.name(), correlationId));
    }

    public List<Pagamento> listar() {
        return repository.findAll(Sort.by("id"));
    }

    private void simularTempoDeProcessamento() {
        if (tempoProcessamentoMs <= 0) {
            return;
        }
        try {
            Thread.sleep(tempoProcessamentoMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
