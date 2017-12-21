package org.unileipzig.jqassistant.plugin.parser.impl.scanner;

import com.buschmais.jqassistant.core.scanner.api.Scanner;
import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.core.scanner.api.ScannerPlugin.Requires;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import com.buschmais.jqassistant.plugin.common.api.scanner.AbstractScannerPlugin;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FileResource;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.*;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserClassDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserConstructorDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserFieldDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;
import org.unileipzig.jqassistant.plugin.parser.api.model.*;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.JavaScope;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Requires(FileDescriptor.class)
public class JavaSourceFileScannerPlugin extends AbstractScannerPlugin<FileResource, JavaSourceFileDescriptor> {
    private Resolver resolver;
    private Store store;

    @Override
    public boolean accepts(FileResource item, String path, Scope scope) throws IOException {
        return JavaScope.CLASSPATH.equals(scope) && path.toLowerCase().endsWith(".java");
    }

    @Override
    public JavaSourceFileDescriptor scan(FileResource item, String path, Scope scope, Scanner scanner) throws IOException {
        ScannerContext context = scanner.getContext();
        store = context.getStore();
        resolver = context.peek(Resolver.class); // get it from context, it should be the same object throughout
        FileDescriptor fileDescriptor = context.getCurrentDescriptor();
        JavaSourceFileDescriptor javaSourceFileDescriptor = store.addDescriptorType(fileDescriptor, JavaSourceFileDescriptor.class);
        // parse files and determine concrete types (i.e. class, interface, annotation or enum)
        try (InputStream in = item.createStream()) {
            CompilationUnit cu = JavaParser.parse(in);
            Optional<PackageDeclaration> packageDeclaration = cu.getPackageDeclaration();
            String packageName = packageDeclaration.isPresent() ? packageDeclaration.get().getNameAsString() : "";
            List<TypeDescriptor> typeDescriptors = javaSourceFileDescriptor.getTypes();
            for (TypeDeclaration<?> typeDeclaration : cu.getTypes()) {
                TypeDescriptor typeDescriptor = this.handleType(resolver.resolve(typeDeclaration), javaSourceFileDescriptor);
                if (typeDescriptor != null) typeDescriptors.add(typeDescriptor);
                else throw new RuntimeException("WARNING: couldn't handle type in " + cu);
            }
        }
        return javaSourceFileDescriptor;
    }

    /**
     * Create TypeDescriptor (XO) from a Resolved* Object (JavaParser)
     * From jqa-java-plugin docs:
     * # *NOTE* The full set of information is only available for class files which
     * # have actually been scanned. Types which are only referenced (i.e. from
     * # external libraries not included in the scan) are represented by `:Type` nodes with a
     * # property `fqn` and `DECLARES` relations to their members. These are `:Field` or
     * # `:Method` labeled nodes which only provide the property `signature`.
     *
     * @param resolved             can be either ResolvedType or ResolvedDeclaration
     * @param possiblyDependencyOf the object that would have a dependency on it if it is an external or builtin type
     */
    private TypeDescriptor handleType(Object resolved, Descriptor possiblyDependencyOf) {
        TypeDescriptor descriptor = null;
        if (resolved instanceof ResolvedArrayType) {
            // how array are currently handled in jqa-java-plugin: see SignatureHelper#getType(org.objectweb.asm.Type)
            // "case Type.ARRAY: return getType(t.getElementType());"
            // thus doing the same, here!
            ResolvedType arrayOf = ((ResolvedArrayType) resolved).getComponentType();
            resolved = resolver.resolve(arrayOf);
        }
        if (resolved instanceof ResolvedReferenceType) {
            // just put it through the resolver
            resolved = resolver.resolve((ResolvedReferenceType) resolved);
        }
        // we need to differentiate between JavaParserClassDeclaration and ReflectionClassDeclaration
        // - e.g. for a JavaParserClassDeclaration, we WANT TO know everything really detailed
        // - e.g. for a ReflectionClassDeclaration, we DON'T, since it's a builtin class (treat it as a dependency)
        if (Utils.whichSolverWasUsed(resolved) == Utils.SolverType.JavaParserSolver) {
            if (resolved instanceof ResolvedClassDeclaration) {
                descriptor = this.handleClass((ResolvedClassDeclaration) resolved);
            } else if (resolved instanceof ResolvedInterfaceDeclaration) {
                descriptor = this.handleInterface((ResolvedInterfaceDeclaration) resolved);
            } else if (resolved instanceof ResolvedEnumDeclaration) {
                descriptor = this.handleEnum((ResolvedEnumDeclaration) resolved);
            } else if (resolved instanceof ResolvedAnnotationDeclaration) {
                descriptor = this.handleAnnotation((ResolvedAnnotationDeclaration) resolved);
            } else {
                throw new RuntimeException("WARNING: Unexpected Resolvable: " + resolved);
            }
        } else {
            // get the fullyQualifiedName, create a TypeDescriptor class implying it is to be treated as a dependency
            String fullyQualifiedName = "";
            if (resolved instanceof ResolvedVoidType) {
                fullyQualifiedName = Void.class.getCanonicalName();
            } else if (resolved instanceof ResolvedPrimitiveType) {
                fullyQualifiedName = ((ResolvedPrimitiveType) resolved).getBoxTypeQName();
            } else if (resolved instanceof ResolvedTypeDeclaration) {
                fullyQualifiedName = ((ResolvedTypeDeclaration) resolved).getQualifiedName();
            } else {
                throw new RuntimeException("WARNING: Unexpected Resolvable: " + resolved);
            }
            // HANDLE DEPENDENCIES -> THUS NEED TO KNOW WHICH IS THE THING THAT DEPENDS ON THIS (+1 PARAMETER)
            // TODO: methods may have ambiguities, will need to use signature instead of FQN as ID for them!
            if (resolver.has(fullyQualifiedName)) {
                //System.out.println("resolver has " + fullyQualifiedName);
            } else if (possiblyDependencyOf != null) {
                //System.out.println("will need to add TypeDescriptor (as dependency) Object for " + fullyQualifiedName);
                resolver.addDependency(possiblyDependencyOf, fullyQualifiedName);
            }
        }
        return descriptor;
    }

