package com.tangem.wallet.xlm;

import android.os.Bundle;
import android.util.Log;

import com.tangem.wallet.CoinData;
import com.tangem.wallet.CoinEngine;

import org.stellar.sdk.KeyPair;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.LedgerResponse;

import java.math.BigDecimal;

/*
 * Created by dvol on 7.01.2019.
 */

public class XlmData extends CoinData {


    public static class AccountResponseEx extends AccountResponse {
        AccountResponseEx(String accountId, Long sequenceNumber) {
            super(KeyPair.fromAccountId(accountId), sequenceNumber);
        }
    }

    private CoinEngine.Amount balance = null;

    private Long sequenceNumber = 0L;
    private CoinEngine.Amount baseReserve = new CoinEngine.Amount("0.5", "XLM");
    private CoinEngine.Amount baseFee = new CoinEngine.Amount("0.00001", "XLM");
    private boolean error404 = false;

    @Override
    public void clearInfo() {
        super.clearInfo();
        balance = null;
        error404 = false;
    }

    CoinEngine.Amount getBalance() {
        if (balance != null) {
            return new CoinEngine.Amount(balance.subtract(getReserve()), "XLM");
        } else {
            return null;
        }
    }

    CoinEngine.Amount getReserve() {
        return new CoinEngine.Amount(baseReserve.multiply(BigDecimal.valueOf(2)), "XLM");
    }

    CoinEngine.Amount getBaseFee() {
        return baseFee;
    }

    AccountResponse getAccountResponse() {
        return new AccountResponseEx(getWallet(), sequenceNumber);
    }

    void setAccountResponse(AccountResponse accountResponse) {
        if (accountResponse.getBalances().length > 0) {
            AccountResponse.Balance balanceResponse = accountResponse.getBalances()[0];
            balance = new CoinEngine.Amount(balanceResponse.getBalance(), "XLM");
        }
        sequenceNumber = accountResponse.getSequenceNumber();
        setBalanceReceived(true);
    }

    void setLedgerResponse(LedgerResponse ledgerResponse) {
        XlmEngine xlmEngine = new XlmEngine();
        baseReserve = xlmEngine.convertToAmount(new CoinEngine.InternalAmount(ledgerResponse.getBaseReserveInStroops(), "stroops"));
        baseFee = xlmEngine.convertToAmount(new CoinEngine.InternalAmount(ledgerResponse.getBaseFeeInStroops(), "stroops"));
    }

    public void incSequenceNumber() {
        sequenceNumber++;
    }

    public boolean isError404() {
        return error404;
    }

    public void setError404(boolean error404) {
        this.error404 = error404;
    }

    @Override
    public void loadFromBundle(Bundle B) {
        super.loadFromBundle(B);

        if (B.containsKey("BalanceCurrency") && B.containsKey("BalanceDecimal")) {
            balance = new CoinEngine.Amount(B.getString("BalanceDecimal"), B.getString("BalanceCurrency"));
        } else {
            balance = null;
        }

        if (B.containsKey("sequenceNumber")) {
            sequenceNumber = B.getLong("sequenceNumber");
        } else {
            sequenceNumber = 0L;
        }

        if (B.containsKey("BaseReserveCurrency") && B.containsKey("BaseReserveDecimal")) {
            baseReserve = new CoinEngine.Amount(B.getString("BaseReserveDecimal"), B.getString("BaseReserveCurrency"));
        } else {
            baseReserve = new CoinEngine.Amount("0.5", "XLM");
        }

        if (B.containsKey("BaseFeeCurrency") && B.containsKey("BaseFeeDecimal")) {
            baseFee = new CoinEngine.Amount(B.getString("BaseFeeDecimal"), B.getString("BaseFeeCurrency"));
        } else {
            baseFee = new CoinEngine.Amount("0.00001", "XLM");
        }

        if (B.containsKey("Error404")) error404 = B.getBoolean("Error404");
        else error404 = false;
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

            if (baseReserve != null) {
                B.putString("BaseReserveCurrency", baseReserve.getCurrency());
                B.putString("BaseReserveDecimal", baseReserve.toValueString());
            }

            if (baseFee != null) {
                B.putString("BaseFeeCurrency", baseFee.getCurrency());
                B.putString("BaseFeeDecimal", baseFee.toValueString());
            }

            if (error404) B.putBoolean("Error404", true);

        } catch (Exception e) {
            Log.e("Can't save to bundle ", e.getMessage());
        }
    }
}

