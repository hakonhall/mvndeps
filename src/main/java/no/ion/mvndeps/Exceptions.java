package no.ion.mvndeps;

import java.io.IOException;
import java.io.UncheckedIOException;

public class Exceptions {

    // Unchecking methods returning reference

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    /** Wraps any {@link Exception} thrown by supplier in a {@link RuntimeException}. */
    public static <T> T uncheck(ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    public interface IOThrowingSupplier<T> {
        T get() throws IOException;
    }

    /** Wraps any {@link IOException} thrown by supplier in an {@link UncheckedIOException}. */
    public static <T> T uncheckIO(IOThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // Unchecking void methods

    @FunctionalInterface
    public interface IOThrowingRunnable {
        void run() throws IOException;
    }

    public static void uncheckIO(IOThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
