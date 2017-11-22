package org.unileipzig.jqassistant.plugin.parser.impl.scanner;

import java.util.HashMap;
import java.util.Map;

import org.unileipzig.jqassistant.plugin.parser.api.model.JavaSourceFileDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.MethodDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.TypeDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.TypeResolver;

import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.plugin.common.api.scanner.FileResolver;

public class TypeResolverImpl implements TypeResolver {

    private Map<String, TypeDescriptor> containedTypes = new HashMap<>();

    private Map<String, TypeDescriptor> requiredTypes = new HashMap<>();

    @Override
    public <T extends TypeDescriptor> T createType(String fqn, JavaSourceFileDescriptor fileDescriptor, Class<T> type, ScannerContext scannerContext) {
        TypeDescriptor resolvedTypeDescriptor = fileDescriptor.resolveType(fqn);
        T typeDescriptor = scannerContext.getStore().addDescriptorType(resolvedTypeDescriptor, type);
        containedTypes.put(fqn, typeDescriptor);
        requiredTypes.remove(fqn);
        return typeDescriptor;
    }

    @Override
    public TypeDescriptor resolveType(String fqn, ScannerContext scannerContext) {
        TypeDescriptor typeDescriptor = containedTypes.get(fqn);
        if (typeDescriptor == null) {
            String fileName = "/" + fqn.replace('.', '/') + ".java"; // Inner classes?
            FileResolver fileResolver = scannerContext.peek(FileResolver.class);
            JavaSourceFileDescriptor sourceFileDescriptor = fileResolver.require(fileName, JavaSourceFileDescriptor.class, scannerContext);
            typeDescriptor = sourceFileDescriptor.resolveType(fqn);
            requiredTypes.put(fqn, typeDescriptor);
        }
        return typeDescriptor;
    }

    @Override
    public MethodDescriptor resolveMethod(String fqn, String signature, ScannerContext scannerContext) {
        TypeDescriptor typeDescriptor = resolveType(fqn, scannerContext);

        return null;
    }

}
