package org.jqassistant.contrib.plugin.javasrc.test.set.scanner.loc;

public class LineCount {
    private int a = 0;

    public LineCount(int a) {
        this.a = a;
    }

    public void add(int b) {
        // add a, b, and c
        this.a = a + b;
    }

    public void emptyMethod() {
    }

}
