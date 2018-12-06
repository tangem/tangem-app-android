package com.tangem.domain.wallet;

import android.net.Uri;
import android.text.InputFilter;

import com.tangem.tangemcard.reader.CardProtocol;
import com.tangem.tangemcard.tasks.SignTask;

import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Created by Ilia on 15.02.2018.
 */

public abstract class CoinEngine {


    public static class InternalAmount extends BigDecimal {
        private String currency;

        public InternalAmount() {
            super(0);
            currency="";
        }

        public InternalAmount(String amountString, String currency) {
            super(amountString.replace(',','.'));
            this.currency=currency;
        }

        public InternalAmount(long amount, String currency) {
            super(amount);
            this.currency=currency;
        }

        public InternalAmount(BigDecimal amount, String currency) {
            super(amount.unscaledValue(), amount.scale());
            this.currency=currency;
        }

        public InternalAmount(BigInteger amount, String currency) {
            super(new BigDecimal(amount).unscaledValue(), new BigDecimal(amount).scale());
            this.currency=currency;
        }

        public boolean notZero()
        {
            return compareTo(BigDecimal.ZERO)>0;
        }

        public boolean isZero() {
            return compareTo(BigDecimal.ZERO)==0;
        }

        public String getCurrency() {
            return currency;
        }

        public String toValueString(int decimals) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            symbols.setDecimalSeparator('.');
            DecimalFormat df = new DecimalFormat();
            df.setDecimalFormatSymbols(symbols);
            df.setMaximumFractionDigits(decimals);

            df.setMinimumFractionDigits(0);

            df.setGroupingUsed(false);

            BigDecimal bd=new BigDecimal(unscaledValue(), scale());
            bd.setScale(decimals, ROUND_DOWN);
            return df.format(bd);
        }

        public String toValueString() {
            return toValueString(scale());
        }

        @Override
        public String toString() {
            return super.toString();
        }

    }

    public static class Amount extends BigDecimal {
        private String currency;

        public Amount() {
            super(0);
            currency="";
        }

        public Amount(String amountString, String currency) {
            super(amountString.replace(',','.'));
            this.currency = currency;
        }

        public Amount(Long amount, String currency) {
            super(amount);
            this.currency = currency;
        }

        public Amount(BigDecimal amount, String currency) {
            super(amount.unscaledValue(), amount.scale());
            this.currency = currency;
        }

        public String getCurrency() {
            return currency;
        }

        @Override
        public String toString() {
            return super.toString() + " " + currency;
        }

        public boolean notZero()
        {
            return compareTo(BigDecimal.ZERO)>0;
        }

        public String toDescriptionString(int decimals) {
            return toValueString(decimals)+ " " + currency;
        }

        public String toValueString(int decimals) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            symbols.setDecimalSeparator('.');
            DecimalFormat df = new DecimalFormat();
            df.setDecimalFormatSymbols(symbols);
            df.setMaximumFractionDigits(decimals);

            df.setMinimumFractionDigits(0);

            df.setGroupingUsed(false);

            BigDecimal bd=new BigDecimal(unscaledValue(), scale());
            bd.setScale(decimals, ROUND_DOWN);
            return df.format(bd);
        }

        public String toValueString() {
            return toValueString(scale());
        }

        public String toEquivalentString(double rateValue) {
            if (rateValue > 0) {
                BigDecimal biRate = new BigDecimal(rateValue);
                BigDecimal exchangeCurs = biRate.multiply(this);
                exchangeCurs = exchangeCurs.setScale(2, RoundingMode.DOWN);
                return "≈ USD  " + exchangeCurs.toString();
            } else {
                return "";
            }
        }

        public boolean isZero() {
            return compareTo(BigDecimal.ZERO)==0;
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

//    public abstract byte[] sign(Amount feeValue, Amount amountValue, boolean IncFee, String targetAddress, CardProtocol protocol) throws Exception;

    // TODO - change isExtractPossible to isExtractPossible and if not - return string message
    public abstract boolean isExtractPossible();

    public abstract Uri getShareWalletUriExplorer();

    public abstract Uri getShareWalletUri();

    public abstract boolean checkNewTransactionAmount(Amount amount);

    // TODO - move minFeeInInternalUnits to CoinData
    public abstract boolean checkNewTransactionAmountAndFee(Amount amount, Amount fee, Boolean isFeeIncluded);

    public abstract boolean validateBalance(BalanceValidator balanceValidator);

    public abstract Amount getBalance();

    public abstract String getBalanceHTML();

    public abstract String getBalanceCurrency();

    public abstract InputFilter[] getAmountInputFilters();

    public abstract String getOfflineBalanceHTML();

    public abstract String evaluateFeeEquivalent(String fee);

    public abstract String getFeeCurrency();

    public abstract boolean isNeedCheckNode();

    public abstract String getBalanceEquivalent();

    public abstract boolean validateAddress(String address);

    public abstract String calculateAddress(byte[] pkUncompressed) throws NoSuchProviderException, NoSuchAlgorithmException;

    public abstract Amount convertToAmount(InternalAmount internalAmount) throws Exception;
    public abstract Amount convertToAmount(String strAmount, String currency);

    public abstract InternalAmount convertToInternalAmount(Amount amount) throws Exception;
    public abstract InternalAmount convertToInternalAmount(byte[] bytes) throws Exception;

    public abstract byte[] convertToByteArray(InternalAmount internalAmount) throws Exception;

    public abstract CoinData createCoinData();

    public abstract String getUnspentInputsDescription();

    public void defineWallet() throws CardProtocol.TangemException {
        try {
            String wallet = calculateAddress(ctx.getCard().getWalletPublicKey());
            ctx.getCoinData().setWallet(wallet);
        }
        catch (Exception e)
        {
            ctx.getCoinData().setWallet("ERROR");
            throw new CardProtocol.TangemException("Can't define wallet address");
        }

    }

    public abstract SignTask.PaymentToSign constructPayment(Amount feeValue, Amount amountValue, boolean IncFee, String targetAddress) throws Exception;

    public interface OnNeedSendPayment
    {
        void onPaymentPrepared(byte[] txForSend);
    }
    private OnNeedSendPayment onNeedSendPayment;

    public void setOnNeedSendPayment(OnNeedSendPayment onNeedSendPayment) {
        this.onNeedSendPayment = onNeedSendPayment;
    }

    protected void notifyOnNeedSendPayment(byte[] txForSend) throws Exception {
        if(onNeedSendPayment==null)
            throw new Exception("Payment signed but no callback defined to send!");
        onNeedSendPayment.onPaymentPrepared(txForSend);

    }
}