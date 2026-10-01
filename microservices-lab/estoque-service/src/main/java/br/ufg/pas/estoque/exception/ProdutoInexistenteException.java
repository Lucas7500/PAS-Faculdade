package br.ufg.pas.estoque.exception;

public class ProdutoInexistenteException extends RuntimeException {

    public ProdutoInexistenteException(Long id) {
        super("Produto inexistente: " + id);
    }
}
