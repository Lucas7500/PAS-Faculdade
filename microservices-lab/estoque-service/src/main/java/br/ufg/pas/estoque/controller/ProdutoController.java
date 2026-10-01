package br.ufg.pas.estoque.controller;

import br.ufg.pas.estoque.dto.ReservaRequest;
import br.ufg.pas.estoque.model.Produto;
import br.ufg.pas.estoque.service.EstoqueService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    private final EstoqueService service;

    public ProdutoController(EstoqueService service) {
        this.service = service;
    }

    /** Endpoint 1 - Consultar todos os produtos. */
    @GetMapping
    public List<Produto> listar() {
        return service.listar();
    }

    /** Endpoint 2 - Consultar produto (404 se não existir). */
    @GetMapping("/{id}")
    public Produto buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    /** Endpoint 3 - Reservar estoque (200 / 409 / 404). */
    @PutMapping("/{id}/reservar")
    public Produto reservar(@PathVariable Long id, @Valid @RequestBody ReservaRequest request) {
        return service.reservar(id, request.quantidade());
    }

    /** Endpoint extra - Compensação: devolve ao estoque uma reserva desfeita. */
    @PutMapping("/{id}/liberar")
    public Produto liberar(@PathVariable Long id, @Valid @RequestBody ReservaRequest request) {
        return service.liberar(id, request.quantidade());
    }
}
