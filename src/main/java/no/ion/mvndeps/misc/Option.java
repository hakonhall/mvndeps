package no.ion.mvndeps.misc;

import no.ion.mvndeps.UsageError;

import java.util.Optional;
import java.util.function.Function;

public class Option<T> {
    private final String name;
    private final boolean required;
    private final boolean hasValue;
    private final Function<String, T> mapper;

    private boolean present = false;
    private Optional<T> value;

    Option(String name, boolean required, boolean hasValue, T defaultValue, Function<String, T> mapper) {
        this.name = name;
        this.required = required;
        this.hasValue = hasValue;
        this.mapper = mapper;
        this.value = Optional.ofNullable(defaultValue);
    }

    public String name() { return name; }
    public boolean isPresent() { return present; }
    public T get() { return value.orElseThrow(); }

    /** May be useful to override the default based on environment or options. */
    public void setValue(String value) { this.value = Optional.of(value).map(mapper); }

    void setPresent() { this.present = true; }

    void validate() {
        if (required) {
            if (!present)
                throw new UsageError("Option " + name() + " is required");
        }

        if (present && value.isEmpty())
            throw new UsageError("Missing value of " + name());
    }

    boolean hasValue() { return hasValue; }
}
