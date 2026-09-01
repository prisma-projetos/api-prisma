package br.com.prisma.api_prisma.exception;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Traduz excecoes em respostas JSON com o mesmo formato do filtro de API Key. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** UUID malformado no path chega aqui: e erro do cliente, nao 500. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST,
                "Valor invalido para o parametro '" + ex.getName() + "'.");
    }

    /** Sem isto, o caso cai no /error padrao, com corpo diferente do resto da API. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(
            MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST,
                "Parametro obrigatorio ausente: '" + ex.getParameterName() + "'.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Corpo ausente ou malformado. A mensagem do Jackson nao e repassada: ela
     * carrega nomes de classe e tipos internos.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleBodyIlegivel(
            HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST,
                "Corpo da requisicao ausente ou invalido. Envie um JSON com os "
                        + "parametros da consulta.");
    }

    /**
     * Rede de seguranca: qualquer excecao nao prevista vira 500 generico.
     * O detalhe fica no log do servidor, nunca na resposta -- e o que separa
     * um erro de um mapa da estrutura interna para quem estiver sondando.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleInesperado(Exception ex) {
        log.error("Erro nao tratado ao processar requisicao", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno ao processar a requisicao.");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
