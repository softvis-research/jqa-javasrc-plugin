package org.unileipzig.jqassistant.plugin.parser.impl.scanner;

import java.util.EnumSet;

import org.unileipzig.jqassistant.plugin.parser.api.model.VisibilityModifier;

import com.github.javaparser.ast.Modifier;

public class Utils {
    
	
	
    /**
     *  Returns the VisibilityModifier for an EnumSet<Modifier> from java parser.
     *  
     * @param modifiers
     * @return VisibilityModifier
     */
    public static VisibilityModifier getAccessSpecifier(EnumSet<Modifier> modifiers) {
        if (modifiers.contains(Modifier.PUBLIC)) {
            return VisibilityModifier.PUBLIC;
        } else if (modifiers.contains(Modifier.PROTECTED)) {
            return VisibilityModifier.PROTECTED;
        } else if (modifiers.contains(Modifier.PRIVATE)) {
            return VisibilityModifier.PRIVATE;
        } else {
            return VisibilityModifier.DEFAULT;
        }
    }
}
