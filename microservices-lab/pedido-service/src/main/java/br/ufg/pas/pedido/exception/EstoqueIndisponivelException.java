package br.ufg.pas.pedido.exception;

public class EstoqueIndisponivelException extends RuntimeException {

    public EstoqueIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }
}
