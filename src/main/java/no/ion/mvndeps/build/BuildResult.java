package no.ion.mvndeps.build;

import no.ion.mvndeps.ArtifactId;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.time.Duration;

public record BuildResult(Path modulePath, ArtifactId artifactId, Duration buildTime) {
    public static BuildResult from(FileSystem fileSystem, String modulePath, String artifactId, String buildTime) {
        return new BuildResult(fileSystem.getPath(modulePath),
                               ArtifactId.fromString(artifactId),
                               Duration.parse("PT" + buildTime + "S"));
    }
}
