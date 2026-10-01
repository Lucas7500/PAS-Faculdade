package br.ufg.pas.pedido.model;

public enum StatusPedido {
    /** Estado inicial de todo novo pedido. */
    AGUARDANDO_PAGAMENTO,
    PAGO,
    REJEITADO
}
