package no.ion.mvndeps.dot;

public class DotAttribute {
    private final DotId name;
    private final DotId value;

    public static DotAttribute from(String name, String value) {
        return from(DotId.from(name), DotId.from(value));
    }

    public static DotAttribute fromQuoted(String name, String value) {
        return from(DotId.fromQuoted(name), DotId.fromQuoted(value));
    }

    public static DotAttribute from(DotId name, DotId value) {
        return new DotAttribute(name, value);
    }

    private DotAttribute(DotId name, DotId value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString() { return name + "=" + value; }
}
