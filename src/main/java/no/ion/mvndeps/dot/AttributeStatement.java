package no.ion.mvndeps.dot;

public class AttributeStatement implements DotStatement {
    private final DotAttribute attribute;

    AttributeStatement(DotAttribute attribute) {
        this.attribute = attribute;
    }

    @Override
    public void render(DotWriter writer) {
        writer.appendln(attribute.toString());
    }
}
