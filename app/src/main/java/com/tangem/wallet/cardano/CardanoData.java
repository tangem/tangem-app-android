package com.tangem.wallet.cardano;

import android.os.Bundle;
import android.util.Log;

import com.tangem.wallet.CoinData;
import com.tangem.wallet.CoinEngine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CardanoData extends CoinData {
    public CardanoData() {
        super();
    }

    private Long balance;

    public String getUnspentInputsDescription() {
        try {
//            int gatheredUnspents = 0;
            if( unspentOutputs ==null ) return "";
//            for (int i = 0; i < unspentOutputs.size(); i++) {
//                if (unspentOutputs.get(i).Raw != null && unspentOutputs.get(i).Raw.length() > 1) gatheredUnspents++;
//            }
            return String.valueOf(unspentOutputs.size()) + " unspents";// (" + String.valueOf(gatheredUnspents) + " received)";
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return "";
        }
    }

    public static class UnspentOutput {
        public String txID;
        public Long Amount;
        public Integer Index;

        public Bundle getAsBundle() {
            Bundle B = new Bundle();
            B.putString("txID", txID);
            B.putLong("Amount", Amount);
            B.putInt("Index", Index);
            return B;
        }

        public void loadFromBundle(Bundle B) {
            txID = B.getString("txID");
            Amount = B.getLong("Amount");
            Index = B.getInt("Index");
        }
    }


    private List<UnspentOutput> unspentOutputs = null;

    public List<UnspentOutput> getUnspentOutputs() {
        if (unspentOutputs == null) unspentOutputs = new ArrayList<>();
        return unspentOutputs;
    }

    @Override
    public void loadFromBundle(Bundle B) {
        super.loadFromBundle(B);

        if (B.containsKey("Balance")) balance = B.getLong("Balance");
        else balance = null;
        if (B.containsKey("UnspentTransactions")) {
            unspentOutputs = new ArrayList<>();
            Bundle BB = B.getBundle("UnspentTransactions");
            Integer i = 0;
            while (BB.containsKey(i.toString())) {
                UnspentOutput t = new UnspentOutput();
                t.loadFromBundle(BB.getBundle(i.toString()));
                unspentOutputs.add(t);
                i++;
            }
        }
    }

    @Override
    public void saveToBundle(Bundle B) {
        super.saveToBundle(B);
        try {
            if (unspentOutputs != null) {
                Bundle BB = new Bundle();
                for (Integer i = 0; i < unspentOutputs.size(); i++) {
                    BB.putBundle(i.toString(), unspentOutputs.get(i).getAsBundle());
                }
                B.putBundle("UnspentTransactions", BB);
            }
            if (balance != null) B.putLong("Balance", balance);
        } catch (Exception e) {
            Log.e("Can't save to bundle ", e.getMessage());
        }
    }

    @Override
    public void clearInfo() {
        super.clearInfo();
        balance = null;
        unspentOutputs = null;
    }

    public CoinEngine.InternalAmount getBalanceInInternalUnits() {
        return new CoinEngine.InternalAmount(BigDecimal.valueOf(balance),"Lovelace");
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }

    public Long getBalance() {
        return balance;
    }

    public boolean hasBalanceInfo() {
        return balance != null;
    }
}
