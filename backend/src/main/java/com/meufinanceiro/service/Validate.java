package com.meufinanceiro.service;

import com.meufinanceiro.exception.ApiException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/** Funções pequenas para validar os campos que chegam do front. */
final class Validate {
    private Validate() {
    }

    /** Texto obrigatório (sem espaços nas pontas). */
    static String text(String value, String message, int max) {
        String s = value == null ? "" : value.trim();
        if (s.isEmpty()) throw new ApiException(400, message);
        if (s.length() > max) throw new ApiException(400, "Texto muito longo.");
        return s;
    }

    /** Texto opcional: devolve "" se não vier nada. */
    static String optionalText(String value, int max) {
        String s = value == null ? "" : value.trim();
        if (s.length() > max) throw new ApiException(400, "Texto muito longo.");
        return s;
    }

    static String email(String value) {
        String email = text(value, "Digite um e-mail válido", 200).toLowerCase();
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw new ApiException(400, "Digite um e-mail válido");
        return email;
    }

    /** Valor em centavos (maior que zero). */
    static long cents(Long value) {
        if (value == null || value <= 0 || value > 100_000_000_000L) {
            throw new ApiException(400, "Informe um valor maior que zero.");
        }
        return value;
    }

    /** Data AAAA-MM-DD. Se não for obrigatória e vier vazia, devolve null. */
    static String date(String value, boolean required) {
        String s = value == null ? "" : value.trim();
        if (s.isEmpty()) {
            if (required) throw new ApiException(400, "Preencha a descrição e a data.");
            return null;
        }
        try {
            return LocalDate.parse(s).toString();
        } catch (DateTimeParseException e) {
            throw new ApiException(400, "Data inválida.");
        }
    }

    static UUID id(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new ApiException(404, "Registro não encontrado.");
        }
    }

    static ApiException notFound() {
        return new ApiException(404, "Registro não encontrado.");
    }
}
