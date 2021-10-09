package no.ion.mvndeps;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;

import java.util.Objects;
import java.util.Optional;

public class ParentElement {
    private final MavenCoordinate coordinate;
    private final String relativePath;

    static Optional<ParentElement> fromModel(Model model) {
        Parent parent = model.getParent();
        if (parent == null) {
            return Optional.empty();
        }

        String relativePath = Optional.ofNullable(parent.getRelativePath()).orElse("../pom.xml");

        return Optional.of(new ParentElement(
                Objects.requireNonNull(parent.getGroupId()),
                Objects.requireNonNull(parent.getArtifactId()),
                Objects.requireNonNull(parent.getVersion()),
                relativePath));
    }

    ParentElement(String groupId, String artifactId, String version, String relativePath) {
        this.coordinate = new MavenCoordinate(groupId, artifactId, version);
        this.relativePath = relativePath;
    }

    public MavenCoordinate coordinate() { return coordinate; }
    public String relativePath() { return relativePath; }
}
