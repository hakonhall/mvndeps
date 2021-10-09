package no.ion.mvndeps;

import java.time.Duration;

public record BuiltModule(MavenModule module, Duration elapsedTime, Duration totalElapsedTime) { }
