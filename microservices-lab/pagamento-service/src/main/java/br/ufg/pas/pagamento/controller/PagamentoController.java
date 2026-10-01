package br.ufg.pas.pagamento.controller;

import br.ufg.pas.pagamento.model.Pagamento;
import br.ufg.pas.pagamento.service.PagamentoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consulta apenas para diagnóstico. A porta não é publicada no host (expose),
 * então só é acessível pela rede interna do Docker Compose.
 */
@RestController
@RequestMapping("/pagamentos")
public class PagamentoController {

    private final PagamentoService service;

    public PagamentoController(PagamentoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Pagamento> listar() {
        return service.listar();
    }
}
