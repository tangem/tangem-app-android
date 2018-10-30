package com.tangem.domain.wallet;

import android.util.Log;

/**
 * Created by Ilia on 15.02.2018.
 */

public class CoinEngineFactory {
    public static CoinEngine create(Blockchain blockchain) {
        switch (blockchain) {
            case Bitcoin:
            case BitcoinTestNet:
                return new BtcEngine();
            case BitcoinCash:
            case BitcoinCashTestNet:
                return new BtcCashEngine();
            case Ethereum:
            case EthereumTestNet:
                return new EthEngine();
            case Token:
                return new TokenEngine();
            default:
                return null;
        }
    }

    public static CoinEngine create(TangemContext context) {
        CoinEngine result;
        try {
            if (Blockchain.BitcoinCash == context.getBlockchain() || Blockchain.BitcoinCashTestNet == context.getBlockchain()) {
                result = new BtcCashEngine(context);
            } else if (Blockchain.Bitcoin == context.getBlockchain() || Blockchain.BitcoinTestNet == context.getBlockchain()) {
                result = new BtcEngine(context);
            } else if (Blockchain.Ethereum == context.getBlockchain() || Blockchain.EthereumTestNet == context.getBlockchain()) {
                result = new EthEngine(context);
            } else if (Blockchain.Token == context.getBlockchain()) {
                result = new TokenEngine(context);
            } else {
                return null;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.e("CoinEngineFactory","Can't create CoinEngine!");
            result=null;
        }
        return result;
    }
}
