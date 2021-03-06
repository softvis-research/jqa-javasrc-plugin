package org.jqassistant.contrib.plugin.javasrc.test.set.scanner.annotation;

import static org.jqassistant.contrib.plugin.javasrc.test.set.scanner.annotation.Enumeration.DEFAULT;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An annotation containing values of all supported types.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Annotation {

    String value();

    String[] arrayValue() default {};

    Class<?> classValue() default Object.class;

    Enumeration enumerationValue() default DEFAULT;

    NestedAnnotation nestedAnnotationValue() default @NestedAnnotation("default");

    NestedAnnotation[] nestedAnnotationValues() default @NestedAnnotation("default");

    NestedAnnotation nestedNormalAnnotationValue() default @NestedAnnotation(value = "default");
}
