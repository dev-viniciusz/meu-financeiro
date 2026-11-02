package com.meufinanceiro.exception;

/** Erro "esperado" (ex.: dados inválidos). Vira {"error": "..."} com o status HTTP. */
public class ApiException extends RuntimeException {
    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
