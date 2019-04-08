package com.tangem.wallet.btc;

import android.os.Bundle;
import android.util.Log;

import com.tangem.wallet.CoinData;
import com.tangem.wallet.CoinEngine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BtcData extends CoinData {
    public BtcData() {
        super();
    }

    private Long balanceConfirmed, balanceUnconfirmed;

    public String getUnspentInputsDescription() {
        try {
            int gatheredUnspents = 0;
            if( unspentTransactions==null ) return "";
            for (int i = 0; i < unspentTransactions.size(); i++) {
                if (unspentTransactions.get(i).Raw != null && unspentTransactions.get(i).Raw.length() > 1) gatheredUnspents++;
            }
            return String.valueOf(unspentTransactions.size()) + " unspents (" + String.valueOf(gatheredUnspents) + " received)";
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return "";
        }
    }

    public static class UnspentTransaction {
        public String txID;
        public Long Amount;
        public Integer Height;
        public String Raw = "";

        public Bundle getAsBundle() {
            Bundle B = new Bundle();
            B.putString("txID", txID);
            B.putLong("Amount", Amount);
            B.putInt("Height", Height);
            B.putString("Raw", Raw);
            return B;
        }

        public void loadFromBundle(Bundle B) {
            txID = B.getString("txID");
            Amount = B.getLong("Amount");
            Height = B.getInt("Height");
            Raw = B.getString("Raw");
        }
    }

    private List<UnspentTransaction> unspentTransactions = null;

    public List<UnspentTransaction> getUnspentTransactions() {
        if (unspentTransactions == null) unspentTransactions = new ArrayList<>();
        return unspentTransactions;
    }

    @Override
    public void loadFromBundle(Bundle B) {
        super.loadFromBundle(B);

        if (B.containsKey("BalanceConfirmed")) balanceConfirmed = B.getLong("BalanceConfirmed");
        else balanceConfirmed = null;
        if (B.containsKey("BalanceUnconfirmed"))
            balanceUnconfirmed = B.getLong("BalanceUnconfirmed");
        else balanceUnconfirmed = null;
        if (B.containsKey("UnspentTransactions")) {
            unspentTransactions = new ArrayList<>();
            Bundle BB = B.getBundle("UnspentTransactions");
            Integer i = 0;
            while (BB.containsKey(i.toString())) {
                UnspentTransaction t = new UnspentTransaction();
                t.loadFromBundle(BB.getBundle(i.toString()));
                unspentTransactions.add(t);
                i++;
            }
        }
    }

    @Override
    public void saveToBundle(Bundle B) {
        super.saveToBundle(B);
        try {
            if (unspentTransactions != null) {
                Bundle BB = new Bundle();
                for (Integer i = 0; i < unspentTransactions.size(); i++) {
                    BB.putBundle(i.toString(), unspentTransactions.get(i).getAsBundle());
                }
                B.putBundle("UnspentTransactions", BB);
            }
            if (balanceConfirmed != null) B.putLong("BalanceConfirmed", balanceConfirmed);
            if (balanceUnconfirmed != null) B.putLong("BalanceUnconfirmed", balanceUnconfirmed);
        } catch (Exception e) {
            Log.e("Can't save to bundle ", e.getMessage());
        }
    }

    @Override
    public void clearInfo() {
        super.clearInfo();
        balanceConfirmed = null;
        balanceUnconfirmed = null;
        unspentTransactions = null;
    }

    public CoinEngine.InternalAmount getBalanceInInternalUnits() {
        return new CoinEngine.InternalAmount(BigDecimal.valueOf(balanceConfirmed).add(BigDecimal.valueOf(balanceUnconfirmed)),"Satoshi");
    }

    public Long getBalanceUnconfirmed() {
        return balanceUnconfirmed;
    }

    public void setBalanceConfirmed(Long balance) {
        this.balanceConfirmed = balance;
    }

    public void setBalanceUnconfirmed(Long balance) {
        this.balanceUnconfirmed = balance;
    }

    public boolean hasBalanceInfo() {
        return balanceConfirmed != null || balanceUnconfirmed != null;
    }

}
