package com.tangem.wallet.binance;

import com.tangem.wallet.CoinData;
import com.tangem.wallet.CoinEngine;

public class BinanceData extends CoinData {
    private String balance;

    public CoinEngine.Amount getBalance() {
        return new CoinEngine.Amount(balance, "BNB");
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public boolean hasBalanceInfo() {
        return balance != null;
    }
}
