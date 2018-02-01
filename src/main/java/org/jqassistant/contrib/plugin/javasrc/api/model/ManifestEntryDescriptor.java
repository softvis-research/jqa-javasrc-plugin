package org.jqassistant.contrib.plugin.javasrc.api.model;

import com.buschmais.jqassistant.plugin.common.api.model.ValueDescriptor;
import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;

@Label("ManifestEntry")
public interface ManifestEntryDescriptor extends ValueDescriptor<String> {

    /**
     * Return the value.
     *
     * @return The value.
     */
    @Property("value")
    String getValue();

    /**
     * Set the value.
     *
     * @param value The value.
     */
    void setValue(String value);
}
