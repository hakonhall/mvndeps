package no.ion.mvndeps.program;

public class UsageError extends RuntimeException {
    public UsageError(String message) {
        super(message);
    }
}
