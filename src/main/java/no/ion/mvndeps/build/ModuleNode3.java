package no.ion.mvndeps.build;

import no.ion.mvndeps.MavenCoordinate;

import java.time.Duration;
import java.util.Optional;

public class ModuleNode3 {
    private final MavenCoordinate coordinate;

    private Duration elapsedTime = Duration.ZERO;

    private Optional<MavenCoordinate> criticalDependency = Optional.empty();
    private Duration criticalPathTotalTime = Duration.ZERO;

    public ModuleNode3(MavenCoordinate coordinate) {
        this.coordinate = coordinate;
    }

    public MavenCoordinate coordinate() { return coordinate; }
    public Duration elapsedTime() { return elapsedTime; }
    public Optional<MavenCoordinate> criticalDependency() { return criticalDependency; }
    public Duration criticalPathTotalTime() { return criticalPathTotalTime; }

    public ModuleNode3 setTimes(Duration elapsedTime, Optional<MavenCoordinate> criticalDependency, Duration criticalPathTotalTime) {
        this.elapsedTime = elapsedTime;
        this.criticalDependency = criticalDependency;
        this.criticalPathTotalTime = criticalPathTotalTime;
        return this;
    }
}
