package no.ion.mvndeps;

import static java.util.Objects.requireNonNull;

public record ArtifactId(String groupId, String artifactId) {
    public ArtifactId {
        requireNonNull(groupId);
        requireNonNull(artifactId);
    }

    public static ArtifactId fromMavenCoordinate(MavenCoordinate coordinate) {
        return new ArtifactId(coordinate.groupId(), coordinate.artifactId());
    }

    public static ArtifactId fromString(String artifactId) {
        int colonIndex = artifactId.indexOf(':');
        if (colonIndex == -1)
            throw new IllegalArgumentException("Missing ':' in artifactId: " + artifactId);

        if (artifactId.indexOf(':', colonIndex + 1) != -1)
            throw new IllegalArgumentException("More than one ':' in artifactId: " + artifactId);

        return new ArtifactId(artifactId.substring(0, colonIndex), artifactId.substring(colonIndex + 1));
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId;
    }
}
