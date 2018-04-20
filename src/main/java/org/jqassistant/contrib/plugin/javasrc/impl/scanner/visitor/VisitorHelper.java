package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import java.util.Iterator;

import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.plugin.common.api.model.ValueDescriptor;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import org.apache.commons.lang.StringUtils;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotatedDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotationValueDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.ClassTypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.ConstructorDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.FieldDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.InvokesDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceFileDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.MethodDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.ParameterDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.ReadsDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.VariableDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.WritesDescriptor;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.JavaTypeResolver;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.JavaTypeSolver;

/**
 * The helper delegates creation and caching of types to the type resolver,
 * holds a reference of the type solver and its facade, and provides the
 * visitors with descriptors.
 * 
 * @author Richard Mueller
 *
 */
public class VisitorHelper {

    private ScannerContext scannerContext;
    private JavaSourceFileDescriptor javaSourceFileDescriptor;
    private JavaParserFacade facade;
    private TypeSolver typeSolver;
    public final String CONSTRUCTOR_NAME = "<init>";
    public final String VOID = "void";
    public final String CONSTRUCTOR_SIGNATURE = "void <init>";
    public final String ANNOTATION_MEMBER_SIGNATURE = "()";
    public final String ANNOTATION_MEMBER_DEFAULT_VALUE_NAME = "null";
    public final String SINGLE_MEMBER_ANNOTATION_NAME = "value";
    private int anonymousInnerClassCounter;

    public VisitorHelper(ScannerContext scannerContext, JavaSourceFileDescriptor javaSourceFileDescriptor) {
        this.scannerContext = scannerContext;
        this.javaSourceFileDescriptor = javaSourceFileDescriptor;
        this.facade = getJavaTypeSolver().getFacade();
        this.typeSolver = getJavaTypeSolver().getTypeSolver();
    }

    public <T extends TypeDescriptor> T createType(String fqn, Class<T> type) {
        // set anonymous inner class counter for every new type
        setAnonymousInnerClassCounter(1);
        return getTypeResolver().createType(fqn, getJavaSourceFileDescriptor(), type);
    }

    public <T extends TypeDescriptor> T createAnonymousType(MethodDescriptor methodDescriptor) {
        String fqn = methodDescriptor.getDeclaringType().getFullQualifiedName() + "$" + getAnonymousInnerClassCounter();
        // increase anonymous inner class counter
        increaseAnonymousInnerClassCounter();
        return getTypeResolver().createType(fqn, getJavaSourceFileDescriptor(), (Class<T>) ClassTypeDescriptor.class);
    }

    public TypeDescriptor resolveDependency(String fqn, TypeDescriptor dependent) {
        return getTypeResolver().resolveDependency(fqn, dependent);
    }

    public MethodDescriptor getMethodDescriptor(String signature, TypeDescriptor parent) {
        MethodDescriptor methodDescriptor = null;
        for (Iterator<MethodDescriptor> iterator = parent.getDeclaredMethods().iterator(); iterator.hasNext();) {
            Object member = iterator.next();
            if (member instanceof MethodDescriptor) {
                MethodDescriptor existingMethodDescriptor = (MethodDescriptor) member;
                if (existingMethodDescriptor.getSignature().equals(signature)) {
                    methodDescriptor = existingMethodDescriptor;
                }
            }
        }
        if (methodDescriptor != null) {
            return methodDescriptor;
        }
        if (signature.startsWith(CONSTRUCTOR_SIGNATURE)) {
            methodDescriptor = scannerContext.getStore().create(ConstructorDescriptor.class);
            methodDescriptor.setName(CONSTRUCTOR_NAME);
        } else {
            methodDescriptor = scannerContext.getStore().create(MethodDescriptor.class);
            methodDescriptor.setName(StringUtils.substringBetween(signature, " ", "("));
        }
        methodDescriptor.setSignature(signature);
        parent.getDeclaredMethods().add(methodDescriptor);
        return methodDescriptor;
    }

    public FieldDescriptor getFieldDescriptor(String signature, TypeDescriptor parent) {
        FieldDescriptor fieldDescriptor = null;
        for (Iterator<FieldDescriptor> iterator = parent.getDeclaredFields().iterator(); iterator.hasNext();) {
            Object member = iterator.next();
            if (member instanceof FieldDescriptor) {
                FieldDescriptor existingFieldDescriptor = (FieldDescriptor) member;
                if (existingFieldDescriptor.getSignature().equals(signature)) {
                    fieldDescriptor = existingFieldDescriptor;
                }
            }
        }
        if (fieldDescriptor != null) {
            return fieldDescriptor;
        }
        fieldDescriptor = scannerContext.getStore().create(FieldDescriptor.class);
        fieldDescriptor.setName(signature.substring(signature.indexOf(" ") + 1));
        fieldDescriptor.setSignature(signature);
        parent.getDeclaredFields().add(fieldDescriptor);
        return fieldDescriptor;
    }

