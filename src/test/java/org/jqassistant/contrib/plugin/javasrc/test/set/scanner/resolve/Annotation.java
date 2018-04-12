package org.jqassistant.contrib.plugin.javasrc.test.set.scanner.resolve;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.jqassistant.contrib.plugin.javasrc.impl.scanner.ExternalEnumeration;

@Retention(RetentionPolicy.RUNTIME)
public @interface Annotation {

    ExternalEnumeration enumerationValue() default ExternalEnumeration.DEFAULT;

}
