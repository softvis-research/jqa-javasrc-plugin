package org.unileipzig.jqassistant.plugin.parser.test.set.scanner.generics;

public class NestedGenericMethod {

    <X, Y extends GenericType<X>> X get(Y value) {
        return null;
    }

}
