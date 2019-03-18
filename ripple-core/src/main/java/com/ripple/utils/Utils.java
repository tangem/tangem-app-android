package com.ripple.utils;

import com.ripple.encodings.common.B16;

import java.math.BigInteger;
import java.util.Arrays;

public class Utils {
    public static String bigHex(BigInteger bn) {
        return B16.toStringTrimmed(bn.toByteArray());
    }

    public static BigInteger uBigInt(byte[] bytes) {
        return new BigInteger(1, bytes);
    }

    public static byte[] leadingZeroesTrimmedOrPaddedTo(int size, byte[] bytes) {
        if (bytes.length == size) {
            return bytes;
        }
        if (bytes.length > size) {
            int length = bytes.length;
            int offset = 0;
            while (length > size && bytes[offset++] == 0) {
                length--;
            }
            if (length > size) {
                throw new IllegalArgumentException(
                        "bytes.length: " + bytes.length + " > " +
                                size);
            }
            return copyFrom(bytes, offset, size);
        }

        return padTo(bytes, size);
    }

    public static byte[] padTo256(byte[] bytes) {
        return leadingZeroesTrimmedOrPaddedTo(32, bytes);
    }

    public static byte[] padTo160(byte[] bytes) {
        return leadingZeroesTrimmedOrPaddedTo(20, bytes);
    }

    private static byte[] copyFrom(byte[] bytes, int offset, int size) {
        byte[] result = new byte[size];
        System.arraycopy(bytes, offset, result, 0, size);
        return result;
    }

    private static byte[] padTo(byte[] bytes, int size) {
        byte[] result = new byte[size];
        int offset = result.length - bytes.length;
        System.arraycopy(bytes, 0, result, offset, bytes.length);
        return result;
    }
}
