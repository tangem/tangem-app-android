package com.tangem.data;

import com.tangem.tangemcard.R;

/**
 * Created by dvol on 06.08.2017.
 */
public enum Blockchain {
    Unknown("", "", 1.0, R.drawable.ic_logo_unknown, ""),
    Bitcoin("BTC", "BTC", 100000000.0, R.drawable.ic_logo_bitcoin, "Bitcoin"),
    BitcoinTestNet("BTC/test", "BTC", 100000000.0, R.drawable.ic_logo_bitcoin_testnet, "Bitcoin Testnet"),
    Ethereum("ETH", "ETH", 1.0, R.drawable.ic_logo_ethereum, "Ethereum"),
    EthereumTestNet("ETH/test", "ETH", 1.0, R.drawable.ic_logo_ethereum_testnet, "Ethereum Testnet"),
    Token("Token", "ETH", 1.0, R.drawable.ic_logo_bat_token, "Ethereum"),
    BitcoinCash("BCH", "BCH", 100000000.0, R.drawable.ic_logo_bitcoin_cash, "Bitcoin Cash"),
    Litecoin("LTC", "LTC", 100000000.0, R.drawable.ic_logo_bitcoin, "Litecoin"),
    Rootstock("RSK", "RBTC", 1.0, R.drawable.ic_logo_bitcoin, "Rootstock"),
    RootstockToken("Token", "RBTC", 1.0, R.drawable.ic_logo_bat_token, "Rootstock");

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
        }
        return R.drawable.tangem2;
    }

}
