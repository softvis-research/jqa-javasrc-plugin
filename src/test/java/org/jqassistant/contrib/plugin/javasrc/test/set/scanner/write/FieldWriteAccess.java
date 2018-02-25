package org.jqassistant.contrib.plugin.javasrc.test.set.scanner.write;

public class FieldWriteAccess {
    int a = 0;
    int b = 0;

    void methodAccessingFields() {
        a = 1;
        this.b = 1;
        int c = 4; // should (?) _not_ be recognized as *field*
    }
}
