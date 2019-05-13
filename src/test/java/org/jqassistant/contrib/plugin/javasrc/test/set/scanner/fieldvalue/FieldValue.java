package org.jqassistant.contrib.plugin.javasrc.test.set.scanner.fieldvalue;

public class FieldValue {

    private static final String stringValue = "StringValue";

    private static final int intValue = 1;

    private static final int intValueHex0x = 0x00efefef;

    private static final int intValueHex0X = 0X00efefef;

    private static final double doubleValueWithd = 0.0d;

    private static final long longValueWithL = 0L;

    private static final long longValueWithl = 0l;

    private static final long negativeLongValueWithL = -3455108052199995234L;

    private static final long binaryValue = 1000 * 60 * 10;

    private static final long nameValue = binaryValue;

    private Object arrayCreationValue = new Byte[0];

    private static int nullValue;

    private static SuperClass fieldWithDifferentStaticAndDynamicType = new SubClass();

}
