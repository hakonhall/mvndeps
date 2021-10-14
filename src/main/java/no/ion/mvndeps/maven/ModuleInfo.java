package no.ion.mvndeps.maven;

import java.nio.file.Path;

/**
 * The group ID, artifact ID, and relative path of a Maven module relative a project root.
 */
public record ModuleInfo(ArtifactId artifactId, Path path) {
}
