package org.unileipzig.jqassistant.plugin.parser.test.set.scanner.enumeration;

/**
 * An enum type.
 */
public enum EnumerationType {

    A(true), B(false);

    @SuppressWarnings("unused")
    private boolean value;

    EnumerationType(boolean value) {
        this.value = value;
    }

}
