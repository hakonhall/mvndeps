package no.ion.mvndeps;

import java.util.Objects;

public class MavenModuleId {
    private final MavenCoordinate coordinate;
    private final String packaging;

    private MavenModuleId(MavenCoordinate coordinate, String packaging) {
        this.coordinate = coordinate;
        this.packaging = packaging;
    }

    MavenCoordinate coordinate() { return coordinate; }
    String groupId() { return coordinate.groupId(); }
    String artifactId() { return coordinate.artifactId(); }
    String version() { return coordinate.version(); }
    String packaging() { return packaging; }

    @Override
    public String toString() {
        return coordinate.groupId() + ":" + coordinate.artifactId() + ":" + packaging + ":" + coordinate.version();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MavenModuleId that = (MavenModuleId) o;
        return coordinate.equals(that.coordinate) &&
                packaging.equals(that.packaging);
    }

    @Override
    public int hashCode() { return Objects.hash(coordinate, packaging); }
}
