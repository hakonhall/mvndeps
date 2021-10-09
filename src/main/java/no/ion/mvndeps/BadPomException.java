package no.ion.mvndeps;

import java.nio.file.Path;

public class BadPomException extends RuntimeException {
    BadPomException(Path modulePath, String message) {
        super("Bad maven module " + modulePath + ": " + message);
    }
}
