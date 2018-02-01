package org.jqassistant.contrib.plugin.javasrc.api.model;

import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import com.buschmais.jqassistant.plugin.common.api.model.PropertyDescriptor;
import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

import java.util.List;

/**
 * A descriptor representing a property file.
 */
@Label(value = "Properties")
public interface PropertyFileDescriptor extends JavaDescriptor, FileDescriptor {

    @Relation("HAS")
    List<PropertyDescriptor> getProperties();

}
