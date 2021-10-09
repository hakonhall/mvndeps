package no.ion.mvndeps.build;

public class BuildDependency {
    private boolean criticalPath = false;

    public BuildDependency() {}

    public void setCriticalPath(boolean criticalPath) { this.criticalPath = criticalPath; }
    public boolean isCriticalPath() { return criticalPath; }
}
