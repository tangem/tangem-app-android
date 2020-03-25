package com.tangem.wallet;

import android.net.Uri;
import android.text.InputFilter;

import com.tangem.data.Blockchain;
import com.tangem.tangem_card.reader.CardProtocol;
import com.tangem.tangem_card.tasks.SignTask;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import co.nstant.in.cbor.CborException;

/**
 * Created by Ilia on 15.02.2018.
 */

public abstract class CoinEngine {

    public static class InternalAmount extends BigDecimal {
        private String currency;

        public InternalAmount() {
            super(0);
            currency = "";
        }

        public InternalAmount(String amountString, String currency) {
            super(amountString.replace(',', '.'));
            this.currency = currency;
        }

        public InternalAmount(long amount, String currency) {
            super(amount);
            this.currency = currency;
        }

        public InternalAmount(BigDecimal amount, String currency) {
            super(amount.unscaledValue(), amount.scale());
            this.currency = currency;
        }

        public InternalAmount(BigInteger amount, String currency) {
            super(new BigDecimal(amount).unscaledValue(), new BigDecimal(amount).scale());
            this.currency = currency;
        }

        public boolean notZero() {
            return compareTo(BigDecimal.ZERO) > 0;
        }

        public boolean isZero() {
            return compareTo(BigDecimal.ZERO) == 0;
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

            BigDecimal bd = new BigDecimal(unscaledValue(), scale());
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
            currency = "";
        }

        public Amount(String amountString, String currency) {
            super(amountString.replace(',', '.'));
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

        public boolean notZero() {
            return compareTo(BigDecimal.ZERO) > 0;
        }

        public String toDescriptionString(int decimals) {
            return toValueString(decimals) + " " + currency;
        }

        public String toValueString(int decimals) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            symbols.setDecimalSeparator('.');
            DecimalFormat df = new DecimalFormat();
            df.setDecimalFormatSymbols(symbols);
            df.setMaximumFractionDigits(decimals);

            df.setMinimumFractionDigits(0);

            df.setGroupingUsed(false);

            BigDecimal bd = new BigDecimal(unscaledValue(), scale());
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
            return compareTo(BigDecimal.ZERO) == 0;
        }

