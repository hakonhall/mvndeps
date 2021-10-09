package no.ion.mvndeps.build;

public class Scaler {
    private final double inMin;
    private final double inMax;
    private final int outMin;
    private final int outMax;

    Scaler(double inMin, double inMax, int outMin, int outMax) {
        this.inMin = inMin;
        this.inMax = inMax;
        this.outMin = outMin;
        this.outMax = outMax;
    }

    /* Linear interpolation with in <= inMin returns outMin, in >= inMax returns outMax. */
    int scale(double in) {
        double scaled = (outMax - outMin) * (cap(inMin, in, inMax) - inMin) / (inMax - inMin) + outMin;
        return (int) Math.round(scaled);
    }

    private static double cap(double min, double value, double max) {
        return Math.max(min, Math.min(value, max));
    }
}
