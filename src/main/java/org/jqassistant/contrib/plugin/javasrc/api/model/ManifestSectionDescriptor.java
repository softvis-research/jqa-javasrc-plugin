package org.jqassistant.contrib.plugin.javasrc.api.model;

import java.util.List;

import com.buschmais.jqassistant.plugin.common.api.model.NamedDescriptor;
import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;
import com.buschmais.xo.neo4j.api.annotation.Relation.Outgoing;

@Label("ManifestSection")
public interface ManifestSectionDescriptor extends JavaDescriptor, NamedDescriptor {

    @Relation("HAS")
    @Outgoing
    List<ManifestEntryDescriptor> getManifestEntries();

}
