package org.jqassistant.contrib.plugin.javasrc.test.set.scanner.innerclass;

import java.util.Iterator;

public class AnonymousInnerClasses<X> {

    public Iterator<X> iterator1() {
        return new Iterator<X>() {
            int i;

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public X next() {
                return null;
            }

            @Override
            public void remove() {
            }
        };
    }

    public Iterator<X> iterator2() {
        return new Iterator<X>() {
            int i;

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public X next() {
                return null;
            }

            @Override
            public void remove() {
            }
        };
    }
}
