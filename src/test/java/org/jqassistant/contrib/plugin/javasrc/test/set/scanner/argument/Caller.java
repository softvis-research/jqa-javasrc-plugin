package org.jqassistant.contrib.plugin.javasrc.test.set.scanner.argument;

public class Caller {

    public void callingMethod() {
        Callee callee = new Callee();
        callee.calledMethodWithArgument(new SubClass(), new String("testString"));
        callee.calledMethodWithNullArgument(null);
    }
}
