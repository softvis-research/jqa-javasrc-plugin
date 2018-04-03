package org.jqassistant.contrib.plugin.javasrc.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;

/**
 * Describes a parameter of a method.
 */
@Label(value = "Parameter")
public interface ParameterDescriptor extends JavaSourceCodeDescriptor, TypedDescriptor, AnnotatedDescriptor {

    @Property("index")
    int getIndex();

    void setIndex(int index);

}
