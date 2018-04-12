package org.jqassistant.contrib.plugin.javasrc.test.set.scanner.resolve;

import org.jqassistant.contrib.plugin.javasrc.impl.scanner.ExternalClass;

public class ResolveExternalStaticMethodCall {
    void callExternalStaticMethod() {
        String string = "";
        ExternalClass.externalStaticMethod(string);
    }
}
