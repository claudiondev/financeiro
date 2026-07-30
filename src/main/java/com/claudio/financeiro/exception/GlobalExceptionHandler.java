package com.claudio.financeiro.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centraliza o tratamento de erros da API, padronizando o formato de resposta
 * (mesmo campo "erro" já usado pelo RateLimitInterceptor) em vez de deixar cada
 * exceção vazar no formato genérico do Spring.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> tratarErrosDeValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> detalhes = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            detalhes.put(erro.getField(), erro.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(corpoDeErro(HttpStatus.BAD_REQUEST, "Dados inválidos", detalhes));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> tratarCorpoInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(corpoDeErro(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido", null));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> tratarResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return ResponseEntity.status(status).body(corpoDeErro(status, ex.getReason(), null));
    }

    // Rede de segurança para qualquer exceção não mapeada — nunca deixa o erro cru vazar pro cliente
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> tratarErroInesperado(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(corpoDeErro(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor", null));
    }

    private Map<String, Object> corpoDeErro(HttpStatus status, String mensagem, Map<String, String> detalhes) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("timestamp", LocalDateTime.now());
        corpo.put("status", status.value());
        corpo.put("erro", mensagem);
        if (detalhes != null && !detalhes.isEmpty()) {
            corpo.put("detalhes", detalhes);
        }
        return corpo;
    }
}