    /**
     * Create ClassFileDescriptor (or sth. that derives from it) (XO) from ResolvedReferenceTypeDeclaration (JavaParser)
     * Subclasses of ClassFileDescriptor:
     * - ClassTypeDescriptor
     * - InterfaceTypeDescriptor
     * - EnumTypeDescriptor
     * - AnnotationTypeDescriptor
     */
    private ClassFileDescriptor handleClassLike(ResolvedReferenceTypeDeclaration resolvedClassLike,
                                                Class<? extends ClassFileDescriptor> concreteDescriptor) {
        String fullyQualifiedName = resolvedClassLike.getQualifiedName();
        if (resolver.has(fullyQualifiedName)) return resolver.get(fullyQualifiedName, concreteDescriptor);
        ClassFileDescriptor classLikeDescriptor = resolver.create(fullyQualifiedName, concreteDescriptor);
        // basic metadata
        classLikeDescriptor.setFullQualifiedName(fullyQualifiedName);
        classLikeDescriptor.setName(resolvedClassLike.getName());
        if (resolvedClassLike instanceof HasAccessSpecifier) {
            classLikeDescriptor.setVisibility(Utils.modifierToString(((HasAccessSpecifier) resolvedClassLike).accessSpecifier()));
        }
        // get members (= methods and fields)
        // (subclasses of MemberDescriptor: MethodDescriptor and FieldDescriptor, thus NOT inner classes)
        List<MemberDescriptor> memberDescriptors = classLikeDescriptor.getDeclaredMembers();
        List<MethodDescriptor> methodDescriptors = classLikeDescriptor.getDeclaredMethods();
        List<FieldDescriptor> fieldDescriptors = classLikeDescriptor.getDeclaredFields();
        for (ResolvedMethodDeclaration m : resolvedClassLike.getDeclaredMethods()) {
            MethodDescriptor methodDescriptor = handleMethodLike(m);
            memberDescriptors.add(methodDescriptor);
            methodDescriptors.add(methodDescriptor);
        }
        for (ResolvedFieldDeclaration f : resolvedClassLike.getDeclaredFields()) {
            String fullyQualifiedFieldName = f.declaringType().getQualifiedName() + "." + f.getName();
            FieldDescriptor fieldDescriptor = resolver.create(fullyQualifiedFieldName, FieldDescriptor.class);
            fieldDescriptors.add(fieldDescriptor);
            memberDescriptors.add(fieldDescriptor);
            TypeDescriptor fieldTypeDescriptor = this.handleType(f.getType(), fieldDescriptor);
            fieldDescriptor.setType(fieldTypeDescriptor);
            fieldDescriptor.setVisibility(Utils.modifierToString(f.accessSpecifier()));
            fieldDescriptor.setStatic(f.isStatic());
            // some information we can only get from JavaParserFieldDeclaration.wrappedNode (FieldDeclaration)
            if (f instanceof JavaParserFieldDeclaration) {
                //fieldDescriptor.setSynthetic();//?fieldDescriptor.setSignature();//?
                FieldDeclaration fD = ((JavaParserFieldDeclaration) f).getWrappedNode();
                fieldDescriptor.setTransient(fD.isTransient());
                fieldDescriptor.setFinal(fD.isFinal());
            }
            //System.out.println("Resolved FieldDeclaration: " + fullyQualifiedFieldName + " type: " + f.getType());
        }
        // get inner classes
        Set<TypeDescriptor> innerClassDescriptors = classLikeDescriptor.getDeclaredInnerClasses();
        for (ResolvedReferenceTypeDeclaration internalType : resolvedClassLike.internalTypes()) {
            innerClassDescriptors.add(this.handleType(internalType, classLikeDescriptor));
        }
        return classLikeDescriptor;
    }

