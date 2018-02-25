package org.jqassistant.contrib.plugin.javasrc.test.set.scanner.access;

public class FieldAccess {
    private int a = 0;
    private int b = 0;

    public int getA() {
        return a; // read field a
    }

    public void setA(int a) {
        this.a = a; // read field a + write field a
        int c = 4; // not scanned as it is no field but a variable
    }

    public int getB() {
        return this.b; // read field b
    }

    public void setB(int value) {
        b = value; // read field b + write field b
    }
}
