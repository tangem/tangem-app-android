package com.tangem.domain.wallet.xlm;

import android.os.Bundle;
import android.util.Log;

import com.tangem.domain.wallet.CoinData;
import com.tangem.domain.wallet.CoinEngine;

import org.stellar.sdk.KeyPair;
import org.stellar.sdk.responses.AccountResponse;

/*
 * Created by dvol on 7.01.2019.
 */

public class XlmData extends CoinData {


    public static class AccountResponseEx extends AccountResponse
    {
        AccountResponseEx(String accountId, Long sequenceNumber) {
            super(KeyPair.fromAccountId(accountId), sequenceNumber);
        }
    }

    private CoinEngine.Amount balance = null;

    private Long sequenceNumber = 0L;


    @Override
    public void clearInfo() {
        super.clearInfo();
        balance = null;
    }

    CoinEngine.Amount getBalanceXLM() {
        return balance;

    }

    AccountResponse getAccountResponse() {
        return new AccountResponseEx(getWallet(), sequenceNumber);
    }

    void setAccountResponse(AccountResponse accountResponse) {
        if( accountResponse.getBalances().length>0 ) {
            AccountResponse.Balance balanceResponse = accountResponse.getBalances()[0];
            balance = new CoinEngine.Amount(balanceResponse.getBalance(), "XLM");
        }
        sequenceNumber = accountResponse.getSequenceNumber();
        setBalanceReceived(true);
    }

    public void incSequenceNumber() {
        sequenceNumber++;
    }

    @Override
    public void loadFromBundle(Bundle B) {
        super.loadFromBundle(B);

        if (B.containsKey("BalanceCurrency") && B.containsKey("BalanceDecimal")) {
            String currency = B.getString("BalanceCurrency");
            balance = new CoinEngine.Amount(B.getString("BalanceDecimal"), currency);
        } else {
            balance = null;
        }

        if (B.containsKey("sequenceNumber")) {
            sequenceNumber = B.getLong("sequenceNumber");
        } else {
            sequenceNumber = 0L;
        }
    }

    @Override
    public void saveToBundle(Bundle B) {
        super.saveToBundle(B);
        try {
            if (balance != null) {
                B.putString("BalanceCurrency", balance.getCurrency());
                B.putString("BalanceDecimal", balance.toValueString());
            }

            if (sequenceNumber != null) {
                B.putLong("sequenceNumber", sequenceNumber);
            }

        } catch (Exception e) {
            Log.e("Can't save to bundle ", e.getMessage());
        }

    }
}

