package no.ion.mvndeps.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class DagBuilder<ID, V, E> {
    private final Function<V, ID> idFromPayload;
    private final Set<ID> roots = new HashSet<>();
    private final HashMap<ID, V> payloads = new HashMap<>();
    private final HashMap<ID, Set<ID>> edges = new HashMap<>();
    private final HashMap<ID, List<ID>> sources = new HashMap<>();
    private final HashMap<ID, Set<ID>> reverseEdges = new HashMap<>();

    public DagBuilder(Function<V, ID> idFromPayload) {
        this.idFromPayload = idFromPayload;
    }

    public DagBuilder<ID, V, E> addVertex(V payload, List<ID> sources) {
        ID id = idFromPayload.apply(payload);

        if (payloads.put(id, payload) != null) {
            throw new IllegalArgumentException("Graph already contains ID: " + id);
        }

        if (this.sources.put(id, sources) != null) {
            throw new IllegalArgumentException("Graph already contains ID: " + id);
        }

        if (sources.isEmpty()) {
            roots.add(id);
        }

        for (var source : sources) {
            // 2 dependencies may have same coordinate and different classifier, so ignore duplicates
            edges.computeIfAbsent(source, __ -> new HashSet<>()).add(id);
            reverseEdges.computeIfAbsent(id, __ -> new HashSet<>()).add(source);
        }

        return this;
    }

    public Optional<V> popRoot() {
        var iterator = roots.iterator();
        if (iterator.hasNext()) {
            ID id = iterator.next();
            V payload = payloads.get(id);
            removeVertex(id);
            return Optional.of(payload);
        } else {
            return Optional.empty();
        }
    }

    private void removeVertex(ID id) {
        Set<ID> incoming = reverseEdges.getOrDefault(id, Set.of());
        Set<ID> outgoing = edges.getOrDefault(id, Set.of());

        payloads.remove(id);
        edges.remove(id);
        reverseEdges.remove(id);
        sources.remove(id);
        roots.remove(id);

        for (var in : incoming) {
            edges.get(in).remove(id);
        }

        for (var out : outgoing) {
            reverseEdges.get(out).remove(id);
            if (reverseEdges.getOrDefault(out, Set.of()).isEmpty()) {
                roots.add(out);
            }
        }
    }

    public Dag<ID, V, E> build() {
        var dag = new Dag<ID, V, E>(idFromPayload);

        while (!roots.isEmpty()) {
            ID id = roots.iterator().next();
            dag.addVertex(payloads.get(id), sources.get(id));
            removeVertex(id);
        }

        if (!payloads.isEmpty()) {
            throw new IllegalStateException("Failed to find all vertices from the roots");
        }

        return dag;
    }
}
