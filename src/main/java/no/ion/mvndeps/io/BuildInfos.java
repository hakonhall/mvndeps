package no.ion.mvndeps.io;

import no.ion.mvndeps.maven.ArtifactId;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static no.ion.mvndeps.misc.Exceptions.uncheckIO;

public class BuildInfos {
    private final List<BuildInfo> buildInfos;
    private final Map<ArtifactId, BuildInfo> buildByArtifactId;

    public static BuildInfos readFromPath(Path path) {
        var buildInfos = new ArrayList<BuildInfo>();

        List<String> lines = uncheckIO(() -> Files.readAllLines(path));
        for (String line : lines) {
            if (line.startsWith("#"))
                continue;
            BuildInfo buildInfo = BuildInfo.deserialize(line);
            buildInfos.add(buildInfo);
        }

        return new BuildInfos(buildInfos);
    }

    public BuildInfos(List<BuildInfo> buildInfos) {
        this.buildInfos = List.copyOf(Objects.requireNonNull(buildInfos));
        this.buildByArtifactId = buildInfos.stream().collect(Collectors.toMap(BuildInfo::artifactId, Function.identity()));
    }

    public List<BuildInfo> buildInfos() { return buildInfos; }

    public BuildInfo getByArtifactId(ArtifactId artifactId) {
        return Objects.requireNonNull(buildByArtifactId.get(artifactId), "No build for artifactId '" + artifactId + "'");
    }

    public void writeToPath(Path path) {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            for (BuildInfo buildInfo : buildInfos) {
                String line = buildInfo.serialize();
                writer.append(line).append('\n');
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
