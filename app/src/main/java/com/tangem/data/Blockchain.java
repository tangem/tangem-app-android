package com.tangem.data;

import com.tangem.tangemcard.R;

/**
 * Created by dvol on 06.08.2017.
 */
public enum Blockchain {
    Unknown("", "", R.drawable.ic_logo_unknown, ""),
    Bitcoin("BTC", "BTC", R.drawable.ic_logo_bitcoin, "Bitcoin"),
    BitcoinTestNet("BTC/test", "BTC", R.drawable.ic_logo_bitcoin_testnet, "Bitcoin Testnet"),
    Ethereum("ETH", "ETH", R.drawable.ic_logo_ethereum, "Ethereum"),
    EthereumTestNet("ETH/test", "ETH", R.drawable.ic_logo_ethereum_testnet, "Ethereum Testnet"),
    Token("Token", "ERC20", R.drawable.ic_logo_bat_token, "Ethereum"),
    BitcoinCash("BCH", "BCH", R.drawable.ic_logo_bitcoin_cash, "Bitcoin Cash"),
    Litecoin("LTC", "LTC", R.drawable.ic_logo_bitcoin, "Litecoin"),
    Stellar("XLM", "XLM", R.drawable.ic_logo_stellar, "Stellar Lumens"),
    StellarTestNet("XLM/test", "XLM", R.drawable.ic_logo_bitcoin, "Stellar Lumens");
    Blockchain(String ID, String currency, int imageResource, String officialName) {
        mID = ID;
        mCurrency = currency;
        mImageResource = imageResource;
        mOfficialName = officialName;
    }

    private String mID, mOfficialName;
    private String mCurrency;
    private int mImageResource;

    public String getID() {
        return mID;
    }

    public String getOfficialName() {
        return mOfficialName;
    }

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

    //TODO - ???
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
