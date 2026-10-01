package br.ufg.pas.estoque.service;

import br.ufg.pas.estoque.exception.EstoqueInsuficienteException;
import br.ufg.pas.estoque.exception.ProdutoInexistenteException;
import br.ufg.pas.estoque.model.Produto;
import br.ufg.pas.estoque.repository.ProdutoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EstoqueService {

    private static final Logger log = LoggerFactory.getLogger(EstoqueService.class);

    private final ProdutoRepository repository;

    public EstoqueService(ProdutoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Produto> listar() {
        return repository.findAll(Sort.by("id"));
    }

    @Transactional(readOnly = true)
    public Produto buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new ProdutoInexistenteException(id));
    }

    /**
     * Regras do enunciado:
     * - produto existe e há saldo: reduz a quantidade e retorna 200;
     * - produto existe e não há saldo: não altera e retorna 409;
     * - produto não existe: não altera e retorna 404.
     */
    @Transactional
    public Produto reservar(Long id, int quantidade) {
        String correlationId = MDC.get("correlationId");

        if (!repository.existsById(id)) {
            log.warn("correlationId={} Produto {} inexistente", correlationId, id);
            throw new ProdutoInexistenteException(id);
        }

        if (repository.reservar(id, quantidade) == 0) {
            log.warn("correlationId={} Estoque insuficiente para o produto {} (solicitado {})",
                    correlationId, id, quantidade);
            throw new EstoqueInsuficienteException(id, quantidade);
        }

        log.info("correlationId={} Produto {} reservado", correlationId, id);
        return repository.findById(id).orElseThrow();
    }

    /** Compensação da reserva (usada pela Saga do Pedido Service). */
    @Transactional
    public Produto liberar(Long id, int quantidade) {
        String correlationId = MDC.get("correlationId");
        if (repository.liberar(id, quantidade) == 0) {
            throw new ProdutoInexistenteException(id);
        }
        log.info("correlationId={} Reserva do produto {} liberada (compensação, quantidade {})",
                correlationId, id, quantidade);
        return repository.findById(id).orElseThrow();
    }
}
