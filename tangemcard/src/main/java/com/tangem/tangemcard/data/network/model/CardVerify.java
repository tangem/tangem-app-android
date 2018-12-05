package com.tangem.tangemcard.data.network.model;

public class CardVerify {
    private String CID;
    private String publicKey;

    public CardVerify(String CID, String publicKey) {
        this.CID = CID;
        this.publicKey = publicKey;
    }
}