package br.ufg.pas.pagamento.messaging;

/** Evento publicado após processar o pagamento. status: APROVADO ou REJEITADO. */
public record PagamentoProcessadoEvento(Long pedidoId, String status, String correlationId) {
}
