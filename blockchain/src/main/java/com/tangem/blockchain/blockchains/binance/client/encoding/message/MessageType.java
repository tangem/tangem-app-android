package com.tangem.blockchain.blockchains.binance.client.encoding.message;

import com.tangem.blockchain.blockchains.binance.client.encoding.EncodeUtils;

/**
 * Binance dex standard transactiont types.
 */
public enum MessageType {
    Send("2A2C87FA"),
    NewOrder("CE6DC043"),
    CancelOrder("166E681B"),
    TokenFreeze("E774B32D"),
    TokenUnfreeze("6515FF0D"),
    StdSignature(null),
    PubKey("EB5AE987"),
    StdTx("F0625DEE");

    private byte[] typePrefixBytes;

    MessageType(String typePrefix) {
        if (typePrefix == null) {
            this.typePrefixBytes = new byte[0];
        } else
            this.typePrefixBytes = EncodeUtils.hexStringToByteArray(typePrefix);
    }

    public byte[] getTypePrefixBytes() {
        return typePrefixBytes;
    }

}
