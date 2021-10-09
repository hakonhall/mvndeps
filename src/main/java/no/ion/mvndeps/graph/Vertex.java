package no.ion.mvndeps.graph;

import java.util.ArrayList;
import java.util.List;

public class Vertex<ID, V, E> {
    private final ID id;
    private final V payload;
    private final List<Edge<ID, V, E>> incomingEdges;
    private final List<Edge<ID, V, E>> outgoingEdges = new ArrayList<>();

    public Vertex(ID id, V payload, List<Vertex<ID, V, E>> fromVertices) {
        this.id = id;
        this.payload = payload;

        var incomingEdges = new ArrayList<Edge<ID, V, E>>();
        for (Vertex<ID, V, E> fromVertex : fromVertices) {
            Edge<ID, V, E> edge = new Edge<>(fromVertex, this);
            incomingEdges.add(edge);
            fromVertex.addOutgoingEdge(edge);
        }
        this.incomingEdges = List.copyOf(incomingEdges);
    }

    public ID id() { return id; }
    public V get() { return payload; }

    public List<Edge<ID, V, E>> incoming() { return incomingEdges; }
    public List<Edge<ID, V, E>> outgoing() { return List.copyOf(outgoingEdges); }

    private void addOutgoingEdge(Edge<ID, V, E> edge) {
        outgoingEdges.add(edge);
    }
}