    /**
     * Create either MethodDescriptor or ConstructorDescriptor (XO) from ResolvedMethodLikeDeclaration (JavaParser)
     */
    private MethodDescriptor handleMethodLike(ResolvedMethodLikeDeclaration m) {
        MethodDescriptor methodDescriptor;
        if (m instanceof ResolvedMethodDeclaration) {
            methodDescriptor = resolver.create(Utils.fullyQualifiedSignature(m), MethodDescriptor.class);
        } else {
            assert (m instanceof ResolvedConstructorDeclaration);
            methodDescriptor = resolver.create(Utils.fullyQualifiedSignature(m), ConstructorDescriptor.class);
        }
        methodDescriptor.setName(m.getName());
        methodDescriptor.setSignature(m.getSignature());
        methodDescriptor.setVisibility(Utils.modifierToString(m.accessSpecifier()));
        // handle parameters
        List<ParameterDescriptor> parameterDescriptors = methodDescriptor.getParameters();
        for (int i = 0; i < m.getNumberOfParams(); i++) {
            ResolvedParameterDeclaration p = m.getParam(i);
            ParameterDescriptor parameterDescriptor = store.create(ParameterDescriptor.class);
            parameterDescriptor.setIndex(i);
            TypeDescriptor parameterTypeDescriptor = this.handleType(p.getType(), parameterDescriptor);
            parameterDescriptor.setType(parameterTypeDescriptor);
            // name seems to (currently!) not be relevant for ParameterDescriptor
            // interesting: hasVariadicParameter ...  seems also currently not to be relevant
            parameterDescriptors.add(parameterDescriptor);
        }
        // handle (specified) return type (it would also be possible to be retrieved from body statements)
        if (m instanceof ResolvedMethodDeclaration) {
            methodDescriptor.setStatic(((ResolvedMethodDeclaration) m).isStatic());
            methodDescriptor.setAbstract(((ResolvedMethodDeclaration) m).isAbstract());
            TypeDescriptor returnTypeDescriptor = this.handleType(((ResolvedMethodDeclaration) m).getReturnType(), methodDescriptor);
            methodDescriptor.setReturns(returnTypeDescriptor);
            // handle (specified) thrown exceptions (otherwise needs to be retrieved from body statements)
            for (ResolvedType e : m.getSpecifiedExceptions()) {
                System.out.println("TODO: resolve exception" + e);
            }
        }
        // some information we can only get from JavaParserMethodDeclaration.wrappedNode (MethodDeclaration)
        // for example, ResolvedMethodDeclaration itself has no reference in any way to the method's body
        if (m instanceof JavaParserMethodDeclaration) {
            methodDescriptor.setFinal(false);
            methodDescriptor.setNative(false);
            MethodDeclaration mD = ((JavaParserMethodDeclaration) m).getWrappedNode();
            for (Modifier modifier : mD.getModifiers()) {
                switch (modifier) { // we need only to handle those that where not possible to compute above
                    case FINAL:
                        methodDescriptor.setFinal(true);
                        break;
                    case NATIVE:
                        methodDescriptor.setNative(true);
                        break;
                }
            }
            // handle body statements
            mD.getBody().ifPresent((body) -> this.handleStatements(body.getStatements(), methodDescriptor));
        } else {
            assert (m instanceof JavaParserConstructorDeclaration);
            ConstructorDeclaration cD = ((JavaParserConstructorDeclaration) m).getWrappedNode();
            // handle body statements for constructor (in this case not behind Optional<>)
            this.handleStatements(cD.getBody().getStatements(), methodDescriptor);
        }
        return methodDescriptor;
    }

