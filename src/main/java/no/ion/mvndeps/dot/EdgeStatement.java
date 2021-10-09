package no.ion.mvndeps.dot;

import java.util.List;
import java.util.stream.Collectors;

public class EdgeStatement implements DotStatement {
    private final DotId from;
    private final DotId to;
    private final List<DotAttribute> attributes;

    public EdgeStatement(DotId from, DotId to, List<DotAttribute> attributes) {
        this.from = from;
        this.to = to;
        this.attributes = List.copyOf(attributes);
    }

    public DotId from() { return from; }
    public DotId to() { return to; }
    public List<DotAttribute> attributes() { return attributes; }

    @Override
    public void render(DotWriter writer) {
        writer.append(from + " -> " + to);

        if (!attributes.isEmpty()) {
            String suffix = attributes.stream()
                                      .map(DotAttribute::toString)
                                      .collect(Collectors.joining(",", " [", "]"));
            writer.append(suffix);
        }

        writer.appendln();
    }
}
