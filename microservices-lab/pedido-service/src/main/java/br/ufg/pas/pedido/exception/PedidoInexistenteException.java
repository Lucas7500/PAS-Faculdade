package br.ufg.pas.pedido.exception;

public class PedidoInexistenteException extends RuntimeException {

    public PedidoInexistenteException(Long id) {
        super("Pedido inexistente: " + id);
    }
}
