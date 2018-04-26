package org.jqassistant.contrib.plugin.javasrc.test.set.scanner.access;

public class FieldAccess {
    private int a = 0;
    private int b = 0;
    private int c = 0;
    private int d = 0;
    private int e = 0;

    public int getA() {
        return a; // read a at 11
    }

    public void setA(int a) {
        this.a = a; // write a at 15
        int c = 4; // not scanned as it is no field but a variable
    }

    public int getB() {
        return this.b; // read b at 20
    }

    public void setB(int value) {
        b = value; // write b at 24
    }

    public void setC() {
        c = d; // read a at 28, write c at 28
    }

    public void setD() {
        setB(e); // read e at 32
    }

    public void setE() {
        setB(this.c); // read c at 36
    }
}
