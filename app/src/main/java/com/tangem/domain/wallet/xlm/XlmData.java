package com.tangem.domain.wallet.xlm;

import android.os.Bundle;
import android.util.Log;

import com.tangem.domain.wallet.CoinData;
import com.tangem.domain.wallet.CoinEngine;

import org.stellar.sdk.Account;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.GsonSingleton;

import java.math.BigInteger;

public class XlmData extends CoinData {
    private CoinEngine.InternalAmount balance = null;
    private AccountResponse accountResponse = null;


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

    public AccountResponse getAccountResponse() {
        return accountResponse;
    }

    public void setAccountResponse(AccountResponse accountResponse) {
        this.accountResponse = accountResponse;
    }

    @Override
    public void loadFromBundle(Bundle B) {
        super.loadFromBundle(B);

        if (B.containsKey("BalanceCurrency") && B.containsKey("BalanceDecimal")) {
            String currency = B.getString("BalanceCurrency");
            balance = new CoinEngine.InternalAmount(B.getString("BalanceDecimal"), currency);
        } else {
            balance = null;
        }

        if (B.containsKey("accountResponse")) {
            accountResponse = GsonSingleton.getInstance().fromJson(B.getString("accountResponse"), AccountResponse.class);
        } else {
            accountResponse = null;
        }

    }

    @Override
    public void saveToBundle(Bundle B) {
        super.saveToBundle(B);
        try {
            if (balance != null) {
                B.putString("BalanceCurrency", balance.getCurrency());
                B.putString("BalanceDecimal", balance.toString());
            }

            if (accountResponse != null) {
                B.putString("accountResponse", GsonSingleton.getInstance().toJson(accountResponse));
            }

        } catch (Exception e) {
            Log.e("Can't save to bundle ", e.getMessage());
        }

    }

}