    private void handleStatements(List<Statement> statements, MethodDescriptor methodDescriptor) {
        for (Statement statement : statements) {
            System.out.println(statement);
        }
    }

    /**
     * Create ClassTypeDescriptor (XO) from ResolvedClassDeclaration (JavaParser)
     */
    private ClassTypeDescriptor handleClass(ResolvedClassDeclaration resolvedClass) {
        ClassTypeDescriptor classDescriptor = (ClassTypeDescriptor) handleClassLike(resolvedClass, ClassTypeDescriptor.class);
        // get superclasses / implemented interfaces
        TypeDescriptor superClassDescriptor = this.handleType(resolvedClass.getSuperClass(), classDescriptor);
        classDescriptor.setSuperClass(superClassDescriptor);
        List<TypeDescriptor> interfaces = classDescriptor.getInterfaces();
        for (ResolvedReferenceType interfaceType : resolvedClass.getInterfaces()) {
            TypeDescriptor interfaceDescriptor = this.handleType(interfaceType, classDescriptor);
            interfaces.add(interfaceDescriptor);
        }
        if (resolvedClass instanceof JavaParserClassDeclaration) {
            ClassOrInterfaceDeclaration cD = ((JavaParserClassDeclaration) resolvedClass).getWrappedNode();
            classDescriptor.setAbstract(cD.isAbstract());
            classDescriptor.setFinal(cD.isFinal());
            classDescriptor.setStatic(cD.isStatic());
        }
        // get constructor(s)
        List<MemberDescriptor> memberDescriptors = classDescriptor.getDeclaredMembers(); // is it a member??
        List<MethodDescriptor> methodDescriptors = classDescriptor.getDeclaredMethods();
        for (ResolvedConstructorDeclaration constructorDeclaration : resolvedClass.getConstructors()) {
            MethodDescriptor methodDescriptor = handleMethodLike(constructorDeclaration);
            memberDescriptors.add(methodDescriptor); // is it a member??
            methodDescriptors.add(methodDescriptor);
        }
        return classDescriptor;
    }

    /**
     * Create EnumTypeDescriptor (XO) from ResolvedEnumDeclaration (JavaParser)
     */
    private EnumTypeDescriptor handleEnum(ResolvedEnumDeclaration resolvedEnum) {
        EnumTypeDescriptor enumDescriptor = (EnumTypeDescriptor) handleClassLike(resolvedEnum, EnumTypeDescriptor.class);
        for (ResolvedEnumConstantDeclaration enumConstant : resolvedEnum.getEnumConstants()) {
            //enumDescriptor.set...()? // seems like there is no attribute for that, yet?
            System.out.println("Enum would have those:" + enumConstant);
        }
        return enumDescriptor;
    }

    /**
     * Create InterfaceTypeDescriptor (XO) from ResolvedInterfaceDeclaration (JavaParser)
     */
    private InterfaceTypeDescriptor handleInterface(ResolvedInterfaceDeclaration resolvedInterface) {
        InterfaceTypeDescriptor interfaceDescriptor = (InterfaceTypeDescriptor) handleClassLike(resolvedInterface, InterfaceTypeDescriptor.class);
        List<TypeDescriptor> interfaces = interfaceDescriptor.getInterfaces();
        for (ResolvedReferenceType extendedInterfaceType : resolvedInterface.getInterfacesExtended()) {
            TypeDescriptor extendedInterfaceDescriptor = this.handleType(extendedInterfaceType, interfaceDescriptor);
            interfaces.add(extendedInterfaceDescriptor);
        }
        return interfaceDescriptor;
    }

    /**
     * Create AnnotationTypeDescriptor (XO) from ResolvedAnnotationDeclaration (JavaParser)
     */
    private AnnotationTypeDescriptor handleAnnotation(ResolvedAnnotationDeclaration resolvedAnnotation) {
        // what resolvedAnnotation.getAnnotationMembers() does is wrappedNode.getMembers()...filter()...
        // --> thus it should be already handled in handleClassLike()
        /*AnnotationTypeDescriptor annotationDescriptor = (AnnotationTypeDescriptor) handleClassLike(resolvedAnnotation, AnnotationTypeDescriptor.class);
        for (ResolvedAnnotationMemberDeclaration annotationMember : resolvedAnnotation.getAnnotationMembers()) {}*/
        return (AnnotationTypeDescriptor) handleClassLike(resolvedAnnotation, AnnotationTypeDescriptor.class);
    }
}
