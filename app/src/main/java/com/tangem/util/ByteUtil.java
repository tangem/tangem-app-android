package com.tangem.util;

public class ByteUtil {

    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    public static byte[] and(byte[] b1, byte[] b2) {
        if (b1.length != b2.length) throw new RuntimeException("Array sizes differ");
        byte[] ret = new byte[b1.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = (byte) (b1[i] & b2[i]);
        }
        return ret;
    }

    public static byte[] or(byte[] b1, byte[] b2) {
        if (b1.length != b2.length) throw new RuntimeException("Array sizes differ");
        byte[] ret = new byte[b1.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = (byte) (b1[i] | b2[i]);
        }
        return ret;
    }

    public static boolean isNullOrZeroArray(byte[] array){
        return (array == null) || (array.length == 0);
    }

    public static boolean isSingleZero(byte[] array){
        return (array.length == 1 && array[0] == 0);
    }

    public static int length(byte[]... bytes) {
        int result = 0;
        for (byte[] array : bytes) {
            result += (array == null) ? 0 : array.length;
        }
        return result;
    }
}