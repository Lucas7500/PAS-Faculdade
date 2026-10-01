package br.ufg.pas.pedido.exception;

import br.ufg.pas.pedido.dto.MensagemResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EstoqueInsuficienteException.class)
    public ResponseEntity<MensagemResponse> estoqueInsuficiente(EstoqueInsuficienteException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new MensagemResponse("Estoque insuficiente"));
    }

    @ExceptionHandler(ProdutoInexistenteException.class)
    public ResponseEntity<MensagemResponse> produtoInexistente(ProdutoInexistenteException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MensagemResponse("Produto inexistente"));
    }

    @ExceptionHandler(PedidoInexistenteException.class)
    public ResponseEntity<MensagemResponse> pedidoInexistente(PedidoInexistenteException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MensagemResponse("Pedido inexistente"));
    }

    @ExceptionHandler(EstoqueIndisponivelException.class)
    public ResponseEntity<MensagemResponse> estoqueIndisponivel(EstoqueIndisponivelException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new MensagemResponse("Estoque Service indisponível"));
    }

    @ExceptionHandler(FalhaCriacaoPedidoException.class)
    public ResponseEntity<MensagemResponse> falhaCriacao(FalhaCriacaoPedidoException e) {
        String detalhe = e.isCompensado()
                ? "reserva de estoque desfeita (compensação executada)"
                : "reserva de estoque NÃO foi desfeita (sem compensação)";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MensagemResponse("Falha ao criar o pedido: " + detalhe));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MensagemResponse> validacao(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage())
                .findFirst()
                .orElse("Requisição inválida");
        return ResponseEntity.badRequest().body(new MensagemResponse(msg));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<MensagemResponse> corpoInvalido(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(new MensagemResponse("Corpo da requisição inválido"));
    }
}
