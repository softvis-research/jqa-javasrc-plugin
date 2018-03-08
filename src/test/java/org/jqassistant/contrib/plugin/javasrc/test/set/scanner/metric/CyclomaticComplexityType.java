package org.jqassistant.contrib.plugin.javasrc.test.set.scanner.metric;

/**
 * Defines a class for verifying calculation of cyclomatic complexity metrics of
 * the class scanner.
 */
public class CyclomaticComplexityType {

    public CyclomaticComplexityType() {// complexity = 1
    }

    public void ifStatement(boolean value) {// complexity = 2
        if (value) {

        } else {

        }
    }

    public void nestedIfStatement(int value) {// complexity = 4
        if (value == 0) {

        } else if (value > 0) {
            if (value == 1) {

            } else {

            }
        } else {

        }
    }

    public void caseStatement(int value) {// complexity = 5
        switch (value) {
        case 0:
            break;
        case 1:
            break;
        default:
        }
    }

    void baseComplexity() { // complexity = 1
        highComplexity();
    }

    void highComplexity() { // complexity = 11
        int x = 0, y = 2, t = 2;
        boolean a = false, b = true, d = false;

        if (a || (y == 1 ? b : true)) { // +3
            if (y == x) { // +1
                while (true) { // +1
                    if (x++ < 20) { // +1
                        break; // +1
                    }
                }
            } else if (y == t && !d) { // +2
                x = a ? y : x; // +1
            } else {
                x = 2;
            }
        }
    }
}
