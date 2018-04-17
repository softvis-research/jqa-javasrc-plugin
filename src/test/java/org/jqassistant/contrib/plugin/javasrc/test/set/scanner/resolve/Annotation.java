package org.jqassistant.contrib.plugin.javasrc.test.set.scanner.resolve;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;

@Retention(RetentionPolicy.RUNTIME)
public @interface Annotation {

    JavaScope enumerationValue() default JavaScope.SRC;

}
