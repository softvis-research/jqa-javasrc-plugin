package org.jqassistant.contrib.plugin.javasrc.test.set.scanner.metric;

/**
 * Defines a class for verifying calculation of cyclomatic complexity metrics of
 * the class scanner.
 */
public class CyclomaticComplexityType {

    public CyclomaticComplexityType() {
    }

    public void ifStatement(boolean value) {
        if (value) {

        } else {

        }
    }

    public void caseStatement(int value) {
        switch (value) {
        case 0:
            break;
        case 1:
            break;
        default:
        }
    }
}
