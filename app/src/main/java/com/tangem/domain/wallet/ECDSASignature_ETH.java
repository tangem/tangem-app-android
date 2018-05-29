package com.tangem.domain.wallet;

import java.math.BigInteger;

/**
 * Created by Ilia on 07.01.2018.
 */

public class ECDSASignature_ETH {
    /**
     * The two components of the signature.
     */
    public final BigInteger r, s;
    public byte v;

    /**
     * Constructs a signature with the given components. Does NOT automatically canonicalise the signature.
     *
     * @param r -
     * @param s -
     */
    public ECDSASignature_ETH(BigInteger r, BigInteger s) {
        this.r = r;
        this.s = s;
    }

    /**
     *t
     * @param r
     * @param s
     * @return -
     */
    private static ECDSASignature_ETH fromComponents(byte[] r, byte[] s) {
        return new ECDSASignature_ETH(new BigInteger(1, r), new BigInteger(1, s));
    }

    /**
     *
     * @param r -
     * @param s -
     * @param v -
     * @return -
     */
    public static ECDSASignature_ETH fromComponents(byte[] r, byte[] s, byte v) {
        ECDSASignature_ETH signature = fromComponents(r, s);
        signature.v = v;
        return signature;
    }


}

