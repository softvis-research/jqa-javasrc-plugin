package org.jqassistant.contrib.plugin.javasrc.test.set.scanner.dependency;

public class MigratedDependency {

    public MigratedDependency() {
        InnerClass.staticMethod();
    }

    public void setInnerClass(InnerClass innerClass) {

    }

    public static class InnerClass {
        public static void staticMethod() {
        }
    }
}
