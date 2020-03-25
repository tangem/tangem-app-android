package com.tangem.wallet.xrp;

import android.os.Bundle;
import android.util.Log;

import com.tangem.wallet.CoinData;
import com.tangem.wallet.CoinEngine;

import java.math.BigDecimal;

public class XrpData extends CoinData {
    public XrpData() {
        super();
    }

    private Long balanceConfirmed, balanceUnconfirmed, sequence;

    private Long reserve = 20000000L;

    private Boolean accountNotFound, targetAccountCreated = false;

    @Override
    public void loadFromBundle(Bundle B) {
        super.loadFromBundle(B);

        if (B.containsKey("BalanceConfirmed")) balanceConfirmed = B.getLong("BalanceConfirmed");
        else balanceConfirmed = null;
        if (B.containsKey("BalanceUnconfirmed")) balanceUnconfirmed = B.getLong("BalanceUnconfirmed");
        else balanceUnconfirmed = null;
        if (B.containsKey("Sequence")) sequence = B.getLong("Sequence");
        else  sequence = null;
        if (B.containsKey("Reserve")) reserve = B.getLong("Reserve");
        else reserve = 20000000L;
        if (B.containsKey("AccoundNotFound")) accountNotFound = B.getBoolean("AccoundNotFound");
        else accountNotFound = false;
        if (B.containsKey("TargetAccountCreated")) targetAccountCreated = B.getBoolean("TargetAccountCreated");
        else targetAccountCreated = false;
    }

    @Override
    public void saveToBundle(Bundle B) {
        super.saveToBundle(B);
        try {
            if (balanceConfirmed != null) B.putLong("BalanceConfirmed", balanceConfirmed);
            if (balanceUnconfirmed != null) B.putLong("BalanceUnconfirmed", balanceUnconfirmed);
            if (sequence != null) B.putLong("Sequence", sequence);
            if (reserve != null) B.putLong("Reserve", reserve);
            if (accountNotFound != null) B.putBoolean("AccoundNotFound", accountNotFound);
            if (targetAccountCreated != null) B.putBoolean("TargetAccountCreated", targetAccountCreated);
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
        reserve = 20000000L;
        accountNotFound = false;
        targetAccountCreated = false;
    }

    // balanceUnconfirmed is just the latest balance, it equals balanceConfirmed if no unconfirmed transaction present
    public CoinEngine.InternalAmount getBalanceInInternalUnits() {
        if (balanceUnconfirmed != null)
            return new CoinEngine.InternalAmount(BigDecimal.valueOf(balanceUnconfirmed).subtract(BigDecimal.valueOf(reserve)), "Drops");
        else if (balanceConfirmed != null)
            return new CoinEngine.InternalAmount(BigDecimal.valueOf(balanceConfirmed).subtract(BigDecimal.valueOf(reserve)), "Drops");
        else
            return null;
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

    public void setReserve(Long reserve) {
        this.reserve = reserve;
    }

    public CoinEngine.InternalAmount getReserveInInternalUnits() {
        return new CoinEngine.InternalAmount(BigDecimal.valueOf(reserve), "Drops");
    }

    public boolean isAccountNotFound() {
        return accountNotFound;
    }

    public void setAccountNotFound(boolean accountFound) {
        this.accountNotFound = accountFound;
    }

    public Boolean isTargetAccountCreated() {
        return targetAccountCreated;
    }

    public void setTargetAccountCreated(boolean targetAccountCreated) {
        this.targetAccountCreated = targetAccountCreated;
    }

    public boolean hasBalanceInfo() {
        return balanceConfirmed != null || balanceUnconfirmed != null;
    }

    public boolean hasUnconfirmed() {
        return !balanceConfirmed.equals(balanceUnconfirmed);
    }
}
