package br.ufg.pas.pedido.exception;

public class ProdutoInexistenteException extends RuntimeException {

    public ProdutoInexistenteException() {
        super("Produto inexistente");
    }
}
