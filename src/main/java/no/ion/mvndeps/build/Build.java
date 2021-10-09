package no.ion.mvndeps.build;

import no.ion.mvndeps.maven.MavenCoordinate;
import no.ion.mvndeps.maven.MavenModule;
import no.ion.mvndeps.maven.MavenProject;
import no.ion.mvndeps.maven.MavenProjectReader;
import no.ion.mvndeps.dot.DagToDot;
import no.ion.mvndeps.dot.Digraph;
import no.ion.mvndeps.dot.DotAttribute;
import no.ion.mvndeps.dot.DotId;
import no.ion.mvndeps.dot.EdgeStatement;
import no.ion.mvndeps.dot.NodeStatement;
import no.ion.mvndeps.graph.Dag;
import no.ion.mvndeps.graph.DagBuilder;
import no.ion.mvndeps.graph.Edge;
import no.ion.mvndeps.graph.Vertex;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Build {
    private final Dag<MavenCoordinate, ModuleBuild, BuildEdge> dag;

    public static Build read(Path projectDirectory) {
        MavenProject project = MavenProjectReader.readProjectDirectory(projectDirectory);

        var dagBuilder = new DagBuilder<MavenCoordinate, ModuleBuild, BuildEdge>(build -> build.module().coordinate());

        for (MavenModule module : project.modules()) {
            var build = new ModuleBuild(module);
            dagBuilder.addVertex(build, build.module().getAllProjectDependencies(project));
        }

        return new Build(dagBuilder.build());
    }

    private Build(Dag<MavenCoordinate, ModuleBuild, BuildEdge> dag) {
        this.dag = dag;
    }

    public String toDot(String name) {
        var digraph = new Digraph(DotId.from(name));

        dag.visit(vertex -> {
            digraph.addNode(dotIdOf(vertex), List.of());

            for (var outgoingEdge : vertex.outgoing()
                                          .stream().
                                          sorted(Comparator.comparing(e -> e.toVertex().id().toString()))
                                          .collect(Collectors.toList())) {
                digraph.addEdge(dotIdOf(vertex), dotIdOf(outgoingEdge.toVertex()), List.of());
            }
        });

        //uncheckIO(() -> Files.writeString(dotPath, dot, StandardCharsets.UTF_8));

        return digraph.toString();
    }

    void toDotFile(String name,
                   List<DotAttribute> digraphAttributes,
                   Function<Vertex<MavenCoordinate, ModuleBuild, BuildEdge>, NodeStatement> toNode,
                   Function<Edge<MavenCoordinate, ModuleBuild, BuildEdge>, EdgeStatement> toEdge,
                   Path outFile) {
        DagToDot.toDotFile(name, dag, digraphAttributes, toNode, toEdge, outFile);
    }

    public List<Vertex<MavenCoordinate, ModuleBuild, BuildEdge>> buildOrder() {
        var order = new ArrayList<Vertex<MavenCoordinate, ModuleBuild, BuildEdge>>(dag.size());
        inBuildOrder(order::add);
        return order;
    }

    public void inBuildOrder(Consumer<Vertex<MavenCoordinate, ModuleBuild, BuildEdge>> callback) {
        var dagBuilder = dag.toBuilder();
        while (true) {
            Optional<ModuleBuild> moduleBuild = dagBuilder.popRoot();
            if (moduleBuild.isEmpty()) {
                return;
            }

            Vertex<MavenCoordinate, ModuleBuild, BuildEdge> vertex = dag.vertexFromId(moduleBuild.get().module().coordinate());
            callback.accept(vertex);
        }
    }

    private static <V, E> DotId dotIdOf(Vertex<MavenCoordinate, V, E> vertex) {
        return DotId.from(vertex.id().getGroupArtifact().toString());
    }
}
