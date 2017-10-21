package org.unileipzig.jqassistant.plugin.parser.api.model;

import com.buschmais.xo.api.annotation.Abstract;
import com.buschmais.xo.neo4j.api.annotation.Label;

@Abstract
@Label("Property")
public interface PropertyDescriptor extends DeclarationDescriptor {
    boolean getIsWritable();
    boolean setIsWritable(boolean b);
    boolean getIsStatic();
    boolean setIsStatic(boolean b);
}
