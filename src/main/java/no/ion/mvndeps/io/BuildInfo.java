package no.ion.mvndeps.io;

import no.ion.mvndeps.UsageError;
import no.ion.mvndeps.maven.ArtifactId;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Scanner;

public record BuildInfo(ArtifactId artifactId, Path modulePath, Duration buildTimeOrNull) {

    public static BuildInfo fromRaw(String groupId, String artifactId, String modulePath, Double buildTimeSeconds) {
        Duration elapsedBuildTimeOrNull = buildTimeSeconds == null ? null : Duration.ofNanos(Math.round(buildTimeSeconds * 1e9));
        return new BuildInfo(new ArtifactId(groupId, artifactId), Path.of(modulePath), elapsedBuildTimeOrNull);
    }

    public static BuildInfo deserialize(String serialized) {
        var scanner = new Scanner(serialized);
        String groupId = scanner.next();
        String artifactId = scanner.next();
        String modulePath = scanner.next();
        Double buildTimeSeconds = scanner.hasNext() ? scanner.nextDouble() : null;
        return fromRaw(groupId, artifactId, modulePath, buildTimeSeconds);
    }

    public BuildInfo {
        Objects.requireNonNull(artifactId);
        Objects.requireNonNull(modulePath);
    }

    public String serialize() {
        var stringBuilder = new StringBuilder();

        stringBuilder.append(serializeToken("groupId", artifactId.groupId(), true))
                     .append(' ')
                     .append(serializeToken("artifactId", artifactId.artifactId(), false))
                     .append(' ')
                     .append(serializeToken("module path", modulePath.toString(), false));

        if (buildTimeOrNull != null) {
            stringBuilder.append(' ')
                         .append(String.format("%.3f", buildTimeOrNull.toMillis() / 1000.0));
        }

        return stringBuilder.toString();
    }

    private static String serializeToken(String name, String string, boolean bol) {
        if (bol && string.startsWith("#"))
            throw new UsageError(name + " cannot start with '#'");
        if (string.indexOf(' ') != -1)
            throw new UsageError(name + " contains space: '" + string + "'");
        if (string.indexOf('\n') != -1)
            throw new UsageError(name + " contains newline: '" + string + "'");
        return string;
    }
}
