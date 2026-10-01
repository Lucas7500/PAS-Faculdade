package br.ufg.pas.pedido.messaging;

/** Evento recebido do Pagamento Service. status: APROVADO ou REJEITADO. */
public record PagamentoProcessadoEvento(Long pedidoId, String status, String correlationId) {
}
