package no.ion.mvndeps.build;

public class BuildEdge {
    private boolean critical = false;
    private boolean onCriticalPath = false;

    public BuildEdge() {
    }

    boolean critical() { return critical; }
    BuildEdge setCritical(boolean critical) { this.critical = critical; return this; }

    boolean onCriticalPath() { return onCriticalPath; }
    BuildEdge setOnCriticalPath(boolean onCriticalPath) { this.onCriticalPath = onCriticalPath; return this; }
}
