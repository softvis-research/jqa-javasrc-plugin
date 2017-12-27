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
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.*;
import org.unileipzig.jqassistant.plugin.parser.api.model.*;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.JavaScope;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
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
                TypeDescriptor typeDescriptor = this.handleType(typeDeclaration, javaSourceFileDescriptor);
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
        } else if (resolved instanceof TypeDeclaration) {
            // just put it through the resolver
            resolved = resolver.resolve((TypeDeclaration) resolved);
        }
        // we need to differentiate between JavaParserClassDeclaration and ReflectionClassDeclaration
        // - e.g. for a JavaParserClassDeclaration, we WANT TO know everything really detailed
        // - e.g. for a ReflectionClassDeclaration, we DON'T, since it's a builtin class (treat it as a dependency)
        if (Utils.whichSolverWasUsed(resolved) == Utils.SolverType.JavaParserSolver) {
            if (resolved instanceof ResolvedClassDeclaration) {
                return this.handleClass((ResolvedClassDeclaration) resolved);
            } else if (resolved instanceof ResolvedInterfaceDeclaration) {
                return this.handleInterface((ResolvedInterfaceDeclaration) resolved);
            } else if (resolved instanceof ResolvedEnumDeclaration) {
                return this.handleEnum((ResolvedEnumDeclaration) resolved);
            } else if (resolved instanceof ResolvedAnnotationDeclaration) {
                return this.handleAnnotation((ResolvedAnnotationDeclaration) resolved);
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
                throw new RuntimeException("WARNING: Unexpected Resolvable: " + resolved.getClass());
            }
            // HANDLE DEPENDENCIES -> THUS NEED TO KNOW WHICH IS THE THING THAT DEPENDS ON THIS (+1 PARAMETER)
            // TODO: methods may have ambiguities, will need to use signature instead of FQN as ID for them!
            if (resolver.has(fullyQualifiedName)) {
                //System.out.println("resolver has " + fullyQualifiedName);
                return resolver.get(fullyQualifiedName, TypeDescriptor.class);
            } else if (possiblyDependencyOf != null) {
                //System.out.println("will need to add TypeDescriptor (as dependency) Object for " + fullyQualifiedName);
                return resolver.addDependency(possiblyDependencyOf, fullyQualifiedName);
            }
        }
        return null; // should never end up here
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
        String qualifiedName = resolvedClassLike.getQualifiedName();
        if (resolver.has(qualifiedName)) return resolver.get(qualifiedName, concreteDescriptor);
        ClassFileDescriptor classLikeDescriptor = resolver.create(qualifiedName, concreteDescriptor);
        // basic metadata
        classLikeDescriptor.setFullQualifiedName(qualifiedName);
        classLikeDescriptor.setName(resolvedClassLike.getName());
        if (resolvedClassLike instanceof HasAccessSpecifier && !(resolvedClassLike instanceof ResolvedEnumDeclaration)) {
            classLikeDescriptor.setVisibility(Utils.modifierToString(((HasAccessSpecifier) resolvedClassLike).accessSpecifier()));
        }
        // get members (= methods and fields)
        // (subclasses of MemberDescriptor: MethodDescriptor and FieldDescriptor, thus NOT inner classes)
        //List<MemberDescriptor> memberDescriptors = classLikeDescriptor.getDeclaredMembers();
        List<MethodDescriptor> methodDescriptors = classLikeDescriptor.getDeclaredMethods();
        List<FieldDescriptor> fieldDescriptors = classLikeDescriptor.getDeclaredFields();
        if (!(resolvedClassLike instanceof ResolvedEnumDeclaration) // can't call getDeclaredFields() for enum- and annotation-declarations!
            && !(resolvedClassLike instanceof ResolvedAnnotationDeclaration)) {
            for (ResolvedFieldDeclaration f : resolvedClassLike.getDeclaredFields()) {
                String qualifiedFieldName = Utils.fullyQualifiedFieldName(f);
                FieldDescriptor fieldDescriptor = resolver.create(qualifiedFieldName, FieldDescriptor.class);
                fieldDescriptors.add(fieldDescriptor);
                //memberDescriptors.add(fieldDescriptor);
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
                    // handle annotations
                    handleAnnotations(fD.getAnnotations(), fieldDescriptor);
                }
                //System.out.println("Resolved FieldDeclaration: " + qualifiedFieldName + " type: " + f.getType());
            }
        }
        if (!(resolvedClassLike instanceof ResolvedAnnotationDeclaration)) { // getDeclaredMethods() for annotation-declarations throws UnsupportedOperationException
            for (ResolvedMethodDeclaration m : resolvedClassLike.getDeclaredMethods()) {
                MethodDescriptor methodDescriptor = handleMethodLike(m);
                //memberDescriptors.add(methodDescriptor); // caused redundancy when calling MethodDescriptor.getDeclaredMethods()!
                methodDescriptors.add(methodDescriptor);

            }
        }

        // get inner classes
        Set<TypeDescriptor> innerClassDescriptors = classLikeDescriptor.getDeclaredInnerClasses();
        if (!(resolvedClassLike instanceof ResolvedAnnotationDeclaration)) { // internalTypes() for annotation-declarations throws UnsupportedOperationException
            for (ResolvedReferenceTypeDeclaration internalType : resolvedClassLike.internalTypes()) {
                innerClassDescriptors.add(this.handleType(internalType, classLikeDescriptor));
            }
        }
        // get annotations
        NodeList<AnnotationExpr> annotations = new NodeList<>();
        if (resolvedClassLike instanceof JavaParserClassDeclaration) {
            annotations = ((JavaParserClassDeclaration) resolvedClassLike).getWrappedNode().getAnnotations();
        } else if (resolvedClassLike instanceof JavaParserEnumDeclaration) {
            annotations = ((JavaParserEnumDeclaration) resolvedClassLike).getWrappedNode().getAnnotations();
        } else if (resolvedClassLike instanceof JavaParserInterfaceDeclaration) {
            annotations = ((JavaParserInterfaceDeclaration) resolvedClassLike).getWrappedNode().getAnnotations();
        }
        /*else if (resolvedClassLike instanceof JavaParserAnnotationDeclaration) {  // has no getWrappedNode()!
            //wrapped = ((JavaParserAnnotationDeclaration) resolvedClassLike).getWrappedNode();
        }*/
        handleAnnotations(annotations, classLikeDescriptor);
        return classLikeDescriptor;
    }

    /**
     * Create either MethodDescriptor or ConstructorDescriptor (XO) from ResolvedMethodLikeDeclaration (JavaParser)
     */
    private MethodDescriptor handleMethodLike(ResolvedMethodLikeDeclaration m) {
        String qualifiedSignature = m.getQualifiedSignature();
        MethodDescriptor methodDescriptor;
        if (m instanceof ResolvedMethodDeclaration) {
            if (resolver.has(qualifiedSignature)) return resolver.get(qualifiedSignature, MethodDescriptor.class);
            methodDescriptor = resolver.create(qualifiedSignature, MethodDescriptor.class);
        } else {
            assert (m instanceof ResolvedConstructorDeclaration);
            if (resolver.has(qualifiedSignature)) return resolver.get(qualifiedSignature, ConstructorDescriptor.class);
            methodDescriptor = resolver.create(qualifiedSignature, ConstructorDescriptor.class);
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
            // handle annotations for parameter
            if (p instanceof JavaParserParameterDeclaration) {
                Parameter parameter = ((JavaParserParameterDeclaration) p).getWrappedNode();
                handleAnnotations(parameter.getAnnotations(), parameterDescriptor);
            }
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
            // handle (specified) thrown exceptions (otherwise it would need to be retrieved from body statements)
            List<TypeDescriptor> declaredThrowables = methodDescriptor.getDeclaredThrowables();
            for (ResolvedType e : m.getSpecifiedExceptions()) {
                declaredThrowables.add(this.handleType(e, methodDescriptor));
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
            // handle annotations
            handleAnnotations(mD.getAnnotations(), methodDescriptor);
            // handle body statements
            mD.getBody().ifPresent((body) -> this.handleBody(body, methodDescriptor));
        } else if (m instanceof JavaParserConstructorDeclaration) {
            ConstructorDeclaration cD = ((JavaParserConstructorDeclaration) m).getWrappedNode();
            // handle body statements for constructor (in this case not behind Optional<>)
            this.handleBody(cD.getBody(), methodDescriptor);
        } else {
            assert (m instanceof ResolvedConstructorDeclaration); // actually Default Constructor (not explicitly defined)
        }
        return methodDescriptor;
    }

    /**
     * Things of interest in a method body:
     * - assignments to Field objects (WritesDescriptor)
     * ..... - FieldDescriptor#getWrittenBy()
     * ..... - MethodDescriptor#getWrites()
     * - accessing Field objects (ReadsDescriptor)
     * ..... - FieldDescriptor#getReadBy()
     * ..... - MethodDescriptor#getReads()
     * - method calls (InvokesDescriptor)
     * ..... - MethodDescriptor#getInvokes()
     * ..... - MethodDescriptor#getInvokedBy()
     * - "inner" classes (actually nested classes and anonymous classes)
     * - declared Variables/Fields (?)
     */
    private void handleBody(BlockStmt body, MethodDescriptor ofMethod) {
        body.accept(new VoidVisitorAdapter<Void>() {
            /*@Override // written fields
            public void visit(AssignExpr assignExpr, Void arg) {
                // see https://github.com/javaparser/javasymbolsolver/issues/300
                // probably best to wait for that pull request? https://github.com/javaparser/javasymbolsolver/pull/357
                Expression target = assignExpr.getTarget(), value = assignExpr.getValue();
                ResolvedType type = JavaParserFacade.get(resolver.typeSolver).getType(target);
                // we need a ResolvedFieldDeclaration from target
                //System.out.println(JavaParserFacade.get(resolver.typeSolver).solve(target));
                System.out.println(target.getClass());
                super.visit(assignExpr, arg);
            }*/
            @Override // method calls
            public void visit(MethodCallExpr methodCallExpr, Void arg) {
                ResolvedMethodDeclaration resolvedMethodDeclaration = resolver.resolve(methodCallExpr);
                //System.out.println("resolvedMethodDeclaration: " + resolvedMethodDeclaration+ "--->"+resolvedMethodDeclaration.getClass());
                MethodDescriptor calledMethodDescriptor = handleMethodLike(resolvedMethodDeclaration);
                InvokesDescriptor invokesDescriptor = resolver.store.create(ofMethod, InvokesDescriptor.class, calledMethodDescriptor);
                methodCallExpr.getBegin().ifPresent((pos) -> invokesDescriptor.setLineNumber(pos.line));
                super.visit(methodCallExpr, arg);
            }

            @Override // nested and anonymous classes
            public void visit(ClassOrInterfaceDeclaration classDeclaration, Void arg) {
                TypeDescriptor typeDescriptor = handleType(classDeclaration, ofMethod);
                ofMethod.getDeclaredInnerClasses().add(typeDescriptor);
                super.visit(classDeclaration, arg);
            }
        }, null);
    }

    /**
     * Create ClassTypeDescriptor (XO) from ResolvedClassDeclaration (JavaParser)
     */
    private ClassTypeDescriptor handleClass(ResolvedClassDeclaration resolvedClass) {
        String qualifiedName = resolvedClass.getQualifiedName();
        if (resolver.has(qualifiedName)) return resolver.get(qualifiedName, ClassTypeDescriptor.class);
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
        //List<MemberDescriptor> memberDescriptors = classDescriptor.getDeclaredMembers(); // is it a member??
        List<MethodDescriptor> methodDescriptors = classDescriptor.getDeclaredMethods();
        for (ResolvedConstructorDeclaration constructorDeclaration : resolvedClass.getConstructors()) {
            MethodDescriptor methodDescriptor = handleMethodLike(constructorDeclaration);
            //memberDescriptors.add(methodDescriptor); // is it a member??
            methodDescriptors.add(methodDescriptor);
        }
        return classDescriptor;
    }

    /**
     * Create EnumTypeDescriptor (XO) from ResolvedEnumDeclaration (JavaParser)
     */
    private EnumTypeDescriptor handleEnum(ResolvedEnumDeclaration resolvedEnum) {
        String qualifiedName = resolvedEnum.getQualifiedName();
        if (resolver.has(qualifiedName)) return resolver.get(qualifiedName, EnumTypeDescriptor.class);
        EnumTypeDescriptor enumDescriptor = (EnumTypeDescriptor) handleClassLike(resolvedEnum, EnumTypeDescriptor.class);
        int i = 0;
        for (ResolvedEnumConstantDeclaration enumConstant : resolvedEnum.getEnumConstants()) {
            // JavaParser throws NullPointerException when calling getDeclaredFields() -> need to translate enum constants to Fields manually
            String name = enumConstant.getName();
            String qualifiedFieldName = enumDescriptor.getFullQualifiedName() + "." + name;
            FieldDescriptor fieldDescriptor = resolver.create(qualifiedFieldName, FieldDescriptor.class);
            PrimitiveValueDescriptor primitiveValueDescriptor = store.create(PrimitiveValueDescriptor.class);
            EnumValueDescriptor valueDescriptor = store.create(EnumValueDescriptor.class);
            primitiveValueDescriptor.setValue(++i); // NOT SURE ABOUT THAT...
            fieldDescriptor.setName(name);
            fieldDescriptor.setValue(primitiveValueDescriptor);
            valueDescriptor.setName(name);
            valueDescriptor.setType(enumDescriptor);
            valueDescriptor.setValue(fieldDescriptor);
            enumDescriptor.getDeclaredFields().add(fieldDescriptor);
        }
        return enumDescriptor;
    }

    /**
     * Create InterfaceTypeDescriptor (XO) from ResolvedInterfaceDeclaration (JavaParser)
     */
    private InterfaceTypeDescriptor handleInterface(ResolvedInterfaceDeclaration resolvedInterface) {
        String qualifiedName = resolvedInterface.getQualifiedName();
        if (resolver.has(qualifiedName)) return resolver.get(qualifiedName, InterfaceTypeDescriptor.class);
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
        // in handleClassLike(), getDeclaredMethods() and getDeclaredFields() throw UnsupportedOperationException for ResolvedAnnotationDeclaration!!
        String qualifiedName = resolvedAnnotation.getQualifiedName();
        if (resolver.has(qualifiedName)) return resolver.get(qualifiedName, AnnotationTypeDescriptor.class);
        AnnotationTypeDescriptor annotationDescriptor = (AnnotationTypeDescriptor) handleClassLike(resolvedAnnotation, AnnotationTypeDescriptor.class);
        List<MethodDescriptor> declaredMethods = annotationDescriptor.getDeclaredMethods();
        for (ResolvedAnnotationMemberDeclaration m : resolvedAnnotation.getAnnotationMembers()) {
            MethodDescriptor methodDescriptor = store.create(MethodDescriptor.class);
            methodDescriptor.setName(m.getName());
            methodDescriptor.setSignature(m.getName() + "()");
            // other than getName(), most methods of ResolvedAnnotationMemberDeclaration throw UnsupportedOperationException, thus operate on wrapped node
            assert (m instanceof JavaParserAnnotationMemberDeclaration);
            JavaParserAnnotationMemberDeclaration annotationMemberWrapping = (JavaParserAnnotationMemberDeclaration) m;
            AnnotationMemberDeclaration annotationMember = annotationMemberWrapping.getWrappedNode();
            annotationMember.getDefaultValue().ifPresent((defaultValue) -> {
                PrimitiveValueDescriptor primitiveValueDescriptor = store.create(PrimitiveValueDescriptor.class);
                primitiveValueDescriptor.setName(m.getName());
                primitiveValueDescriptor.setValue(defaultValue.toString());
                methodDescriptor.setHasDefault(primitiveValueDescriptor);
            });
            TypeDescriptor memberType = this.handleType(annotationMember.getType().resolve(), annotationDescriptor);
            methodDescriptor.setReturns(memberType);
            //System.out.println("add " + methodDescriptor.getSignature() + " to " + annotationDescriptor.getName());
            declaredMethods.add(methodDescriptor);
        }
        return annotationDescriptor;
    }

    /**
     * fill AnnotatedDescriptor's (XO) annotationDescriptors from lists of AnnotationExpr
     * Problem: "a" + "b" is actually a BinaryExpr -> no way to store that with current Descriptor classes!
     * ---> there are not many alternative variants of ValueDescriptor, more would need to be added
     * ----> for now, everything is stored as PrimitiveValueDescriptor with the toString() representation used as value
     */
    private void handleAnnotations(NodeList<AnnotationExpr> annotationExpressions, AnnotatedDescriptor descriptor) {
        List<AnnotationValueDescriptor> annotationDescriptors = descriptor.getAnnotatedBy();
        for (AnnotationExpr annotationExpr : annotationExpressions) {
            TypeDescriptor annotationType = this.handleType(resolver.resolve(annotationExpr), descriptor);
            AnnotationValueDescriptor annotationValueDescriptor = store.create(AnnotationValueDescriptor.class);
            List<ValueDescriptor<?>> values = new LinkedList<>();
            if (annotationExpr instanceof SingleMemberAnnotationExpr) {
                SingleMemberAnnotationExpr singleMemberAnnotationExpr = (SingleMemberAnnotationExpr) annotationExpr;
                singleMemberAnnotationExpr.getName();
                Expression v = singleMemberAnnotationExpr.getMemberValue();
                PrimitiveValueDescriptor primitiveValueDescriptor = store.create(PrimitiveValueDescriptor.class);
                primitiveValueDescriptor.setName("value"); // always "value" for SingleMemberAnnotationExpr
                primitiveValueDescriptor.setValue(v.toString());
                values.add(primitiveValueDescriptor);
                //System.out.println("single member annotation: " + v.toString() + " // " + v.getClass().getName());
            } else if (annotationExpr instanceof NormalAnnotationExpr) {
                for (MemberValuePair pair : ((NormalAnnotationExpr) annotationExpr).getPairs()) {
                    PrimitiveValueDescriptor primitiveValueDescriptor = store.create(PrimitiveValueDescriptor.class);
                    primitiveValueDescriptor.setName(pair.getName().toString());
                    primitiveValueDescriptor.setValue(pair.getValue().toString());
                    values.add(primitiveValueDescriptor);
                    //System.out.println("expression value pair: " + pair.getName() + "->" + pair.getValue());
                }
            }
            annotationValueDescriptor.setName(annotationExpr.getNameAsString());
            annotationValueDescriptor.setType(annotationType);
            annotationValueDescriptor.setValue(values);
            annotationDescriptors.add(annotationValueDescriptor);
            //System.out.println(annotationExpr);
        }
    }
}
