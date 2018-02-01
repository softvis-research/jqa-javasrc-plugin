package org.jqassistant.contrib.plugin.javasrc.api.model;

import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

import java.util.List;

/**
 *
 */
@Label("ServiceLoader")
public interface ServiceLoaderDescriptor extends JavaDescriptor, FileDescriptor, TypedDescriptor {

    @Relation("CONTAINS")
    List<TypeDescriptor> getContains();
}
