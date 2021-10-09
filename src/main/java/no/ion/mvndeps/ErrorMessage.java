package no.ion.mvndeps;

public class ErrorMessage extends RuntimeException {
    public ErrorMessage(String message) {
        super(message);
    }
}
