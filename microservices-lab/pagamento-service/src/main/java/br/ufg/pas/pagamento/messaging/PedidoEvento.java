package br.ufg.pas.pagamento.messaging;

/**
 * Contrato do evento pedido.criado. É uma cópia própria deste serviço: os
 * microsserviços compartilham o formato JSON, não classes Java.
 */
public record PedidoEvento(Long pedidoId, Long produtoId, Integer quantidade, String correlationId) {
}
