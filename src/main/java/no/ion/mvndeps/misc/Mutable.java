package no.ion.mvndeps.misc;

public class Mutable<T> {
    private T value;

    public Mutable(T value) {
        this.value = value;
    }

    public T get() { return value; }

    public T set(T value) {
        this.value = value;
        return value;
    }
}
