package br.ufg.pas.pedido.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long produtoId;

    private Integer quantidade;

    private String status;

    public Pedido() {
    }

    public Pedido(Long produtoId, Integer quantidade, StatusPedido status) {
        this.produtoId = produtoId;
        this.quantidade = quantidade;
        this.status = status.name();
    }

    public Long getId() {
        return id;
    }

    public Long getProdutoId() {
        return produtoId;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(StatusPedido status) {
        this.status = status.name();
    }
}
