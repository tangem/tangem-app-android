package com.tangem.domain.wallet;

import android.os.Bundle;
import android.util.Log;

import java.math.BigDecimal;
import java.math.BigInteger;

public class EthData extends CoinData
{
    private CoinEngine.InternalAmount balance=null;
    private CoinEngine.InternalAmount balanceAlter=null;

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

    public String getAmountInGwei(String amount) {
        BigInteger d = new BigInteger(amount, 10);
        BigInteger m = d.divide(BigInteger.valueOf(1000000000L));
        return m.toString(10);
    }

    @Override
    public void clearInfo() {
        super.clearInfo();
        balance = null;
        balanceAlter = null;
    }

    public CoinEngine.InternalAmount getBalanceInInternalUnits() {
        return balance;

    }
    public CoinEngine.InternalAmount getBalanceAlterInInternalUnits() {
        return balanceAlter;

    }

    public void setBalanceInInternalUnits(CoinEngine.InternalAmount value) {
        balance = value;
    }

    public void setBalanceAlterInInternalUnits(CoinEngine.InternalAmount value) {
        balanceAlter = value;
    }

    public Long getBalanceETH() {

        BigDecimal b = null;
            if (balance != null) {
                b = balance; // Returns ETH / token balance
            } else if (balanceAlter != null) {
                b = balanceAlter; // or ETH balance if there're no tokens on Token card
            }

            if (b != null) {
                return b.longValue(); // Will leave only lower 64 bits for ETH and Tokens
            } else {
                return null;
            }
    }

    @Override
    public void loadFromBundle(Bundle B) {
        super.loadFromBundle(B);

        balance = new CoinEngine.InternalAmount(B.getString("BalanceDecimal"));
        balanceAlter =new CoinEngine.InternalAmount(B.getString("BalanceDecimalAlter"));

        if (B.containsKey("confirmTx"))
            countConfirmedTX = new BigInteger(B.getString("confirmTx"), 16);
        if (B.containsKey("unconfirmTx"))
            countUnconfirmedTX = new BigInteger(B.getString("unconfirmTx"), 16);
    }

    @Override
    public void saveToBundle(Bundle B) {
        try {
            B.putString("BalanceDecimal", balance.toString());
            B.putString("BalanceDecimalAlter", balance.toString());

            B.putString("confirmTx", getConfirmedTXCount().toString(16));
            B.putString("unconfirmTx", getUnconfirmedTXCount().toString(16));

        } catch (Exception e) {
            Log.e("Can't save to bundle ", e.getMessage());
        }

    }


}
