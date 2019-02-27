package com.tangem.tangemcommon.util;

import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.macs.HMac;
import org.spongycastle.crypto.params.KeyParameter;

import java.security.InvalidKeyException;
import java.util.Arrays;

public final class PBKDF2 {
    private static final HMac F =new HMac(new SHA256Digest());

    /**
     * Derive a key.
     *
     * @param password   The password to derive the key from.
     * @param iterations The iteration count.
     * @return Returns a key derived with the specified parameters.
     * @throws InvalidKeyException If the specified length for the derived key
     *                             is to long.
     */
    public static byte[] deriveKey(final byte[] password, final byte[] salt, final int iterations) throws InvalidKeyException {
        return deriveKey(password, salt, iterations, F.getMacSize());
    }

    /**
     * Derive a key with a specified length.
     *
     * @param password   The password to derive the key from.
     * @param iterations The iteration count.
     * @param len      The length of the derived key.
     * @return Returns a key derived with the specified parameters.
     * @throws InvalidKeyException If the specified length for the derived key
     *                             is to long.
     */
    public static byte[] deriveKey(final byte[] password, final byte[] salt, final int iterations, final int len) throws InvalidKeyException {
        // Check key length
        if (len > ((Math.pow(2, 32) - 1) * F.getMacSize()))
            throw new InvalidKeyException("Derived key to long");

        byte[] derivedKey = new byte[len];

        final int J = 0;
        final int K = F.getMacSize();
        final int U = F.getMacSize() << 1;
        final int B = K + U;
        final byte[] workingArray = new byte[K + U + 4];

        // Initialize F
        CipherParameters macParams = new KeyParameter(password);
        F.init(macParams);

        // Perform iterations
        for (int kpos = 0, blk = 1; kpos < len; kpos += K, blk++) {
            storeInt32BE(blk, workingArray, B);

            F.update(salt, 0, salt.length);

            F.reset();
            F.update(salt, 0, salt.length);
            F.update(workingArray, B, 4);
            F.doFinal(workingArray, U);
            System.arraycopy(workingArray, U, workingArray, J, K);

            for (int i = 1, j = J, k = K; i < iterations; i++) {
                F.init(macParams);
                F.update(workingArray, j, K);
                F.doFinal(workingArray, k);

                for (int u = U, v = k; u < B; u++, v++)
                    workingArray[u] ^= workingArray[v];

                int swp = k;
                k = j;
                j = swp;
            }

            int tocpy = Math.min(len - kpos, K);
            System.arraycopy(workingArray, U, derivedKey, kpos, tocpy);
        }

        Arrays.fill(workingArray, (byte) 0);

        return derivedKey;
    }

    /**
     * Convert a 32-bit integer value into a big-endian byte array
     *
     * @param value  The integer value to convert
     * @param bytes  The byte array to store the converted value
     * @param offSet The offset in the output byte array
     */
    public static void storeInt32BE(int value, byte[] bytes, int offSet) {
        bytes[offSet + 3] = (byte) (value);
        bytes[offSet + 2] = (byte) (value >>> 8);
        bytes[offSet + 1] = (byte) (value >>> 16);
        bytes[offSet] = (byte) (value >>> 24);
    }

}