        @Override
        public Amount setScale(int newScale) {
            return new Amount(super.setScale(newScale), currency);
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

    // TODO - return string message
    public abstract boolean isExtractPossible();

    public abstract Uri getWalletExplorerUri();

    public abstract Uri getShareWalletUri();

    public Uri getShareWalletUriEx(){
        if (ctx.getBlockchain() == Blockchain.BitcoinCash)
            return getShareWalletUri();
        else
            return Uri.parse(ctx.getBlockchain().name().toLowerCase() + ":" + getShareWalletUri().toString());
    }

    public abstract boolean checkNewTransactionAmount(Amount amount);

    public abstract boolean checkNewTransactionAmountAndFee(Amount amount, Amount fee, Boolean isFeeIncluded);

    public abstract boolean validateBalance(BalanceValidator balanceValidator);

    public abstract Amount getBalance();

    public abstract String getBalanceHTML();

    public abstract String getBalanceCurrency();

    public abstract InputFilter[] getAmountInputFilters();

    public abstract String evaluateFeeEquivalent(String fee);

    public abstract String getFeeCurrency();

    public abstract boolean isNeedCheckNode();

    public abstract String getBalanceEquivalent();

    public abstract boolean validateAddress(String address);

    public abstract String calculateAddress(byte[] pkUncompressed) throws NoSuchProviderException, NoSuchAlgorithmException, CborException, IOException, Exception;

    public abstract Amount convertToAmount(InternalAmount internalAmount) throws Exception;

    public abstract Amount convertToAmount(String strAmount, String currency);

    public abstract InternalAmount convertToInternalAmount(Amount amount) throws Exception;

    public abstract InternalAmount convertToInternalAmount(byte[] bytes) throws Exception;

    public abstract byte[] convertToByteArray(InternalAmount internalAmount) throws Exception;

    public abstract CoinData createCoinData();

    public abstract String getUnspentInputsDescription();

    public String getOfflineBalanceHTML() {
        return "";
    }

//    public String getOfflineBalanceHTML() {
//        InternalAmount offlineInternalAmount = convertToInternalAmount(ctx.getCard().getOfflineBalance());
//        Amount offlineAmount = convertToAmount(offlineInternalAmount);
//        return offlineAmount.toDescriptionString(getDecimals());
//    }

    public void defineWallet() throws CardProtocol.TangemException {
        try {
            String wallet = calculateAddress(ctx.getCard().getWalletPublicKey());
            ctx.getCoinData().setWallet(wallet);
        } catch (Exception e) {
            ctx.getCoinData().setWallet("ERROR");
            throw new CardProtocol.TangemException("Can't define wallet address");
        }

    }

    /**
     * Create instance of {@link SignTask.TransactionToSign} used for transaction signing and sending
     *
     * Transaction processing sequence:
     * 1. User enter transaction attributes
     * 2. Application create instance of {@link SignTask.TransactionToSign} by call {@see constructTransaction}
     * 3. Application set notification when transaction were prepared {@see setOnNeedSendTransaction} and init {@link SignTask}
     * 4. User tap card and card sign transaction
     * 5. Application receive {@link OnNeedSendTransaction} notification with prepared raw transaction
     * 6. Application show user information that transaction ready for sending and init sending procedure by call {@see requestSendTransaction}
     * 7. Application receive notification of sending result through {@link CoinEngine.BlockchainRequestsCallbacks} and show result to user
     *
     * @param amountValue   - amount of desired transaction
     * @param feeValue      - fee amount of desired transaction
     * @param IncFee        - true if fee amount is included in amountValue (amountValue is total amount of transaction)
     * @param targetAddress - target address of transaction
     * @return instance of {@link SignTask.TransactionToSign}
     * @throws Exception if something goes wrong
     */
    public abstract SignTask.TransactionToSign constructTransaction(Amount amountValue, Amount feeValue, boolean IncFee, String targetAddress) throws Exception;

    /**
     * Interface used to notify main application when new transaction is prepared to send
     */
    public interface OnNeedSendTransaction {
        void onTransactionPrepared(byte[] txForSend);
    }

    protected OnNeedSendTransaction onNeedSendTransaction;


    /**
     * Set notification callback when new transaction is prepared to send
     */
    public void setOnNeedSendTransaction(OnNeedSendTransaction onNeedSendTransaction) {
        this.onNeedSendTransaction = onNeedSendTransaction;
    }

    protected void notifyOnNeedSendTransaction(byte[] txForSend) throws Exception {
        if (onNeedSendTransaction == null)
            throw new Exception("Transaction was signed but no callback defined to send!");
        onNeedSendTransaction.onTransactionPrepared(txForSend);
    }

    /**
     * Interface used to notify/querying application during processing sequence of request to blockchain nodes/servers
     */
    public interface BlockchainRequestsCallbacks {
        /**
         * Notification that the all requests in sequence completed
         * Call after a last request completed
         * If occurred error return in {@link TangemContext} {@see TangemContext.getError()}
         *
         * @param success -*
         */
        void onComplete(Boolean success);

        /**
         * Notification that a new part of data received and it's possible to update view
         * May call when some request in the sequence completed but there are still a few requests left
         */
        void onProgress();

        /**
         * Return flag that allow to add new or re-requests in the sequence
         * Call between requests or when request fail and before re-request
         *
         * @return true if not need terminate (e.g. activity is online)
         */
        boolean allowAdvance();
    }

    /**
     * Start sequence of request to blockchain nodes needed to get balance and other information (for example unspent transaction) needed to
     * show current state of wallet and prepare new withdrawal transaction
     * Save result in {@link CoinData}
     * If occurred error can be get at onComplete callback in {@link TangemContext}.getError()
     * @param blockchainRequestsCallbacks - notifications
     * @throws Exception if something goes wrong
     */
    public abstract void requestBalanceAndUnspentTransactions(BlockchainRequestsCallbacks blockchainRequestsCallbacks) throws Exception;

    /**
     * Start sequence of request to blockchain nodes needed to get fee amount for a new transaction
     * Save result in {@link CoinData} minFee, maxFee, normalFee
     * @param blockchainRequestsCallbacks - notifications
     * @throws Exception if something goes wrong
     */
    public abstract void requestFee(BlockchainRequestsCallbacks blockchainRequestsCallbacks, String targetAddress, Amount amount) throws Exception;

    /**
     * Start sequence of request to blockchain nodes needed to send new transaction
     * If occurred error can be get at onComplete callback in {@link TangemContext}.getError()
     * @param blockchainRequestsCallbacks - notifications
     * @throws Exception if something goes wrong
     */
    public abstract void requestSendTransaction(BlockchainRequestsCallbacks blockchainRequestsCallbacks, byte[] txForSend) throws Exception;


    /**
     * @return true if blockchain need multiple lines to show balance (e.g. need show additional balance information, Token count for example)
     */
    public boolean needMultipleLinesForBalance() {
        return false;
    }

    /**
     * @return true to allow user select fee level - min, normal or priority
     */
    public boolean allowSelectFeeLevel() {
        return true;
    }

    /**
     * @return true to allow user select include or exclude fee
     */
    public boolean allowSelectFeeInclusion() {
        return true;
    }

    public boolean isNftToken() {
        return false;
    }


    public int pendingTransactionTimeoutInSeconds() { return 30; }

}