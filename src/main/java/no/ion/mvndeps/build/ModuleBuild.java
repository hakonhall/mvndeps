package no.ion.mvndeps.build;

import no.ion.mvndeps.maven.MavenCoordinate;
import no.ion.mvndeps.maven.MavenModule;
import no.ion.mvndeps.graph.Edge;

import java.time.Duration;
import java.util.Optional;

public class ModuleBuild {
    private final MavenModule module;

    private Duration elapsedTime = null;
    private Duration totalElapsedTime = null;
    private Optional<Edge<MavenCoordinate, ModuleBuild, BuildEdge>> criticalEdge = Optional.empty();

    ModuleBuild(MavenModule module) {
        this.module = module;
    }

    public MavenModule module() { return module; }

    public Duration elapsedTime() { return elapsedTime; }
    public Duration totalElapsedTime() { return totalElapsedTime; }
    public Optional<Edge<MavenCoordinate, ModuleBuild, BuildEdge>> criticalEdge() { return criticalEdge; }

    public void setTimes(Duration elapsedTime, Duration totalElapsedTime,
                         Edge<MavenCoordinate, ModuleBuild, BuildEdge> criticalPath) {
        this.elapsedTime = elapsedTime;
        this.totalElapsedTime = totalElapsedTime;
        this.criticalEdge = Optional.ofNullable(criticalPath);
    }
}
