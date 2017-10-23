package org.unileipzig.jqassistant.plugin.parser.impl.scanner;

import com.buschmais.jqassistant.core.scanner.api.ScannerPlugin.Requires;
import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import java8.Java8Lexer;
import java8.Java8Parser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenSource;
import org.unileipzig.jqassistant.plugin.parser.api.model.ModuleDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.ModuleScannerPlugin;

@Requires(FileDescriptor.class)
public class JavaModuleScannerPlugin extends ModuleScannerPlugin {

    @Override
    public void read(ModuleDescriptor moduleDescriptor, Store store, String input) {
        TokenSource tokenSource = new Java8Lexer(new ANTLRInputStream(input));
        Java8Parser parser = new Java8Parser(new CommonTokenStream(tokenSource));
        Java8Parser.CompilationUnitContext c = parser.compilationUnit();
        for (Java8Parser.TypeDeclarationContext tdc : c.typeDeclaration()) {
            Java8Parser.ClassDeclarationContext cdc = tdc.classDeclaration();//if(cdc = null) continue;
            Java8Parser.NormalClassDeclarationContext cls = cdc.normalClassDeclaration();
            String name = cls.Identifier().toString();
            System.out.println("NAME" + name);
        }
    }
}
