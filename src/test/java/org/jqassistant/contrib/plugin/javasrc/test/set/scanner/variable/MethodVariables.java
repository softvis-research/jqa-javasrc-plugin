package org.jqassistant.contrib.plugin.javasrc.test.set.scanner.variable;

import java.util.List;

public class MethodVariables {
    final boolean boolField = false;
    String stringField = "stringField";

    public MethodVariables() {
        // four variables
        String stringVariable = "string";
        Integer intVariable = 2;
        char charArray[] = { 'a', 'b' };
        long longVariable;
        stringField = stringVariable;
        List<Double> stringList;

    }

    public boolean methodWithVariables() {
        // five variables
        int intVariable = 1;
        String stringVariable = stringField;
        Double doubleVariable = 1.9;
        char charArray[] = { 'a', 'b' };
        long longVariable;
        List<Double> stringList;
        return boolField;
    }
}
