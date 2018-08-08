package com.tangem.domain.wallet;

/**
 * Created by Ilia on 15.02.2018.
 */

public class CoinEngineFactory {
    public static CoinEngine create(Blockchain chain) {
        if (Blockchain.BitcoinCash == chain || Blockchain.BitcoinCashTestNet == chain) {
            return new BtcCashEngine();
        } else if (Blockchain.Bitcoin == chain || Blockchain.BitcoinTestNet == chain) {
            return new BtcEngine(); //TODO: ВРЕМЕНГГО!!!!
        } else if (Blockchain.Ethereum == chain || Blockchain.EthereumTestNet == chain) {
            return new EthEngine();
        } else if (Blockchain.Token == chain) {
            return new TokenEngine();
        } else {
            return null;
        }
    }
}
