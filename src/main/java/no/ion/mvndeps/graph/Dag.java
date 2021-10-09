package no.ion.mvndeps.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * A directed acyclic graph (DAG) of vertices and edges.
 *
 * @param <V> the type of payload held by each vertex
 * @param <E> the type of payload held by each edge
 */
public class Dag<ID, V, E> {
    private final Function<V, ID> idFromPayload;
    private final List<Vertex<ID, V, E>> vertices = new ArrayList<>();
    private final List<Vertex<ID, V, E>> roots = new ArrayList<>();
    private final HashMap<ID, Vertex<ID, V, E>> byId = new HashMap<>();

    public Dag(Function<V, ID> idFromPayload) {
        this.idFromPayload = idFromPayload;
    }

    /** Add a vertex A with the given payload, and having incoming edges defined by the given from-vertices. */
    public Dag<ID, V, E> addVertex(V payload, List<ID> sources) {
        ID id = idFromPayload.apply(payload);
        if (byId.containsKey(id)) {
            throw new IllegalArgumentException("The ID has already been added as a vertex: " + id);
        }

        List<Vertex<ID, V, E>> fromVertices = sources
                .stream()
                .map(fromId ->  requireNonNull(byId.get(fromId), "From-vertex is not part of Dag: " + fromId))
                .collect(Collectors.toList());

        var vertex = new Vertex<>(id, payload, fromVertices);
        vertices.add(vertex);
        if (sources.isEmpty()) {
            roots.add(vertex);
        }
        byId.put(id, vertex);
        return this;
    }

    public int size() { return vertices.size(); }
    public List<Vertex<ID, V, E>> roots() { return List.copyOf(roots); }
    public List<Vertex<ID, V, E>> vertices() { return List.copyOf(vertices); }

    public Vertex<ID, V, E> vertexFromPayload(V payload) { return vertexFromId(idFromPayload.apply(payload)); }

    public Vertex<ID, V, E> vertexFromId(ID id) {
        var vertex = byId.get(id);
        if (vertex == null) {
            throw new IllegalArgumentException("No vertex with given ID in DAG: " + id);
        }
        return vertex;
    }

    /** Visit each node once, starting with the roots (sources). */
    public void visit(Consumer<Vertex<ID, V, E>> visitor) {
        var visited = new HashSet<ID>();
        var pending = new ArrayList<>(roots);

        while (!pending.isEmpty()) {
            var vertex = pending.remove(pending.size() - 1);
            visited.add(vertex.id());
            visitor.accept(vertex);

            for (var outgoingEdge : vertex.outgoing()) {
                var outgoingVertex = outgoingEdge.toVertex();
                if (!visited.contains(outgoingVertex.id())) {
                    pending.add(outgoingVertex);
                }
            }
        }
    }

    public DagBuilder<ID, V, E> toBuilder() {
        var builder = new DagBuilder<ID, V, E>(idFromPayload);

        visit(vertex -> {
            List<ID> sources = vertex.incoming().stream().map(Edge::fromVertex).map(Vertex::id).collect(Collectors.toList());
            builder.addVertex(vertex.get(), sources);
        });

        return builder;
    }
}
