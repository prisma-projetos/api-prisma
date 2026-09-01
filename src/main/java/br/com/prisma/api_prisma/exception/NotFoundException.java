package br.com.prisma.api_prisma.exception;

/** Recurso inexistente: vira 404 no {@link GlobalExceptionHandler}. */
public class NotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(message);
    }
}
