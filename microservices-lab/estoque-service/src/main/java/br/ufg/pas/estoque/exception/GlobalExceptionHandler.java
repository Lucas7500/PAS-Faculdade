package br.ufg.pas.estoque.exception;

import br.ufg.pas.estoque.dto.MensagemResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProdutoInexistenteException.class)
    public ResponseEntity<MensagemResponse> produtoInexistente(ProdutoInexistenteException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new MensagemResponse("Produto inexistente"));
    }

    @ExceptionHandler(EstoqueInsuficienteException.class)
    public ResponseEntity<MensagemResponse> estoqueInsuficiente(EstoqueInsuficienteException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new MensagemResponse("Estoque insuficiente"));
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
