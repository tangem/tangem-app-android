package com.tangem.wallet.EOS;

import android.os.Bundle;
import android.util.Log;

import com.tangem.wallet.CoinData;
import com.tangem.wallet.CoinEngine;

import java.math.BigInteger;

import io.jafka.jeos.core.request.chain.transaction.PushTransactionRequest;

public class EosData extends CoinData {
    private CoinEngine.Amount balance = null;

    @Override
    public void clearInfo() {
        super.clearInfo();
        balance = null;
    }

    public CoinEngine.Amount getBalance() {
        return balance;
    }

    public void setBalance(CoinEngine.Amount value) {
        balance = value;
    }

    @Override
    public void loadFromBundle(Bundle B) {
        super.loadFromBundle(B);

        if (B.containsKey("BalanceCurrency") && B.containsKey("BalanceDecimal")) {
            String currency = B.getString("BalanceCurrency");
            balance = new CoinEngine.Amount(B.getString("BalanceDecimal"), currency);
        } else {
            balance = null;
        }
    }

    @Override
    public void saveToBundle(Bundle B) {
        super.saveToBundle(B);
        try {
            if (balance != null) {
                B.putString("BalanceCurrency", balance.getCurrency());
                B.putString("BalanceDecimal", balance.toString());
            }

        } catch (Exception e) {
            Log.e("Can't save to bundle ", e.getMessage());
        }

    }

}