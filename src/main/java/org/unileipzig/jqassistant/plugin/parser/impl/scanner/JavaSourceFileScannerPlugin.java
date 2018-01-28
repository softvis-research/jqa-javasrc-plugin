package org.unileipzig.jqassistant.plugin.parser.impl.scanner;

import com.buschmais.jqassistant.core.scanner.api.Scanner;
import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.core.scanner.api.ScannerPlugin.Requires;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import com.buschmais.jqassistant.plugin.common.api.model.ValueDescriptor;
import com.buschmais.jqassistant.plugin.common.api.scanner.AbstractScannerPlugin;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FileResource;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.*;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.*;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionMethodDeclaration;
import org.unileipzig.jqassistant.plugin.parser.api.model.*;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.JavaScope;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.visitor.FieldVisitor;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.visitor.MethodVisitor;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.visitor.TypeVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Requires(FileDescriptor.class)
public class JavaSourceFileScannerPlugin extends AbstractScannerPlugin<FileResource, JavaSourceFileDescriptor> {
	private TypeResolver typeResolver;
	private Store store;

	@Override
	public boolean accepts(FileResource item, String path, Scope scope) throws IOException {
		return JavaScope.CLASSPATH.equals(scope) && path.toLowerCase().endsWith(".java");
	}

	@Override
	public JavaSourceFileDescriptor scan(FileResource item, String path, Scope scope, Scanner scanner) throws IOException {
		ScannerContext context = scanner.getContext();
		store = context.getStore();
		typeResolver = context.peek(TypeResolver.class); // get it from context, it should be the same object throughout
		FileDescriptor fileDescriptor = context.getCurrentDescriptor();
		JavaSourceFileDescriptor javaSourceFileDescriptor = store.addDescriptorType(fileDescriptor, JavaSourceFileDescriptor.class);
		try (InputStream in = item.createStream()) {
			CompilationUnit cu = JavaParser.parse(in);
			cu.accept(new TypeVisitor(typeResolver), javaSourceFileDescriptor);
			cu.accept(new FieldVisitor(typeResolver), javaSourceFileDescriptor);
			cu.accept(new MethodVisitor(typeResolver), javaSourceFileDescriptor);
		}
		return javaSourceFileDescriptor;
	}
}
