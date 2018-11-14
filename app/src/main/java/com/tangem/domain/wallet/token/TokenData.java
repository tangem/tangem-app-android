package com.tangem.domain.wallet.token;

import android.os.Bundle;
import android.util.Log;

import com.tangem.domain.wallet.CoinEngine;
import com.tangem.domain.wallet.eth.EthData;

public class TokenData extends EthData {
    private CoinEngine.InternalAmount balanceAlter = null;


    @Override
    public void clearInfo() {
        super.clearInfo();
        balanceAlter = null;
    }

    public CoinEngine.InternalAmount getBalanceAlterInInternalUnits() {
        return balanceAlter;

    }
    public void setBalanceAlterInInternalUnits(CoinEngine.InternalAmount value) {
        balanceAlter = value;
    }

    @Override
    public void loadFromBundle(Bundle B) {
        super.loadFromBundle(B);

        balanceAlter = new CoinEngine.InternalAmount(B.getString("BalanceDecimalAlter"),"wei");
    }

    @Override
    public void saveToBundle(Bundle B) {
        super.saveToBundle(B);

        try {
            B.putString("BalanceDecimalAlter", balanceAlter.toString());
        } catch (Exception e) {
            Log.e("Can't save to bundle ", e.getMessage());
        }

    }

}