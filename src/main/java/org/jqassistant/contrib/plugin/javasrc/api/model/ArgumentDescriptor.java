package org.jqassistant.contrib.plugin.javasrc.api.model;

import com.buschmais.xo.neo4j.api.annotation.Property;

/**
 * Defines a descriptor containing line number information.
 */
public interface ArgumentDescriptor {

    @Property("arguments")
    String getArguments();

    void setArguments(String arguments);

}
