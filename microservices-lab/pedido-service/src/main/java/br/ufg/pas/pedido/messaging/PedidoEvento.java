package br.ufg.pas.pedido.messaging;

/** Evento publicado em pedidos.exchange com a routing key pedido.criado. */
public record PedidoEvento(Long pedidoId, Long produtoId, Integer quantidade, String correlationId) {
}
