package org.unileipzig.jqassistant.plugin.parser.lib;

import java.util.List;

@FunctionalInterface
public interface RedShiftReducer {
    boolean reduce(List<Object> stack, List<Token> tokens);
}
