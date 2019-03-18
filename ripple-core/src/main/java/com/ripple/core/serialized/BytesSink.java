package com.ripple.core.serialized;

public interface BytesSink {
    default void add(byte aByte) {
        add(new byte[] {aByte});
    }
    void add(byte[] bytes);
}
