package no.ion.mvndeps.build;

import no.ion.mvndeps.ArtifactId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildResults {
    private final List<BuildResult> results = new ArrayList<>();
    private final Map<ArtifactId, BuildResult> byArtifactId = new HashMap<>();

    BuildResults() {}

    void add(BuildResult result) {
        results.add(result);
        byArtifactId.put(result.artifactId(), result);
    }

    BuildResult get(ArtifactId artifactId) {
        BuildResult result = byArtifactId.get(artifactId);
        if (result == null)
            throw new IllegalArgumentException("There is no build result for " + artifactId);
        return result;
    }
}
