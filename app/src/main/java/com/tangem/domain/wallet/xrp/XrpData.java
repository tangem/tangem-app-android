package com.tangem.domain.wallet.xrp;

import android.os.Bundle;
import android.util.Log;

import com.tangem.domain.wallet.CoinData;
import com.tangem.domain.wallet.CoinEngine;

import java.math.BigDecimal;

public class XrpData extends CoinData {
    public XrpData() {
        super();
    }

    private Long balanceConfirmed, balanceUnconfirmed, sequence;

    @Override
    public void loadFromBundle(Bundle B) {
        super.loadFromBundle(B);

        if (B.containsKey("BalanceConfirmed")) balanceConfirmed = B.getLong("BalanceConfirmed");
        else balanceConfirmed = null;
        if (B.containsKey("BalanceUnconfirmed"))
            balanceUnconfirmed = B.getLong("BalanceUnconfirmed");
        else balanceUnconfirmed = null;
        if (B.containsKey("Sequence")) sequence = B.getLong("Sequence");
    }

    @Override
    public void saveToBundle(Bundle B) {
        super.saveToBundle(B);
        try {
            if (balanceConfirmed != null) B.putLong("BalanceConfirmed", balanceConfirmed);
            if (balanceUnconfirmed != null) B.putLong("BalanceUnconfirmed", balanceUnconfirmed);
            if (sequence != null) B.putLong("Sequence", sequence);
        } catch (Exception e) {
            Log.e("Can't save to bundle ", e.getMessage());
        }
    }

    @Override
    public void clearInfo() {
        super.clearInfo();
        balanceConfirmed = null;
        balanceUnconfirmed = null;
        sequence = null;
    }

    // balanceUnconfirmed is just the latest balance, it equals balanceConfirmed if no unconfirmed transaction present
    public CoinEngine.InternalAmount getBalanceInInternalUnits() {
        if (balanceUnconfirmed != null)
            return new CoinEngine.InternalAmount(BigDecimal.valueOf(balanceUnconfirmed), "Drops");
        else
            return new CoinEngine.InternalAmount(BigDecimal.valueOf(balanceConfirmed), "Drops");
    }

//    public Long getBalanceUnconfirmed() {
//        return balanceUnconfirmed;
//    }

    public void setBalanceConfirmed(Long balance) {
        this.balanceConfirmed = balance;
    }

    public void setBalanceUnconfirmed(Long balance) {
        this.balanceUnconfirmed = balance;
    }

    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }

    public Long getSequence() {
        return sequence;
    }

    public boolean hasBalanceInfo() {
        return balanceConfirmed != null || balanceUnconfirmed != null;
    }

    public boolean hasUnconfirmed() {
        return !balanceConfirmed.equals(balanceUnconfirmed);
    }

}
