package no.ion.mvndeps.dot;

import java.util.List;
import java.util.stream.Collectors;

public class NodeStatement implements DotStatement {
    private final DotId id;
    private final List<DotAttribute> attributes;

    public NodeStatement(DotId id, List<DotAttribute> attributes) {
        this.id = id;
        this.attributes = List.copyOf(attributes);
    }

    public DotId id() { return id; }
    public List<DotAttribute> attributes() { return attributes; }

    @Override
    public void render(DotWriter writer) {
        writer.append(id.toString());

        if (!attributes.isEmpty()) {
            String suffix = attributes.stream()
                                      .map(DotAttribute::toString)
                                      .collect(Collectors.joining(",", " [", "]"));
            writer.append(suffix);
        }

        writer.appendln();
    }
}
