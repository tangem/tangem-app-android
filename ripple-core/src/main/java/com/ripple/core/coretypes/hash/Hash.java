package com.ripple.core.coretypes.hash;

import com.ripple.core.serialized.BinaryParser;
import com.ripple.core.serialized.BytesSink;
import com.ripple.core.serialized.SerializedType;
import com.ripple.core.serialized.TypeTranslator;
import com.ripple.encodings.common.B16;

import java.math.BigInteger;
import java.util.Arrays;

abstract public class Hash<Subclass extends Hash> implements SerializedType, Comparable<Subclass> {
    protected final byte[] hash;

    public Hash(byte[] bytes, int size) {
        hash = checkHash(bytes, size);
    }

    @Override
    public String toString() {
        return B16.encode(hash);
    }

    @Override
    public int hashCode() {
        // Having this lazily computed and then cached would give significant
        // performance boosts in some single threaded contexts. Given the inputs
        // for the method are at least effectively final, and the method is
        // essentially a pure function, it seems probably safe to do. It
        // seems at worst, that in a multi threaded context, some extra work
        // will be done in rare cases?
        // TODO: check this assumption
        return Arrays.hashCode(hash);
    }

    private byte[] checkHash(byte[] bytes, int size) {
        int length = bytes.length;
        if (length > size) {
            throwIllegalArg(length, "wide");
        } else if (length == size) {
            // TODO: What costs for this ?
            return bytes.clone();
        } else {
            throwIllegalArg(length, "small");
        }
        throw new IllegalStateException("Can not get here");
    }

    private void throwIllegalArg(int length, String wrongness) {
        String simpleName = getClass().getSimpleName();
        throw new IllegalArgumentException("Hash length of " + length + "bytes  is too " +
                wrongness +
                " for " + simpleName);
    }

    BigInteger bigInteger() {
        return new BigInteger(1, hash);
    }

    // TODO: Is a defensive copy worthwhile? Or just be adults? Never been a
    // problem in practice, though others may not know to avoid mutating the
    // returned array. Pity Java doesn't have something like C++ const.
    // If you did it in one place, should do it EVERY WHERE, inputs/outputs and
    // on every type that stores mutable arrays internally and the perf and
    // maybe memory (at least GC stress) costs could add up.
    // It's a bit like polishing a tur*.
    public byte[] bytes() {
        return hash.clone();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Hash) {
            return Arrays.equals(hash, ((Hash) obj).hash);
        }

        return super.equals(obj);
    }

    @Override
    public int compareTo(Subclass another) {
        byte[] thisBytes = bytes();
        byte[] bytes = another.bytes();

        return compareBytes(thisBytes, bytes, 0, thisBytes.length);
    }

    public int compareStartingAt(Subclass another, int start) {
        byte[] thisBytes = bytes();
        byte[] bytes = another.bytes();

        return compareBytes(thisBytes, bytes, start, thisBytes.length);
    }

    private int compareBytes(byte[] thisBytes, byte[] bytes, int start, int numBytes) {
        int thisLength = thisBytes.length;
        if (!(bytes.length == thisLength)) {
            throw new RuntimeException();
        }

        for (int i = start; i < numBytes; i++) {
            int cmp = (thisBytes[i] & 0xFF) - (bytes[i] & 0xFF);
            if (cmp != 0) {
                return cmp < 0 ? -1 : 1;
            }
        }
        return 0;
    }

    public byte[] slice(int start) {
        return slice(start, 0);
    }

    public byte get(int i) {
        if (i < 0) i += hash.length;
        return hash[i];
    }

    private byte[] slice(int start, int end) {
        if (start < 0)  start += hash.length;
        if (end  <= 0)  end   += hash.length;

        int length = end - start;
        byte[] slice = new byte[length];

        System.arraycopy(hash, start, slice, 0, length);
        return slice;
    }

    static public abstract class HashTranslator<T extends Hash> extends TypeTranslator<T> {

        public abstract T newInstance(byte[] b);
        public abstract int byteWidth();

        @Override
        public T fromParser(BinaryParser parser, Integer hint) {
            return newInstance(parser.read(byteWidth()));
        }

        @Override
        public Object toJSON(T obj) {
            return B16.encode(obj.hash);
        }

        @Override
        public T fromString(String value) {
            return newInstance(B16.decode(value));
        }

        @Override
        public void toBytesSink(T obj, BytesSink to) {
            to.add(obj.bytes());
        }
    }
}
