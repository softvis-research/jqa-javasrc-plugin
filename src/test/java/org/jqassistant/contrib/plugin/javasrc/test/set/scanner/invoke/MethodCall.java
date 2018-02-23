package org.jqassistant.contrib.plugin.javasrc.test.set.scanner.invoke;

public class MethodCall {
    public void callingMethod() {
        this.calledMethod1();
        (new InnerClass()).calledMethod2();
        class NestedClass {
            public void calledMethod0() {

            }
        }
        (new NestedClass()).calledMethod0();
    }

    public void calledMethod1() {
    }

    class InnerClass {
        public void calledMethod2() {

        }
    }
}
