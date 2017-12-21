// example from https://github.com/javaparser/javaparser/issues/962
package samples2;

public class InnerClasses {
    static class Z {
        void z() {
        }

        void b() {
            class B {
            }
        }

        class Q {
            void q() {
            }
        }
    }

    class X {
        void x() {
        }

        class Y {
            void y() {
            }
        }
    }
}

class Xx {
    void xx() {
    }
}
