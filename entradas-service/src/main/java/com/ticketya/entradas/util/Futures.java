package com.ticketya.entradas.util;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * Puente entre los clientes asíncronos (requeridos por @TimeLimiter) y los
 * servicios de negocio, que siguen trabajando de forma imperativa.
 */
public final class Futures {

    private Futures() {
    }

    public static <T> T join(java.util.concurrent.CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            Throwable cause = unwrap(e);
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw e;
        }
    }

    public static Throwable unwrap(Throwable error) {
        Throwable actual = error;
        while ((actual instanceof CompletionException || actual instanceof ExecutionException)
                && actual.getCause() != null) {
            actual = actual.getCause();
        }
        return actual;
    }
}
