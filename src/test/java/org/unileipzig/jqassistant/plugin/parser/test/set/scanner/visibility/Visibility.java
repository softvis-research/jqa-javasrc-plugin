package org.unileipzig.jqassistant.plugin.parser.test.set.scanner.visibility;

import java.util.List;

public class Visibility {

	public int publicField;

	private boolean privateField;

	protected String protectedField;

	long defaultField;

	public int publicMethod() {
		return 1;
	}

	private void privateMethod() {
	}

	protected boolean protectedMethod() {
		return true;
	}

	void defaultMethod() {
	}

}
