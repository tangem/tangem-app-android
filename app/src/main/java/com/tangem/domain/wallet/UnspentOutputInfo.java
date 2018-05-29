package com.tangem.domain.wallet;

/**
 * Created by Ilia on 29.09.2017.
 */

@SuppressWarnings("WeakerAccess")
public class UnspentOutputInfo {
    public final byte[] txHash;
    public final Transaction.Script script;
    public final long value;
    public final int outputIndex;
    public final long confirmations;
    public String txHashForBuild;
    public byte[] scriptForBuild;
    public byte[] bodyDoubleHash;
    public byte[] bodyHash;

    public UnspentOutputInfo(byte[] txHash, Transaction.Script script, long value, int outputIndex, long confirmations, String hashForBuild, byte[] sign) {
        this.txHash = txHash;
        this.script = script;
        this.value = value;
        this.outputIndex = outputIndex;
        this.confirmations = confirmations;
        this.txHashForBuild = hashForBuild;
        this.scriptForBuild = sign;
    }
}