package org.unileipzig.jqassistant.plugin.parser.test;

import org.junit.Test;
import org.unileipzig.jqassistant.plugin.parser.impl.scanner.Resolver;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ResolverTest {
    @Test
    public void testResolver() throws IOException {
        Resolver r = new Resolver("src", null);
        Set<File> dirs = new HashSet<>();
        r.recursiveSubDirs(new File("src"), dirs);
        System.out.println(dirs);
        System.out.println(r.typeSolver);
    }

}
