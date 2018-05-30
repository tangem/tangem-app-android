package com.tangem.domain.wallet;

import java.math.BigInteger;

/**
 * Created by Ilia on 07.01.2018.
 */

public class ECDSASignatureETH {
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
    public ECDSASignatureETH(BigInteger r, BigInteger s) {
        this.r = r;
        this.s = s;
    }

    /**
     *t
     * @param r
     * @param s
     * @return -
     */
    private static ECDSASignatureETH fromComponents(byte[] r, byte[] s) {
        return new ECDSASignatureETH(new BigInteger(1, r), new BigInteger(1, s));
    }

    /**
     *
     * @param r -
     * @param s -
     * @param v -
     * @return -
     */
    public static ECDSASignatureETH fromComponents(byte[] r, byte[] s, byte v) {
        ECDSASignatureETH signature = fromComponents(r, s);
        signature.v = v;
        return signature;
    }


}

