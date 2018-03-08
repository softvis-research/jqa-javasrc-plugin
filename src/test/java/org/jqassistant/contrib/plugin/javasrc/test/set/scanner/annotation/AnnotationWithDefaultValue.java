package org.jqassistant.contrib.plugin.javasrc.test.set.scanner.annotation;

/**
 * Defines an annotation with a value having a default.
 */
public @interface AnnotationWithDefaultValue {

    Class<?> classValue() default Number.class;

    Enumeration enumerationValue() default Enumeration.DEFAULT;

    double primitiveValue() default 0.0;

    Class<?>[] arrayValue() default { Integer.class, Double.class };

    NestedAnnotation annotationValue() default @NestedAnnotation("test");
}
