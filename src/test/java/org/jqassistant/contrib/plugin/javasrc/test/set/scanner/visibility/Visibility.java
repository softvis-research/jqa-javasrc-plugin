package org.jqassistant.contrib.plugin.javasrc.test.set.scanner.visibility;

public class Visibility {

    public int publicField;

    private boolean privateField;

    protected String protectedField;

    long defaultField;

    public int publicMethod() {
        return 1;
    }

    private void privateMethod() {
    }

    protected boolean protectedMethod() {
        return true;
    }

    void defaultMethod() {
    }

}
