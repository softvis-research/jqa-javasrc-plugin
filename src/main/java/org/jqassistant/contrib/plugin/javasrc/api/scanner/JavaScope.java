package org.jqassistant.contrib.plugin.javasrc.api.scanner;

import com.buschmais.jqassistant.core.scanner.api.Scope;

/**
 * Defines the scopes for java.
 */
public enum JavaScope implements Scope {

    CLASSPATH, SRC;

    @Override
    public String getPrefix() {
        return "java";
    }

    @Override
    public String getName() {
        return name();
    }

}
