package no.ion.mvndeps;

import no.ion.mvndeps.maven.MavenModule;

import java.time.Duration;

public record BuiltModule(MavenModule module, Duration elapsedTime, Duration totalElapsedTime) { }
