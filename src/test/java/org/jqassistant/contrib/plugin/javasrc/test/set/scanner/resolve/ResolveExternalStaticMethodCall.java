package org.jqassistant.contrib.plugin.javasrc.test.set.scanner.resolve;

import java.util.ArrayList;
import java.util.List;

import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.external.ExternalClass;

public class ResolveExternalStaticMethodCall {
    void callExternalStaticMethod() {
        List<String> stringList = new ArrayList<>();
        ExternalClass.externalStaticMethod(stringList);
    }
}
