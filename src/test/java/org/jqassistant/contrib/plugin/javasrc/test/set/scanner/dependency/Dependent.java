package org.jqassistant.contrib.plugin.javasrc.test.set.scanner.dependency;

public class Dependent {

    private Dependency dependency;

    public Dependent(Dependency value) {
        this.dependency = new Dependency();
    }

    public Dependency getDependency() {
        return dependency;
    }

    public void setDependency(Dependency dependency) {
        this.dependency = dependency;
    }

}
