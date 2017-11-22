package org.unileipzig.jqassistant.plugin.parser.api.scanner;

import org.unileipzig.jqassistant.plugin.parser.api.model.JavaSourceFileDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.MethodDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.TypeDescriptor;

import com.buschmais.jqassistant.core.scanner.api.ScannerContext;

public interface TypeResolver {

    <T extends TypeDescriptor> T createType(String fqn, JavaSourceFileDescriptor fileDescriptor, Class<T> type, ScannerContext scannerContext);

    TypeDescriptor resolveType(String fqn, ScannerContext scannerContext);

    MethodDescriptor resolveMethod(String fqn, String signature, ScannerContext scannerContext);
}
