package com.tangem.domain.wallet;

import android.content.Context;
import android.os.Bundle;


public class TangemContext {
    private static final String EXTRA_BLOCKCHAIN_DATA = "BLOCKCHAIN_DATA";
    private Context context;
    private TangemCard card;
    private CoinData coinData;
    private String error;
    private String message;


    public TangemContext() {

    }

    public TangemContext(TangemCard card) {
        this.card = card;
    }

    public Blockchain getBlockchain() {
        if (card == null) return Blockchain.Unknown;
        return card.getBlockchain();
    }

    public void setBlockchain(Blockchain blockchain) {
        if (card == null) return;
        card.setBlockchain(blockchain);
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public TangemCard getCard() {
        return card;
    }

    public void setCard(TangemCard card) {
        this.card = card;
    }

    public CoinData getCoinData() {
        return coinData;
    }

    public void setCoinData(CoinData coinData) {
        this.coinData = coinData;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public void setMessage(String value) {
        this.message = value;
    }

    public String getMessage() {
        return message;
    }


    public static TangemContext loadFromBundle(Context context, Bundle bundle) {
        TangemContext tangemContext = new TangemContext();
        tangemContext.setContext(context);

        if (bundle.containsKey(TangemCard.EXTRA_UID)) {
            tangemContext.card = new TangemCard(bundle.getString(TangemCard.EXTRA_UID));
            tangemContext.card.loadFromBundle(bundle.getBundle(TangemCard.EXTRA_CARD));
        }

        if (tangemContext.getBlockchain() != null && bundle.containsKey(EXTRA_BLOCKCHAIN_DATA)) {
            tangemContext.coinData=CoinData.fromBundle(tangemContext.getBlockchain(), bundle.getBundle(EXTRA_BLOCKCHAIN_DATA));
        }

        tangemContext.error = bundle.getString("Error");
        tangemContext.message = bundle.getString("Message");

        return tangemContext;
    }

    public void saveToBundle(Bundle bundle) {

        if (card != null) {
            bundle.putString(TangemCard.EXTRA_UID, card.getUID());
            bundle.putBundle(TangemCard.EXTRA_CARD, card.getAsBundle());
        }

        if (coinData != null) {
            bundle.putBundle(EXTRA_BLOCKCHAIN_DATA, coinData.asBundle());
        }

        bundle.putString("Error", error);
        bundle.putString("Message", message);

    }

    public String getString(int stringId) {
        if( context!=null ) return getContext().getResources().getString(stringId);
        return "context.resources.string["+stringId+"]";
    }
}
