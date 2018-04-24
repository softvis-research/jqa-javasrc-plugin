package org.jqassistant.contrib.plugin.javasrc.test.set.scanner.access;

public class FieldAccess {
    private int a = 0;
    private int b = 0;
    private int c = 0;
    private int d = 0;

    public int getA() {
        return a; // read a at 10
    }

    public void setA(int a) {
        this.a = a; // write a at 14
        int c = 4; // not scanned as it is no field but a variable
    }

    public int getB() {
        return this.b; // read b at 19
    }

    public void setB(int value) {
        b = value; // write b at 23
    }

    public void setC() {
        c = d; // read a at 27, write c at 27
    }

    public void setD() {
        setB(d); // read d at 31 = currently not supported
    }
}
