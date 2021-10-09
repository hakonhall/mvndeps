package no.ion.mvndeps.dot;

import java.util.ArrayList;
import java.util.List;

public class Digraph {
    private final DotId id;
    private final List<DotStatement> statements = new ArrayList<>();

    public Digraph(DotId id) {
        this.id = id;
    }

    public Digraph addAttribute(DotAttribute attribute) {
        statements.add(new AttributeStatement(attribute));
        return this;
    }

    public Digraph addNode(DotId id, List<DotAttribute> attributes) {
        statements.add(new NodeStatement(id, attributes));
        return this;
    }

    public Digraph addEdge(DotId from, DotId to, List<DotAttribute> attributes) {
        statements.add(new EdgeStatement(from, to, attributes));
        return this;
    }

    @Override
    public String toString() {
        var writer = new DotWriter();
        writer.appendln("digraph " + id + " {")
              .indent();
        statements.forEach(statement -> statement.render(writer));
        writer.outdent()
              .appendln("}");
        return writer.toString();
    }
}
