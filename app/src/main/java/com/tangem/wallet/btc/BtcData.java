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

    protected Long balanceConfirmed, balanceUnconfirmed;

    private boolean useBlockcypher = false;

    //for blockchain.info
    private boolean hasUnconfirmed = false;

    public Unspents getUnspentInputsDescription() {
        try {
            int gatheredUnspents = 0;
            if (unspentTransactions == null) return null;
            for (int i = 0; i < unspentTransactions.size(); i++) {
                if (unspentTransactions.get(i).script != null && unspentTransactions.get(i).script.length() > 1)
                    gatheredUnspents++;
            }

            return new Unspents(unspentTransactions.size(), gatheredUnspents);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class UnspentTransaction {
        public String txID;
        public Long amount;
        public Integer outputN;
        public String script = "";

        public Bundle getAsBundle() {
            Bundle B = new Bundle();
            B.putString("txID", txID);
            B.putLong("Amount", amount);
            B.putInt("OutputN", outputN);
            B.putString("Script", script);
            return B;
        }

        public void loadFromBundle(Bundle B) {
            txID = B.getString("txID");
            amount = B.getLong("Amount");
            outputN = B.getInt("OutputN");
            script = B.getString("Script");
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
        if (B.containsKey("UseBlockcypher")) useBlockcypher = B.getBoolean("UseBlockcypher");
        if (B.containsKey("HasUnconfirmed")) useBlockcypher = B.getBoolean("HasUnconfirmed");
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
            if (useBlockcypher) B.putBoolean("UseBlockcypher", true);
            if (hasUnconfirmed) B.putBoolean("HasUnconfirmed", true);
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
        hasUnconfirmed = false;
    }

    public CoinEngine.InternalAmount getBalanceInInternalUnits() {
        if (balanceConfirmed != null && balanceUnconfirmed != null) {
            return new CoinEngine.InternalAmount(BigDecimal.valueOf(balanceConfirmed).add(BigDecimal.valueOf(balanceUnconfirmed)), "Satoshi");
        } else {
            return null;
        }
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

    public boolean isUseBlockcypher() {
        return useBlockcypher;
    }

    public void setUseBlockcypher(boolean useBlockcypher) {
        this.useBlockcypher = useBlockcypher;
    }

    public boolean isHasUnconfirmed() {
        return hasUnconfirmed;
    }

    public void setHasUnconfirmed(boolean hasUnconfirmed) {
        this.hasUnconfirmed = hasUnconfirmed;
    }
}
