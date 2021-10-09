package no.ion.mvndeps.dot;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DotAttributes {
    private final List<DotAttribute> attributes = new ArrayList<>();

    public DotAttributes() {}

    public DotAttributes add(DotId name, DotId value) {
        attributes.add(DotAttribute.from(name, value));
        return this;
    }

    @Override
    public String toString() {
        return attributes.stream().map(DotAttribute::toString).collect(Collectors.joining(","));
    }
}
