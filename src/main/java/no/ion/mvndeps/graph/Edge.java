package no.ion.mvndeps.graph;

public class Edge<ID, V, E> {
    private final Vertex<ID, V, E> from;
    private final Vertex<ID, V, E> to;

    private E value = null;

    public Edge(Vertex<ID, V, E> from, Vertex<ID, V, E> to) {
        this.from = from;
        this.to = to;
    }

    public Vertex<ID, V, E> fromVertex() { return from; }
    public Vertex<ID, V, E> toVertex() { return to; }

    public void set(E value) { this.value = value; }
    public E get() { return value; }

    @Override
    public String toString() { return from + " -> " + to; }
}
