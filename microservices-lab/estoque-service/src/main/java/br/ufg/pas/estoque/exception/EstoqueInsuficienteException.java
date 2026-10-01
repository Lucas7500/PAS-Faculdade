package br.ufg.pas.estoque.exception;

public class EstoqueInsuficienteException extends RuntimeException {

    public EstoqueInsuficienteException(Long id, int solicitada) {
        super("Estoque insuficiente para o produto " + id + " (solicitado: " + solicitada + ")");
    }
}
