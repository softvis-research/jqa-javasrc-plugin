package org.jqassistant.contrib.plugin.javasrc.test.set.scanner.innerclass;

public class NestedInnerClasses {

    public void doSomething() {
        new FirstLevel().doSomething();
    }

    public class FirstLevel {

        public void doSomething() {
            new SecondLevel().doSomething();
        }

        public class SecondLevel {

            public void doSomething() {
            }

        }
    }

}
