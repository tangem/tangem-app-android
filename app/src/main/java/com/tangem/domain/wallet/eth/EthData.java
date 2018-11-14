package com.tangem.domain.wallet.eth;

import android.os.Bundle;
import android.util.Log;

import com.tangem.domain.wallet.CoinData;
import com.tangem.domain.wallet.CoinEngine;

import java.math.BigInteger;

public class EthData extends CoinData {
    private CoinEngine.InternalAmount balance = null;

    private BigInteger countConfirmedTX = null;
    private BigInteger countUnconfirmedTX = BigInteger.valueOf(0);

    public BigInteger getConfirmedTXCount() {
        if (countConfirmedTX == null) {
            countConfirmedTX = BigInteger.valueOf(0);
        }
        return countConfirmedTX;
    }

    public void setConfirmedTXCount(BigInteger count) {
        countConfirmedTX = count;
    }

    public BigInteger getUnconfirmedTXCount() {
        if (countUnconfirmedTX == null) {
            countUnconfirmedTX = BigInteger.valueOf(0);
        }
        return countUnconfirmedTX;
    }

    public void setUnconfirmedTXCount(BigInteger count) {
        countUnconfirmedTX = count;
    }
//
//    public String getAmountInGwei(String amount) {
//        BigInteger d = new BigInteger(amount, 10);
//        BigInteger m = d.divide(BigInteger.valueOf(1000000000L));
//        return m.toString(10);
//    }

    @Override
    public void clearInfo() {
        super.clearInfo();
        balance = null;
    }

    public CoinEngine.InternalAmount getBalanceInInternalUnits() {
        return balance;

    }

    public void setBalanceInInternalUnits(CoinEngine.InternalAmount value) {
        balance = value;
    }


    @Override
    public void loadFromBundle(Bundle B) {
        super.loadFromBundle(B);

        String currency = B.getString("BalanceCurrency");
        balance = new CoinEngine.InternalAmount(B.getString("BalanceDecimal"), currency);

        if (B.containsKey("confirmTx"))
            countConfirmedTX = new BigInteger(B.getString("confirmTx"), 16);
        if (B.containsKey("unconfirmTx"))
            countUnconfirmedTX = new BigInteger(B.getString("unconfirmTx"), 16);
    }

    @Override
    public void saveToBundle(Bundle B) {
        super.saveToBundle(B);
        try {
            B.putString("BalanceCurrency", balance.getCurrency());
            B.putString("BalanceDecimal", balance.toString());

            B.putString("confirmTx", getConfirmedTXCount().toString(16));
            B.putString("unconfirmTx", getUnconfirmedTXCount().toString(16));

        } catch (Exception e) {
            Log.e("Can't save to bundle ", e.getMessage());
        }

    }

}