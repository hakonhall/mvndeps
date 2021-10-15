package no.ion.mvndeps.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static no.ion.mvndeps.misc.Exceptions.uncheckIO;

public record BuildInfos(List<BuildInfo> buildInfos) {
    public static BuildInfos readFromPath(Path path) {
        var buildInfos = new ArrayList<BuildInfo>();

        List<String> lines = uncheckIO(() -> Files.readAllLines(path));
        for (String line : lines) {
            if (line.startsWith("#"))
                continue;
            BuildInfo buildInfo = BuildInfo.deserialize(line);
            buildInfos.add(buildInfo);
        }

        return new BuildInfos(List.copyOf(buildInfos));
    }

    public BuildInfos {
        Objects.requireNonNull(buildInfos);
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
