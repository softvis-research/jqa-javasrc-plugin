package samples3;

import java.io.IOException;

public class ThrowsExample {
    public void f1() throws IOException {

    }

    class MyException extends Exception {
    }

    public void f2() throws RuntimeException, MyException {

    }
}
