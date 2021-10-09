package no.ion.mvndeps.maven;

import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

public class PomParent {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final Path relativeDirectoryPath;

    public static PomParent from(Path pomPath, String groupId, String artifactId, String version, Path relativePath) {
        Path pomDirectory = pomPath.getParent();

        final Path relativeDirectoryPath;
        if (Files.isDirectory(pomDirectory.resolve(relativePath))) {
            relativeDirectoryPath = relativePath;
        } else if (Files.isRegularFile(pomDirectory.resolve(relativePath))) {
            if (relativePath.getFileName().toString().equals("pom.xml")) {
                relativeDirectoryPath = relativePath.getParent();
            } else {
                throw new IllegalArgumentException("relativePath in parent element of " + pomPath +
                        " must refer to a directory or pom.xml: " + relativePath);
            }
        } else {
            throw new IllegalArgumentException("there is no parent file or directory " + relativePath +
                    " in " + pomPath);
        }

        return new PomParent(groupId, artifactId, version, relativeDirectoryPath);
    }

    private PomParent(String groupId, String artifactId, String version, Path relativeDirectoryPath) {
        this.groupId = requireNonNull(groupId);
        this.artifactId = requireNonNull(artifactId);
        this.version = requireNonNull(version);
        this.relativeDirectoryPath = requireNonNull(relativeDirectoryPath);
    }

    public String groupId() { return groupId; }
    public String artifactId() { return artifactId; }
    public String version() { return version; }
    public Path relativePathToPomXml() { return relativeDirectoryPath.resolve("pom.xml"); }
    public Path relativePathToDirectory() { return relativeDirectoryPath; }
}
