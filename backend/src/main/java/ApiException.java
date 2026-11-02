/** Erro "esperado" (ex.: dados inválidos). Vira uma resposta JSON {"error": "..."} com o status HTTP. */
final class ApiException extends RuntimeException {
    final int status;

    ApiException(int status, String message) {
        super(message);
        this.status = status;
    }
}
