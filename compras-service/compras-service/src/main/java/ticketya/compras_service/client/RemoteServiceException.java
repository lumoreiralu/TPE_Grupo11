package ticketya.compras_service.client;

/**
 * Marca los fallos transitorios de una dependencia remota (5xx, timeouts, red).
 * Resilience4j sólo reintenta esta excepción; los errores de negocio 4xx se
 * representan con {@code NegocioException} y no se reintentan.
 */
public class RemoteServiceException extends RuntimeException {

    public RemoteServiceException(String message) {
        super(message);
    }

    public RemoteServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
