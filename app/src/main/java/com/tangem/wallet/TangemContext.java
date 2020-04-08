package com.tangem.wallet;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.tangem.Constant;
import com.tangem.data.Blockchain;
import com.tangem.tangem_card.data.TangemCard;
import com.tangem.tangem_sdk.data.TangemCardExtensionsKt;
import com.tangem.util.AnalyticsParam;

public class TangemContext {
    //    public static final String EXTRA_BLOCKCHAIN_DATA = "BLOCKCHAIN_DATA";
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
        Blockchain blockchain = Blockchain.fromId(card.getBlockchainID());
        if ((blockchain == Blockchain.Ethereum || blockchain == Blockchain.EthereumTestNet) && card.isToken()) {
            if (card.getTokenSymbol().startsWith("NFT:")) {
                return Blockchain.NftToken;
            } else {
                return Blockchain.Token;
            }
        }
        if (blockchain == Blockchain.Ethereum && card.isIDCard()) {
            return Blockchain.EthereumId;
        }
        if ((blockchain == Blockchain.Rootstock) && card.isToken()) {
            return Blockchain.RootstockToken;
        }
        if (blockchain == Blockchain.Stellar && card.isToken()) {
            return Blockchain.StellarAsset;
        }
        return blockchain;
    }

//    public void setBlockchain(Blockchain blockchain) {
//        if (card == null) return;
//        card.setBlockchainID(blockchain.getID());
//    }

//    private String blockchainName = "";

    public String getBlockchainName() {
        Blockchain blockchain = getBlockchain();
        if (blockchain == Blockchain.Token || blockchain == Blockchain.RootstockToken) {
            return card.getTokenSymbol() + "<br><small><small> " + getBlockchain().getOfficialName() + " smart contract token</small></small>";
        }
        if (blockchain == Blockchain.NftToken) {
            return card.getTokenSymbol().substring(4) + "<br><small><small> " + getBlockchain().getOfficialName() + " non-fungible token</small></small>";
        }
        if (blockchain == Blockchain.StellarAsset) {
            return card.getTokenSymbol() + "<br><small><small> " + getBlockchain().getOfficialName() + " asset</small></small>";
        }
        if (blockchain == Blockchain.StellarTag) {
            return "TANGEM TAG<br><small><small> " + getBlockchain().getOfficialName() + " non-fungible token </small></small>";
        }
        return blockchain.getOfficialName();
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
        if (error != null) {
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.setCustomKey(AnalyticsParam.BLOCKCHAIN.getParam(), getBlockchainName());
            crashlytics.recordException(new Exception(error));
        }
        this.error = error;
    }

    public void setError(int valueId) {
        this.error = getString(valueId);
    }

    public String getError() {
        return error;
    }

    public boolean hasError() {
        return error != null && !error.isEmpty();
    }

    public void setMessage(String value) {
        this.message = value;
    }

    public void setMessage(int valueId) {
        this.message = getString(valueId);
    }

    public String getMessage() {
        return message;
    }


    public static TangemContext loadFromBundle(Context context, Bundle bundle) {
        TangemContext tangemContext = new TangemContext();
        tangemContext.setContext(context);

        if (bundle.containsKey(TangemCardExtensionsKt.EXTRA_TANGEM_CARD_UID)) {
            tangemContext.card = new TangemCard(bundle.getString(TangemCardExtensionsKt.EXTRA_TANGEM_CARD_UID));
            TangemCardExtensionsKt.loadFromBundle(tangemContext.card, bundle.getBundle(TangemCardExtensionsKt.EXTRA_TANGEM_CARD));
        }

        if (tangemContext.getBlockchain() != null) {
            if (bundle.containsKey(Constant.EXTRA_BLOCKCHAIN_DATA)) {
                tangemContext.coinData = CoinData.fromBundle(tangemContext.getBlockchain(), bundle.getBundle(Constant.EXTRA_BLOCKCHAIN_DATA));
            } else {
                tangemContext.coinData = CoinEngineFactory.INSTANCE.create(tangemContext).createCoinData();
            }
        }
        tangemContext.error = bundle.getString("Error");
        tangemContext.message = bundle.getString("Message");

        return tangemContext;
    }

    public void saveToBundle(Bundle intent) {

        if (card != null) {
            intent.putString(TangemCardExtensionsKt.EXTRA_TANGEM_CARD_UID, card.getUID());
            intent.putBundle(TangemCardExtensionsKt.EXTRA_TANGEM_CARD, TangemCardExtensionsKt.getAsBundle(card));
        }

        if (coinData != null) {
            intent.putBundle(Constant.EXTRA_BLOCKCHAIN_DATA, coinData.asBundle());
        }

        intent.putString("Error", error);
        intent.putString("Message", message);
    }

    public void saveToIntent(Intent intent) {

        if (card != null) {
            intent.putExtra(TangemCardExtensionsKt.EXTRA_TANGEM_CARD_UID, card.getUID());
            intent.putExtra(TangemCardExtensionsKt.EXTRA_TANGEM_CARD, TangemCardExtensionsKt.getAsBundle(card));
        }

        if (coinData != null) {
            intent.putExtra(Constant.EXTRA_BLOCKCHAIN_DATA, coinData.asBundle());
        }

        intent.putExtra("Error", error);
        intent.putExtra("Message", message);
    }

    public String getString(int stringId) {
        if (context != null) return getContext().getResources().getString(stringId);
        return "context.resources.string[" + stringId + "]";
    }

    public void setDenomination(byte[] denomination) {
        try {
            CoinEngine engine = CoinEngineFactory.INSTANCE.create(getBlockchain());
            CoinEngine.InternalAmount internalAmount = engine.convertToInternalAmount(denomination);
            CoinEngine.Amount amount = engine.convertToAmount(internalAmount);
            card.setDenomination(denomination, amount.toString());
        } catch (Exception e) {
            e.printStackTrace();
            card.setDenomination(denomination, "N/A");
        }
    }

}
