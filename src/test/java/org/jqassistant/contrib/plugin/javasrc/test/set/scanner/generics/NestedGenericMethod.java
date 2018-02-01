package org.jqassistant.contrib.plugin.javasrc.test.set.scanner.generics;

public class NestedGenericMethod {

    <X, Y extends GenericType<X>> X get(Y value) {
        return null;
    }

}
