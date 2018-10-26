package com.tangem.domain.wallet;

import android.net.Uri;
import android.text.InputFilter;

import com.tangem.domain.cardReader.CardProtocol;

import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * Created by Ilia on 15.02.2018.
 */

public abstract class CoinEngine {

    @Nullable
    public static final String EXTRA_ENGINE = "CoinEngine";

    public static class InternalAmount extends BigDecimal {
        public InternalAmount() {
            super(0);
        }

        public InternalAmount(String amountString) {
            super(amountString);
        }

        public InternalAmount(long amount) {
            super(amount);
        }

        public InternalAmount(BigDecimal amount) {
            super(amount.unscaledValue(), amount.scale());
        }

        public boolean notZero()
        {
            return compareTo(BigDecimal.ZERO)>0;
        }

        public boolean isZero() {
            return compareTo(BigDecimal.ZERO)==0;
        }
    }

    public static class Amount extends BigDecimal {
        private Blockchain blockchain;

        public Amount() {
            super(0);
            blockchain = Blockchain.Unknown;
        }

        public Amount(String amountString, Blockchain blockchain) {
            super(amountString);
            this.blockchain = blockchain;
        }

        public Amount(Long amount, Blockchain blockchain) {
            super(amount);
            this.blockchain=blockchain;
        }

        public String getCurrency() {
            return blockchain.getCurrency();
        }

        @Override
        public String toString() {
            return super.toString() + " " + blockchain.getCurrency();
        }

        public boolean notZero()
        {
            return compareTo(BigDecimal.ZERO)>0;
        }

        public String getStringToEdit() {
            return super.toString();
        }

    }

    protected TangemContext ctx;

    public CoinEngine() {

    }

    public CoinEngine(TangemContext ctx) {
        this.ctx = ctx;
    }

    public abstract boolean awaitingConfirmation();

    public abstract boolean hasBalanceInfo();

    public abstract boolean isBalanceNotZero();

    public abstract boolean isBalanceAlterNotZero();

    public abstract byte[] sign(String feeValue, String amountValue, boolean IncFee, String toValue, CardProtocol protocol) throws Exception;

    public abstract boolean checkUnspentTransaction() throws Exception;

    public abstract Uri getShareWalletUriExplorer();

    public abstract Uri getShareWalletUri();

//    public abstract Long getBalanceLong(TangemCard card);

    public abstract boolean checkNewTransactionAmount(Amount amount);

    // TODO - move minFeeInInternalUnits to CoinData
    public abstract boolean checkNewTransactionAmountAndFee(Amount amount, Amount fee, Boolean isFeeIncluded, InternalAmount minFeeInInternalUnits);

    public abstract boolean validateBalance(BalanceValidator balanceValidator);

    public abstract Amount getBalance();

    public abstract String getBalanceHTML();

    public abstract String getBalanceCurrencyHTML();

    public abstract InputFilter[] getAmountInputFilters();

    public abstract String getOfflineBalanceHTML();

    public abstract String evaluateFeeEquivalent(String fee);

    public abstract String getFeeCurrencyHTML();

    public abstract boolean isNeedCheckNode();

    public abstract String getBalanceEquivalent();

//    public abstract String getBalanceValue(TangemCard card);

//    public abstract String getAmountDescription(TangemCard card, String amount) throws Exception;

//    public abstract String getAmountEquivalentDescriptor(TangemCard card, String value);

    public abstract boolean validateAddress(String address);

    public abstract String calculateAddress(byte[] pkUncompressed) throws NoSuchProviderException, NoSuchAlgorithmException;

    public abstract Amount convertToAmount(InternalAmount internalAmount);
    public abstract Amount convertToAmount(String strAmount);

    public abstract InternalAmount convertToInternalAmount(Amount amount) throws Exception;
    public abstract InternalAmount convertToInternalAmount(byte[] bytes) throws Exception;

    public abstract byte[] convertToByteArray(InternalAmount internalAmount) throws Exception;


//    public abstract CoinEngine swithToBaseEngine();

    public abstract CoinData createCoinData();

    public abstract String getUnspentInputsDescription();

}