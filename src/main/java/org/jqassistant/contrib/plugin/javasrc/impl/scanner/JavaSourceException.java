package org.jqassistant.contrib.plugin.javasrc.impl.scanner;

/**
 * This exception is thrown if something goes wrong while solving parsed types.
 * It is catched in
 * {@link org.jqassistant.contrib.plugin.javasrc.impl.scanner.JavaSourceFileScannerPlugin}.
 * 
 * @author Richard Mueller
 *
 */
public class JavaSourceException extends RuntimeException {
    private static final long serialVersionUID = 44798050316448167L;

    public JavaSourceException(String string) {
        super(string);
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
