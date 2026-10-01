package br.ufg.pas.pedido.controller;

import br.ufg.pas.pedido.dto.CriarPedidoRequest;
import br.ufg.pas.pedido.model.Pedido;
import br.ufg.pas.pedido.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    /**
     * Cria um pedido. Os parâmetros opcionais servem apenas ao experimento de
     * consistência da Etapa 3:
     * POST /pedidos?simularFalha=true                 -> falha sem compensação
     * POST /pedidos?simularFalha=true&compensar=true  -> falha com compensação (Saga)
     */
    @PostMapping
    public ResponseEntity<Pedido> criar(@Valid @RequestBody CriarPedidoRequest request,
                                        @RequestParam(defaultValue = "false") boolean simularFalha,
                                        @RequestParam(defaultValue = "false") boolean compensar) {
        Pedido pedido = service.criar(request, simularFalha, compensar);
        return ResponseEntity.created(URI.create("/pedidos/" + pedido.getId())).body(pedido);
    }

    @GetMapping
    public List<Pedido> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Pedido buscar(@PathVariable Long id) {
        return service.buscar(id);
    }
}
