package com.tangem.data;

import com.tangem.tangem_sdk.R;

/**
 * Created by dvol on 06.08.2017.
 */
public enum Blockchain {
    Unknown("", "", 1.0, R.drawable.ic_logo_unknown, "Unknown"),
    Bitcoin("BTC", "BTC", 100000000.0, R.drawable.ic_logo_bitcoin, "Bitcoin"),
    BitcoinTestNet("BTC/test", "BTC", 100000000.0, R.drawable.ic_logo_bitcoin_testnet, "Bitcoin Testnet"),
    BitcoinDual("BTC/dual", "BTC", 100000000.0, R.drawable.ic_logo_bitcoin, "Bitcoin"),
    Ethereum("ETH", "ETH", 1.0, R.drawable.ic_logo_ethereum, "Ethereum"),
    EthereumId("ETH/ID", "ETH", 1.0, R.drawable.ic_logo_ethereum, "Ethereum"),
    EthereumTestNet("ETH/test", "ETH", 1.0, R.drawable.ic_logo_ethereum_testnet, "Ethereum Testnet"),
    Token("Token", "ETH", 1.0, R.drawable.ic_logo_ethereum, "Ethereum"),
    NftToken("NftToken", "", 1.0, R.drawable.ic_logo_ethereum, "Ethereum"),
    BitcoinCash("BCH", "BCH", 100000000.0, R.drawable.ic_logo_bitcoin_cash, "Bitcoin Cash"),
    Litecoin("LTC", "LTC", 100000000.0, R.drawable.ic_logo_litecoin, "Litecoin"),
    Rootstock("RSK", "RBTC", 1.0, R.drawable.tangem2, "RSK"),
    RootstockToken("RskToken", "RBTC", 1.0, R.drawable.tangem2, "RSK"),
    Cardano("CARDANO", "ADA", 1000000.0, R.drawable.tangem2, "Cardano"),
    Ripple("XRP", "XRP", 1000000.0, R.drawable.ic_logo_xrp, "XRP"),
    Binance("BINANCE", "BNB", 100000000.0, R.drawable.ic_logo_binance, "Binance"),
    BinanceTestNet("BINANCE/test", "BNB", 100000000.0, R.drawable.ic_logo_binance, "Binance Testnet"),
    Matic("MATIC", "MTX", 1.0, R.drawable.tangem2, "Matic"),
    MaticTestNet("MATIC/test", "MTX", 1.0, R.drawable.tangem2, "Matic Testnet"),
    Stellar("XLM", "XLM", 10000000.0, R.drawable.ic_logo_stellar, "Stellar"),
    StellarTestNet("XLM/test", "XLM", 10000000.0, R.drawable.ic_logo_stellar, "Stellar Testnet"),
    StellarAsset("Asset", "XLM", 10000000.0, R.drawable.ic_logo_stellar, "Stellar"),
    StellarTag("XLM-Tag", "XLM", 1000000.0, R.drawable.ic_logo_stellar, "Stellar"),
    Eos("EOS", "EOS", 10000.0, R.drawable.tangem2, "EOS"),
    Ducatus("DUC", "DUC", 100000000.0, R.drawable.tangem2, "Ducatus"),
    Tezos("TEZOS", "XTZ", 10000000.0, R.drawable.ic_logo_tezos, "Tezos"),
    FlowDemo("FLOW/demo", "", 1.0, R.drawable.tangem2, "Flow demo");

    Blockchain(String ID, String currency, double multiplier, int imageResource, String officialName) {
        mID = ID;
        mCurrency = currency;
//        mMultiplier = multiplier;
        mImageResource = imageResource;
        mOfficialName = officialName;
    }

    private String mID, mOfficialName;
    //private double mMultiplier;
    private String mCurrency;
    private int mImageResource;

    public String getID() {
        return mID;
    }

    public String getOfficialName() {
        return mOfficialName;
    }

//    public double getMultiplier() {
//        return mMultiplier;
//    }

    public String getCurrency() {
        return mCurrency;
    }

    public static Blockchain fromId(String id) {
        for (Blockchain blockchain : values()) {
            if (blockchain.getID().equals(id)) return blockchain;
        }
        return Blockchain.Unknown;
    }

    public static Blockchain fromCurrency(String currency) {
        for (Blockchain blockchain : values()) {
            if (blockchain.getCurrency() == currency) return blockchain;
        }
        return null;
    }

    public static String[] getCurrencies() {
        String[] result = new String[values().length - 1];
        for (int i = 0; i < result.length - 1; i++) {
            result[i] = values()[i + 1].getCurrency();
        }
        return result;
    }

    private int getImageResource() {
        return mImageResource;
    }

    public int getLogoImageResource(String symbolName) {
        switch (this) {
            case Token:
                if (symbolName.equals("SEED"))
                    return R.drawable.ic_logo_seed;
                break;
            case RootstockToken:
                if (symbolName.equals("RIF"))
                    return R.drawable.ic_logo_rif;
        }

        return getImageResource();
    }

}