    public ParameterDescriptor getParameterDescriptor(MethodDescriptor methodDescriptor, int index) {
        ParameterDescriptor parameterDescriptor = scannerContext.getStore().create(ParameterDescriptor.class);
        parameterDescriptor.setIndex(index);
        methodDescriptor.getParameters().add(parameterDescriptor);
        return parameterDescriptor;
    }

    public <T extends ValueDescriptor<?>> T getValueDescriptor(Class<T> valueDescriptorType) {
        return scannerContext.getStore().create(valueDescriptorType);
    }

    public AnnotationValueDescriptor getAnnotationValueDescriptor(String fqn, String name, AnnotatedDescriptor annotatedDescriptor) {
        if (annotatedDescriptor != null) {
            AnnotationValueDescriptor annotationValueDescriptor = scannerContext.getStore().create(AnnotationValueDescriptor.class);
            annotationValueDescriptor.setType(resolveDependency(fqn, null));
            annotationValueDescriptor.setName(name);
            annotatedDescriptor.getAnnotatedBy().add(annotationValueDescriptor);
            return annotationValueDescriptor;
        } else {
            AnnotationValueDescriptor annotationValueDescriptor = scannerContext.getStore().create(AnnotationValueDescriptor.class);
            annotationValueDescriptor.setType(resolveDependency(fqn, null));
            annotationValueDescriptor.setName(name);
            return annotationValueDescriptor;
        }
    }

    public VariableDescriptor getVariableDescriptor(String name, String signature) {
        VariableDescriptor variableDescriptor = scannerContext.getStore().create(VariableDescriptor.class);
        variableDescriptor.setName(name);
        variableDescriptor.setSignature(signature);
        return variableDescriptor;
    }

    public void addInvokes(MethodDescriptor methodDescriptor, final Integer lineNumber, MethodDescriptor invokedMethodDescriptor) {
        InvokesDescriptor invokesDescriptor = scannerContext.getStore().create(methodDescriptor, InvokesDescriptor.class, invokedMethodDescriptor);
        invokesDescriptor.setLineNumber(lineNumber);
    }

    public void addWrites(MethodDescriptor methodDescriptor, final Integer lineNumber, FieldDescriptor fieldDescriptor) {
        WritesDescriptor writesDescriptor = scannerContext.getStore().create(methodDescriptor, WritesDescriptor.class, fieldDescriptor);
        writesDescriptor.setLineNumber(lineNumber);
    }

    public void addReads(MethodDescriptor methodDescriptor, final Integer lineNumber, FieldDescriptor fieldDescriptor) {
        ReadsDescriptor readsDescriptor = scannerContext.getStore().create(methodDescriptor, ReadsDescriptor.class, fieldDescriptor);
        readsDescriptor.setLineNumber(lineNumber);
    }

    public void storeDependencies() {
        getTypeResolver().addDependencies();
    }

    public JavaParserFacade getFacade() {
        return this.facade;
    }

    public TypeSolver getTypeSolver() {
        return this.typeSolver;
    }

    /**
     * Returns the type resolver.
     * <p>
     * Looks up an instance in the scanner context. If none can be found the
     * default resolver is used.
     * </p>
     *
     * @return The type resolver.
     */
    private JavaTypeResolver getTypeResolver() {
        JavaTypeResolver typeResolver = scannerContext.peek(JavaTypeResolver.class);
        if (typeResolver == null) {
            throw new IllegalStateException("Cannot find XO type resolver.");
        }
        return typeResolver;
    }

    /**
     * Returns the type solver.
     * <p>
     * Looks up an instance in the scanner context. If none can be found the
     * default resolver is used.
     * </p>
     *
     * @return The type solver.
     */
    private JavaTypeSolver getJavaTypeSolver() {
        JavaTypeSolver javaTypeResolver = scannerContext.peek(JavaTypeSolver.class);
        if (javaTypeResolver == null) {
            throw new IllegalStateException("Cannot find Java type resolver.");
        }
        return javaTypeResolver;
    }

    private JavaSourceFileDescriptor getJavaSourceFileDescriptor() {
        return this.javaSourceFileDescriptor;
    }

    private void setAnonymousInnerClassCounter(int value) {
        this.anonymousInnerClassCounter = value;
    }

    private int getAnonymousInnerClassCounter() {
        return this.anonymousInnerClassCounter;
    }

    private void increaseAnonymousInnerClassCounter() {
        this.anonymousInnerClassCounter++;
    }
}
