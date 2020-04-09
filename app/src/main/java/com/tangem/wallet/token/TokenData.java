package com.tangem.wallet.token;

import android.os.Bundle;
import android.util.Log;

import com.tangem.wallet.CoinEngine;
import com.tangem.wallet.eth.EthData;

public class TokenData extends EthData {
    private CoinEngine.InternalAmount balanceAlter = null;
    private Integer sequence = null;

    @Override
    public void clearInfo() {
        super.clearInfo();
        balanceAlter = null;
        sequence = null;
    }

    public CoinEngine.InternalAmount getBalanceAlterInInternalUnits() {
        return balanceAlter;

    }

    public void setBalanceAlterInInternalUnits(CoinEngine.InternalAmount value) {
        balanceAlter = value;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    @Override
    public void loadFromBundle(Bundle B) {
        super.loadFromBundle(B);

        if (B.containsKey("BalanceDecimalAlter")) {
            balanceAlter = new CoinEngine.InternalAmount(B.getString("BalanceDecimalAlter"), "wei");
        } else {
            balanceAlter = null;
        }
        if (B.containsKey("Sequence")) {
            sequence = B.getInt("Sequence");
        } else {
            sequence = null;
        }
    }

    @Override
    public void saveToBundle(Bundle B) {
        super.saveToBundle(B);

        try {
            if (balanceAlter != null) {
                B.putString("BalanceDecimalAlter", balanceAlter.toString());
            }
            if (sequence != null) {
                B.putInt("Sequence", sequence);
            }
        } catch (Exception e) {
            Log.e("Can't save to bundle ", e.getMessage());
        }

    }

}