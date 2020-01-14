package com.tangem.wallet.binance;

import android.os.Bundle;
import android.util.Log;

import com.tangem.wallet.CoinData;
import com.tangem.wallet.CoinEngine;

public class BinanceData extends CoinData {
    private String balance, chainId;
    private Long sequence;
    private Integer accountNumber;
    private boolean error404 = false;

    @Override
    public void loadFromBundle(Bundle B) {
        super.loadFromBundle(B);

        if (B.containsKey("Balance")) balance = B.getString("Balance");
        else balance = null;
        if (B.containsKey("ChainId")) chainId = B.getString("ChainId");
        else chainId = null;
        if (B.containsKey("Sequence")) sequence = B.getLong("Sequence");
        else  sequence = null;
        if (B.containsKey("AccountNumber")) accountNumber = B.getInt("AccountNumber");
        else accountNumber = null;
        if (B.containsKey("Error404")) error404 = B.getBoolean("Error404");
        else error404 = false;
    }

    @Override
    public void saveToBundle(Bundle B) {
        super.saveToBundle(B);
        try {
            if (balance != null) B.putString("Balance", balance);
            if (chainId != null) B.putString("ChainId", chainId);
            if (sequence != null) B.putLong("Sequence", sequence);
            if (accountNumber != null) B.putInt("AccountNumber", accountNumber);
            if (error404) B.putBoolean("Error404", true);
        } catch (Exception e) {
            Log.e("Can't save to bundle ", e.getMessage());
        }
    }

    @Override
    public void clearInfo() {
        super.clearInfo();
        balance = null;
        chainId = null;
        sequence = null;
        accountNumber = null;
        error404 = false;
    }

    public CoinEngine.Amount getBalance() {
        return balance == null ? null : new CoinEngine.Amount(balance, "BNB");
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public boolean hasBalanceInfo() {
        return balance != null;
    }

    public Integer getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(Integer accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Long getSequence() {
        return sequence;
    }

    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }

    public String getChainId() {
        return chainId;
    }

    public void setChainId(String chain_id) {
        this.chainId = chain_id;
    }

    public boolean isError404() {
        return error404;
    }

    public void setError404(boolean error404) {
        this.error404 = error404;
    }
}
