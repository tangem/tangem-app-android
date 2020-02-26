package com.tangem.data;

import com.tangem.tangem_sdk.R;

/**
 * Created by dvol on 06.08.2017.
 */
public enum Blockchain {
    Unknown("", "", 1.0, R.drawable.ic_logo_unknown, ""),
    Bitcoin("BTC", "BTC", 100000000.0, R.drawable.ic_logo_bitcoin, "Bitcoin"),
    BitcoinTestNet("BTC/test", "BTC", 100000000.0, R.drawable.ic_logo_bitcoin_testnet, "Bitcoin Testnet"),
    BitcoinDual("BTC/dual", "BTC", 100000000.0, R.drawable.ic_logo_bitcoin, "Bitcoin"),
    Ethereum("ETH", "ETH", 1.0, R.drawable.ic_logo_ethereum, "Ethereum"),
    EthereumId("ETH/ID", "ETH", 1.0, R.drawable.ic_logo_ethereum, "Ethereum ID"),
    EthereumTestNet("ETH/test", "ETH", 1.0, R.drawable.ic_logo_ethereum_testnet, "Ethereum Testnet"),
    Token("Token", "ETH", 1.0, R.drawable.ic_logo_bat_token, "Ethereum"),
    NftToken("NftToken", "", 1.0, R.drawable.tangem2, "Ethereum"),
    BitcoinCash("BCH", "BCH", 100000000.0, R.drawable.ic_logo_bitcoin_cash, "Bitcoin Cash"),
    Litecoin("LTC", "LTC", 100000000.0, R.drawable.tangem2, "Litecoin"),
    Rootstock("RSK", "RBTC", 1.0, R.drawable.tangem2, "RSK"),
    RootstockToken("RskToken", "RBTC", 1.0, R.drawable.tangem2, "RSK"),
    Cardano("CARDANO", "ADA", 1000000.0, R.drawable.tangem2, "Cardano"),
    Ripple ("XRP", "XRP", 1000000.0, R.drawable.tangem2, "XRP"),
    Binance("BINANCE", "BNB", 100000000.0, R.drawable.tangem2, "Binance"),
    BinanceTestNet("BINANCE/test", "BNB", 100000000.0, R.drawable.tangem2, "Binance Testnet"),
    Matic("MATIC", "MTX", 1.0, R.drawable.tangem2, "Matic"),
    MaticTestNet("MATIC/test", "MTX", 1.0, R.drawable.tangem2, "Matic Testnet"),
    Stellar("XLM", "XLM", 10000000.0, R.drawable.ic_logo_stellar, "Stellar"),
    StellarTestNet("XLM/test", "XLM", 10000000.0, R.drawable.ic_logo_stellar, "Stellar Testnet"),
    StellarAsset("Asset", "XLM", 10000000.0, R.drawable.ic_logo_stellar, "Stellar"),
    StellarTag("XLM-Tag", "XLM", 1000000.0, R.drawable.ic_logo_stellar, "Stellar"),
    Eos("EOS", "EOS", 10000.0, R.drawable.tangem2, "EOS"),
    Ducatus("DUC", "DUC", 100000000.0, R.drawable.tangem2, "Ducatus"),
    Tezos("TEZOS", "XTZ", 10000000.0, R.drawable.tangem2, "Tezos");

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
        return null;
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

    public int getImageResource(android.content.Context context, String name) {
        if (name==null || name.isEmpty())
            return getImageResource();

        name = name.toLowerCase();

        int resourceId = context.getResources().getIdentifier(name + "_token", "drawable", context.getPackageName());

        if (resourceId <= 0)
            return R.drawable.ic_logo_ethereum;
        return resourceId;
    }

    public static int getLogoImageResource(String blockchainID, String symbolName) {
        switch (blockchainID) {
            case "BTC":
                return R.drawable.ic_logo_bitcoin;

            case "Token":
                if (symbolName.equals("SEED"))
                    return R.drawable.ic_logo_seed;
                else
                    return R.drawable.ic_logo_ethereum;

            case "ETH":
                return R.drawable.ic_logo_ethereum;

            case "XLM":
                return R.drawable.ic_logo_stellar;
        }
        return R.drawable.tangem2;
    }

}
