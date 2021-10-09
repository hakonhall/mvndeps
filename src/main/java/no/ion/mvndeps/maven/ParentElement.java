package no.ion.mvndeps.maven;

import no.ion.mvndeps.MavenCoordinate;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;

import java.util.Objects;
import java.util.Optional;

public class ParentElement {
    public static final String POM_XML = "/pom.xml";
    private final MavenCoordinate coordinate;
    private final String relativeDirectory;

    static Optional<ParentElement> fromModel(Model model) {
        Parent parent = model.getParent();
        if (parent == null) {
            return Optional.empty();
        }

        var coordinate = new MavenCoordinate(Objects.requireNonNull(parent.getGroupId()),
                                             Objects.requireNonNull(parent.getArtifactId()),
                                             Objects.requireNonNull(parent.getVersion()));

        String relativeDirectory = parent.getRelativePath();
        if (relativeDirectory == null || relativeDirectory.isEmpty()) {
            relativeDirectory = "..";
        } else if (relativeDirectory.equals("pom.xml")) {
            relativeDirectory = ".";
        } else if (relativeDirectory.endsWith(POM_XML)) {
            relativeDirectory = relativeDirectory.substring(0, relativeDirectory.length() - POM_XML.length());
        }

        return Optional.of(new ParentElement(coordinate, relativeDirectory));
    }

    private ParentElement(MavenCoordinate coordinate, String relativeDirectory) {
        this.coordinate = coordinate;
        this.relativeDirectory = relativeDirectory;
    }

    public MavenCoordinate coordinate() { return coordinate; }
    public String relativeDirectory() { return relativeDirectory; }
}
