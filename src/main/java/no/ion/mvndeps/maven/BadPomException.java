package no.ion.mvndeps.maven;

import java.nio.file.Path;

public class BadPomException extends RuntimeException {
    public BadPomException(Path modulePath, String message) {
        super("Bad maven module " + modulePath + ": " + message);
    }
}
