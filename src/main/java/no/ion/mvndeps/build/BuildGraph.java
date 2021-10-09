package no.ion.mvndeps.build;

import no.ion.mvndeps.maven.MavenCoordinate;
import no.ion.mvndeps.misc.Mutable;
import no.ion.mvndeps.dot.DotAttribute;
import no.ion.mvndeps.dot.DotId;
import no.ion.mvndeps.dot.EdgeStatement;
import no.ion.mvndeps.dot.NodeStatement;
import no.ion.mvndeps.graph.Edge;
import no.ion.mvndeps.graph.Vertex;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BuildGraph {
    private final Path outputPath;
    private final BuildResults buildResults;
    private final Build build;

    public BuildGraph(Path outputPath, BuildResults buildResults, Build build) {
        this.outputPath = outputPath;
        this.buildResults = buildResults;
        this.build = build;
    }

    void go() {
        final Mutable<Vertex<MavenCoordinate, ModuleBuild, BuildEdge>> last = new Mutable<>(null);

        build.inBuildOrder(vertex -> {
            Edge<MavenCoordinate, ModuleBuild, BuildEdge> criticalEdge = null;
            Duration totalElapsedTimeBase = null;

            for (var incomingEdge : vertex.incoming()) {
                var source = incomingEdge.fromVertex().get();
                Duration sourceTotalElapsedTime = source.totalElapsedTime();
                if (criticalEdge == null || sourceTotalElapsedTime.compareTo(totalElapsedTimeBase) > 0) {
                    criticalEdge = incomingEdge;
                    totalElapsedTimeBase = sourceTotalElapsedTime;
                }
            }

            if (criticalEdge != null) {
                var edge = new BuildEdge().setCritical(true);
                criticalEdge.set(edge);
            }

            Duration elapsedTime = buildResults.get(vertex.id().getGroupArtifact()).buildTime();
            if (totalElapsedTimeBase == null)
                totalElapsedTimeBase = Duration.ZERO;
            Duration totalElapsedTime = totalElapsedTimeBase.plus(elapsedTime);

            if (last.get() == null || totalElapsedTime.compareTo(last.get().get().totalElapsedTime()) > 0)
                last.set(vertex);

            vertex.get().setTimes(elapsedTime, totalElapsedTime, criticalEdge);
        });

        for (var x = last.get(); x != null; x = x.get().criticalEdge().map(Edge::fromVertex).orElse(null)) {
            x.get().criticalEdge().map(Edge::get).map(edge -> edge.setOnCriticalPath(true));
        }

        final Mutable<Duration> minElapsedTime = new Mutable<>(null);
        final Mutable<Duration> maxElapsedTime = new Mutable<>(null);
        final Mutable<Double> minMeasure = new Mutable<>(null);
        final Mutable<Double> maxMeasure = new Mutable<>(null);
        build.inBuildOrder(vertex -> {
            Duration elapsed = vertex.get().elapsedTime();
            if (minElapsedTime.get() == null) {
                minElapsedTime.set(elapsed);
                maxElapsedTime.set(elapsed);
            } else if (elapsed.compareTo(minElapsedTime.get()) < 0) {
                minElapsedTime.set(elapsed);
            } else if (elapsed.compareTo(maxElapsedTime.get()) > 0) {
                maxElapsedTime.set(elapsed);
            }

            for (var inEdge : vertex.incoming()) {
                double measure = measureOf(inEdge);
                if (minMeasure.get() == null) {
                    minMeasure.set(measure);
                    maxMeasure.set(measure);
                } else if (measure < minMeasure.get()) {
                    minMeasure.set(measure);
                } else if (measure > maxMeasure.get()) {
                    maxMeasure.set(measure);
                }
            }
        });

        writeDot(Objects.requireNonNull(minElapsedTime.get()).toMillis() / 1000.0,
                 Objects.requireNonNull(maxElapsedTime.get()).toMillis() / 1000.0,
                 Objects.requireNonNull(minMeasure.get()),
                 Objects.requireNonNull(maxMeasure.get()));
    }

    private static final int MIN_WEIGHT = 2;
    private static final int MAX_WEIGHT = 20;
    private static final int MIN_PENWIDTH = 1;
    private static final int MAX_PENWIDTH = 7;
    private static final int MIN_FONTSIZE = 10;
    private static final int MAX_FONTSIZE = 100;

    private void writeDot(double minElapsedTimeSeconds, double maxElapsedTimeSeconds,
                          double minMeasure, double maxMeasure) {

        var weightFromMeasure = new Scaler(minMeasure, maxMeasure, MIN_WEIGHT, MAX_WEIGHT);
        var penwidthFromMeasure = new Scaler(minMeasure, maxMeasure, MIN_PENWIDTH, MAX_PENWIDTH);
        var fontsizeFromElapsedSeconds = new Scaler(minElapsedTimeSeconds, maxElapsedTimeSeconds, MIN_FONTSIZE, MAX_FONTSIZE);

        build.toDotFile("build",
                        List.of(DotAttribute.from("ratio", "2"),
                                DotAttribute.from("nodesep", "0.05")),
                        vertex -> {
                            String text = vertex.id().artifactId() + "\n" + vertex.get().elapsedTime().toSeconds() + "s (" +
                                    vertex.get().totalElapsedTime().toSeconds() + "s)";

                            var attributes = new ArrayList<DotAttribute>();
                            int fontsize = fontsizeFromElapsedSeconds.scale(vertex.get().elapsedTime().toMillis() / 1000.0);
                            attributes.add(DotAttribute.from("fontsize", Integer.toString(fontsize)));
                            attributes.add(DotAttribute.from("label", text));

                            return new NodeStatement(DotId.from(vertex.id().artifactId()), attributes);
                        },
                        edge -> {
                            var attributes = new ArrayList<DotAttribute>();
                            if (edge.get() != null) {
                                if (edge.get().onCriticalPath()) {
                                    attributes.add(DotAttribute.from("color", "red"));
                                } else if (edge.get().critical()) {
                                    attributes.add(DotAttribute.from("color", "blue"));
                                } else {
                                    throw new IllegalStateException("edge only set if critical!?");
                                }

                                double measure = measureOf(edge);
                                int weight = weightFromMeasure.scale(measure);
                                attributes.add(DotAttribute.from("weight", Long.toString(weight)));
                                int penwidth = penwidthFromMeasure.scale(measure);
                                attributes.add(DotAttribute.from("penwidth", Long.toString(penwidth)));
                            } else {
                                //return null;
                            }

                            return new EdgeStatement(DotId.from(edge.fromVertex().id().artifactId()),
                                                     DotId.from(edge.toVertex().id().artifactId()),
                                                     attributes);
                        },
                        outputPath);
    }

    private static double measureOf(Edge<MavenCoordinate, ModuleBuild, BuildEdge> edge) {
        return measureOf(edge.fromVertex().get().elapsedTime().toMillis() / 1000.0,
                         edge.toVertex().get().elapsedTime().toMillis() / 1000.0);
    }

    private static double measureOf(double fromSeconds, double toSeconds) {
        return Math.sqrt(fromSeconds * fromSeconds + toSeconds * toSeconds);
    }
}
