package br.ufg.pas.pedido.exception;

/** Falha ocorrida depois da reserva do estoque e antes da persistência do pedido. */
public class FalhaCriacaoPedidoException extends RuntimeException {

    private final boolean compensado;

    public FalhaCriacaoPedidoException(String message, boolean compensado, Throwable cause) {
        super(message, cause);
        this.compensado = compensado;
    }

    public boolean isCompensado() {
        return compensado;
    }
}
