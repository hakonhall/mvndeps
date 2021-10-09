package no.ion.mvndeps.dot;

import no.ion.mvndeps.graph.Dag;
import no.ion.mvndeps.graph.Edge;
import no.ion.mvndeps.graph.Vertex;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import static no.ion.mvndeps.Exceptions.uncheckIO;

public class DagToDot {
    public static <ID, V, E> void toDotFile(String name,
                                            Dag<ID, V, E> dag,
                                            List<DotAttribute> attributes,
                                            Function<Vertex<ID, V, E>, NodeStatement> toNode,
                                            Function<Edge<ID, V, E>, EdgeStatement> toEdge,
                                            Path outFile) {
        String dot = toDot(name, dag, attributes, toNode, toEdge);
        uncheckIO(() -> Files.writeString(outFile, dot, StandardCharsets.UTF_8));
    }

    public static <ID, V, E> String toDot(String name,
                                          Dag<ID, V, E> dag,
                                          List<DotAttribute> attributes,
                                          Function<Vertex<ID, V, E>, NodeStatement> toNode,
                                          Function<Edge<ID, V, E>, EdgeStatement> toEdge) {
        var digraph = new Digraph(DotId.from(name));
        attributes.forEach(digraph::addAttribute);
        dag.visit(vertex -> {
            NodeStatement nodeStatement = toNode.apply(vertex);
            if (nodeStatement != null)
                digraph.addNode(nodeStatement.id(), nodeStatement.attributes());

            for (var incoming : vertex.incoming()) {
                EdgeStatement edgeStatement = toEdge.apply(incoming);
                if (edgeStatement != null)
                    digraph.addEdge(edgeStatement.from(), edgeStatement.to(), edgeStatement.attributes());
            }
        });

        return digraph.toString();
    }
}
