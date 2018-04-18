package org.jqassistant.contrib.plugin.javasrc.test.set.scanner.variable;

import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;

public class MethodVariables {
    final boolean boolField = false;
    String stringField = "stringField";

    public MethodVariables() {
        // four variables
        String stringVariable = "string";
        JavaScope scopeVariable = JavaScope.SRC;
        char charArray[] = { 'a', 'b' };
        long longVariable;
        stringField = stringVariable;

    }

    public boolean methodWithVariables() {
        // five variables
        int intVariable = 1;
        String stringVariable = stringField;
        JavaScope scopeVariable = JavaScope.SRC;
        char charArray[] = { 'a', 'b' };
        long longVariable;
        return boolField;
    }
}
