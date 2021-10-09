package no.ion.mvndeps.graph;

import no.ion.mvndeps.dot.DagToDot;
import no.ion.mvndeps.dot.DotId;
import no.ion.mvndeps.dot.EdgeStatement;
import no.ion.mvndeps.dot.NodeStatement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DagTest {
    private final DagBuilder<String, VertexPayload, EdgePayload> builder = new DagBuilder<>(VertexPayload::toString);

    @Test
    void empty() {
        assertDag("""
                          digraph build {
                          }
                          """);
    }

    @Test
    void oneNode() {
        addVertex("a");
        assertDag("""
                          digraph build {
                            a
                          }
                          """);
    }

    @Test
    void twoNodes() {
        addVertex("a", "b");
        addVertex("b");
        assertDag("""
                          digraph build {
                            b
                            a
                            b -> a
                          }
                          """);
    }

    @Test
    void manyNodes() {
        addVertex("a");
        addVertex("b");
        addVertex("c", "a", "b");
        addVertex("d", "a");
        addVertex("e", "c", "b");
        assertDag("""
                          digraph build {
                            b
                            e
                            c -> e
                            b -> e
                            c
                            a -> c
                            b -> c
                            a
                            d
                            a -> d
                          }
                          """);
    }

    @Test
    void manyNodes2() {
        addVertex("a");
        addVertex("b");
        addVertex("c", "b");
        addVertex("d", "c");
        addVertex("e", "c", "f");
        addVertex("f");
        assertDag("""
                          digraph build {
                            f
                            e
                            c -> e
                            f -> e
                            b
                            c
                            b -> c
                            d
                            c -> d
                            a
                          }
                          """);
    }

    private void assertDag(String expectedDot) {
        var dag = builder.build();

        String actualDot = DagToDot.toDot("build",
                                          dag,
                                          List.of(),
                                          vertex -> new NodeStatement(DotId.from(vertex.id()), List.of()),
                                          edge -> new EdgeStatement(DotId.from(edge.fromVertex().id()),
                                                                    DotId.from(edge.toVertex().id()),
                                                                    List.of()));

        assertEquals(expectedDot, actualDot);
    }

    private void addVertex(String id, String... sources) {
        builder.addVertex(new VertexPayload(id), List.of(sources));
    }

    private static class VertexPayload {
        private final String id;

        public VertexPayload(String id) {
            this.id = id;
        }

        @Override
        public String toString() { return id; }
    }

    private static class EdgePayload {
        private final String id;

        public EdgePayload(String id) {
            this.id = id;
        }

        @Override
        public String toString() { return id; }
    }
}