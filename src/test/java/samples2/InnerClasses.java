// example from https://github.com/javaparser/javaparser/issues/962
package samples2;

public class InnerClasses {
    static class Z {
        void z() {
        }

        class Q {
            void q() {
            }
        }

        void b() {
            class B {
            }
        }
    }

    class X {
        class Y {
            void y() {
            }
        }

        void x() {
        }
    }
}

class Xx {
    void xx() {
    }
}
