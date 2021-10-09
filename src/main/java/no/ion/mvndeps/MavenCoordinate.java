package no.ion.mvndeps;

import java.util.Objects;

public class MavenCoordinate {
    private final String groupId;
    private final String artifactId;
    private final String version;

    public MavenCoordinate(String groupId, String artifactId, String version) {
        this.groupId = requireNonEmpty(groupId, "groupId");
        this.artifactId = requireNonEmpty(artifactId, "groupId");
        this.version = requireNonEmpty(version, "version");
    }

    private static String requireNonEmpty(String string, String name) {
        Objects.requireNonNull(string, name + " is null");
        if (string.isEmpty()) {
            throw new IllegalArgumentException(name + " is empty");
        }

        return string;
    }

    public String groupId() { return groupId; }
    public String artifactId() { return artifactId; }
    public String version() { return version; }
    public ArtifactId getGroupArtifact() { return ArtifactId.fromMavenCoordinate(this); }
    @Override public String toString() { return groupId + ":" + artifactId + ":" + version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MavenCoordinate that = (MavenCoordinate) o;
        return groupId.equals(that.groupId) &&
                artifactId.equals(that.artifactId) &&
                version.equals(that.version);
    }

    @Override
    public int hashCode() { return Objects.hash(groupId, artifactId, version); }
}
