package org.jqassistant.contrib.plugin.javasrc.api.model;

import com.buschmais.jqassistant.core.store.api.model.FullQualifiedNameDescriptor;
import com.buschmais.jqassistant.plugin.common.api.model.DirectoryDescriptor;
import com.buschmais.jqassistant.plugin.common.api.model.FileContainerDescriptor;
import com.buschmais.xo.neo4j.api.annotation.Label;

/**
 * Describes a Java package.
 */
@Label(value = "Package", usingIndexedPropertyOf = FullQualifiedNameDescriptor.class)
public interface PackageDescriptor extends JavaDescriptor, PackageMemberDescriptor, DirectoryDescriptor, FileContainerDescriptor {
}
