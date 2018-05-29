package com.tangem.domain.wallet;

/**
 * Created by Ilia on 18.12.2017.
 */

public class Keccak256 extends KeccakCore {

    /**
     * Create the engine.
     */
    public Keccak256()
    {
        super("eth-keccak-256");
    }

    public Digest copy()
    {
        return copyState(new Keccak256());
    }

    public int engineGetDigestLength()
    {
        return 32;
    }

    @Override
    protected byte[] engineDigest() {
        return null;
    }

    @Override
    protected void engineUpdate(byte arg0) {
    }

    @Override
    protected void engineUpdate(byte[] arg0, int arg1, int arg2) {
    }
}