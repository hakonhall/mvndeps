package no.ion.mvndeps;

import no.ion.mvndeps.dot.DotAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DotEdge {
    private final String from;
    private final String to;
    private final List<DotAttribute> attributes = new ArrayList<>();

    /** from and to must be quoted already, if necessary. */
    DotEdge(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public void addAttribute(String name, String value) {
        attributes.add(DotAttribute.from(name, value));
    }

    @Override
    public String toString() {
        var stringBuilder = new StringBuilder();
        stringBuilder.append(from).append(" -> ").append(to);

        if (!attributes.isEmpty()) {
            stringBuilder.append(attributes.stream().map(DotAttribute::toString).collect(Collectors.joining(",", " [", "]")));
        }

        return stringBuilder.toString();
    }
}
