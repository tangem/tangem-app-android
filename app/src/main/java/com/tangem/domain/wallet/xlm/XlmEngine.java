package com.tangem.domain.wallet.xlm;

import android.net.Uri;
import android.text.InputFilter;
import android.util.Log;

import com.tangem.data.Blockchain;
import com.tangem.data.network.ServerApiStellar;
import com.tangem.data.network.StellarRequest;
import com.tangem.domain.wallet.BalanceValidator;
import com.tangem.domain.wallet.CoinData;
import com.tangem.domain.wallet.CoinEngine;
import com.tangem.domain.wallet.TangemContext;
import com.tangem.tangemcard.data.TangemCard;
import com.tangem.tangemcard.tasks.SignTask;
import com.tangem.tangemcard.util.Util;
import com.tangem.util.DecimalDigitsInputFilter;
import com.tangem.wallet.R;

import org.stellar.sdk.AssetTypeNative;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Memo;
import org.stellar.sdk.PaymentOperation;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.xdr.TransactionEnvelope;
import org.stellar.sdk.xdr.XdrDataInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;

public class XlmEngine extends CoinEngine {

    private static final String TAG = XlmEngine.class.getSimpleName();

    public XlmData coinData = null;

    public XlmEngine(TangemContext context) throws Exception {
        super(context);
        if (context.getCoinData() == null) {
            coinData = new XlmData();
            context.setCoinData(coinData);
        } else if (context.getCoinData() instanceof XlmData) {
            coinData = (XlmData) context.getCoinData();
        } else {
            throw new Exception("Invalid type of Blockchain data for XlmEngine");
        }
    }

    public XlmEngine() {
        super();
    }

    private static int getDecimals() {
        return 8;
    }


    private void checkBlockchainDataExists() throws Exception {
        if (coinData == null) throw new Exception("No blockchain data");
    }

    @Override
    public boolean awaitingConfirmation() {
        if (coinData == null) return false;
        //TODO
        return false;//coinData.getBalanceUnconfirmed() != 0;
    }

    @Override
    public String getBalanceHTML() {
        Amount balance = getBalance();
        if (balance != null) {
            return balance.toDescriptionString(getDecimals());
        } else {
            return "";
        }
    }

    @Override
    public String getBalanceCurrency() {
        return "XLM";
    }

    @Override
    public String getOfflineBalanceHTML() {
        InternalAmount offlineInternalAmount = convertToInternalAmount(ctx.getCard().getOfflineBalance());
        Amount offlineAmount = convertToAmount(offlineInternalAmount);
        return offlineAmount.toDescriptionString(getDecimals());
    }

    @Override
    public boolean isBalanceNotZero() {
        if (coinData == null) return false;
        if (coinData.getBalanceInInternalUnits() == null) return false;
        return coinData.getBalanceInInternalUnits().notZero();
    }

    @Override
    public boolean hasBalanceInfo() {
        if (coinData == null) return false;
        return coinData.getBalanceInInternalUnits() != null;
    }


    @Override
    public boolean isExtractPossible() {
        if (!hasBalanceInfo()) {
            ctx.setMessage(R.string.cannot_obtain_data_from_blockchain);
        } else if (!isBalanceNotZero()) {
            ctx.setMessage(R.string.wallet_empty);
        } else if (awaitingConfirmation()) {
            ctx.setMessage(R.string.please_wait_while_previous);
        } else {
            return true;
        }
        return false;
    }

    @Override
    public String getFeeCurrency() {
        return "XLM";
    }

    @Override
    public boolean validateAddress(String address) {
        try {
            KeyPair kp = KeyPair.fromAccountId(address);
            // TODO is it possible to check address testNet or not
//            if (ctx.getBlockchain() == Blockchain.StellarTestNet) {
//                return false;
//            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }


    @Override
    public boolean isNeedCheckNode() {
        return false;
    }

    @Override
    public Uri getShareWalletUriExplorer() {
        //TODO - ?
        return Uri.parse((ctx.getBlockchain() == Blockchain.Bitcoin ? "https://blockchain.info/address/" : "https://testnet.blockchain.info/address/") + ctx.getCoinData().getWallet());
    }

    @Override
    public Uri getShareWalletUri() {
        //TODO - ?
        if (ctx.getCard().getDenomination() != null) {
            return Uri.parse("bitcoin:" + ctx.getCoinData().getWallet() + "?amount=" + convertToAmount(convertToInternalAmount(ctx.getCard().getDenomination())).toValueString(8));
        } else {
            return Uri.parse("bitcoin:" + ctx.getCoinData().getWallet());
        }
    }

    @Override
    public InputFilter[] getAmountInputFilters() {
        return new InputFilter[]{new DecimalDigitsInputFilter(getDecimals())};
    }

    @Override
    public boolean checkNewTransactionAmount(Amount amount) {
        if (coinData == null) return false;
        if (amount.compareTo(convertToAmount(coinData.getBalanceInInternalUnits())) > 0) {
            return false;
        }
        return true;
    }

    @Override
    public boolean checkNewTransactionAmountAndFee(Amount amountValue, Amount feeValue, Boolean isIncludeFee) {
        InternalAmount fee;
        InternalAmount amount;

        try {
            checkBlockchainDataExists();
            amount = convertToInternalAmount(amountValue);
            fee = convertToInternalAmount(feeValue);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        if (fee == null || amount == null)
            return false;

        if (fee.isZero() || amount.isZero())
            return false;

        if (isIncludeFee && (amount.compareTo(coinData.getBalanceInInternalUnits()) > 0 || amount.compareTo(fee) < 0))
            return false;

        if (!isIncludeFee && amount.add(fee).compareTo(coinData.getBalanceInInternalUnits()) > 0)
            return false;

        return true;
    }

    @Override
    public boolean validateBalance(BalanceValidator balanceValidator) {
        try {
            if (((ctx.getCard().getOfflineBalance() == null) && !ctx.getCoinData().isBalanceReceived()) || (!ctx.getCoinData().isBalanceReceived() && (ctx.getCard().getRemainingSignatures() != ctx.getCard().getMaxSignatures()))) {
                balanceValidator.setScore(0);
                balanceValidator.setFirstLine("Unknown balance");
                balanceValidator.setSecondLine("Balance cannot be verified. Swipe down to refresh.");
                return false;
            }

            // Workaround before new back-end
//        if (card.getRemainingSignatures() == card.getMaxSignatures()) {
//            firstLine = "Verified balance";
//            secondLine = "Balance confirmed in blockchain. ";
//            secondLine += "Verified note identity. ";
//            return;
//        }

//            if (coinData.getBalanceUnconfirmed() != 0) {
//                balanceValidator.setScore(0);
//                balanceValidator.setFirstLine("Transaction in progress");
//                balanceValidator.setSecondLine("Wait for confirmation in blockchain");
//                return false;
//            }

            if (coinData.isBalanceReceived() && coinData.isBalanceEqual()) {
                balanceValidator.setScore(100);
                balanceValidator.setFirstLine("Verified balance");
                balanceValidator.setSecondLine("Balance confirmed in blockchain");
                if (coinData.getBalanceInInternalUnits().isZero()) {
                    balanceValidator.setFirstLine("Empty wallet");
                    balanceValidator.setSecondLine("");
                }
            }

            // rule 4 TODO: need to check SignedHashed against number of outputs in blockchain
//        if((card.getRemainingSignatures() != card.getMaxSignatures()) && card.getBalance() != 0)
//        {
//            score = 80;
//            firstLine = "Unguaranteed balance";
//            secondLine = "Potential unsent transaction. Redeem immediately if accept. ";
//            return;
//        }

            if ((ctx.getCard().getOfflineBalance() != null) && !coinData.isBalanceReceived() && (ctx.getCard().getRemainingSignatures() == ctx.getCard().getMaxSignatures()) && coinData.getBalanceInInternalUnits().notZero()) {
                balanceValidator.setScore(80);
                balanceValidator.setFirstLine("Verified offline balance");
                balanceValidator.setSecondLine("Can't obtain balance from blockchain. Restore internet connection to be more confident. ");
            }

//            if(card.getFailedBalanceRequestCounter()!=0) {
//                score -= 5 * card.getFailedBalanceRequestCounter();
//                secondLine += "Not all nodes have returned balance. Swipe down or tap again. ";
//                if(score <= 0)
//                    return;
//            }

            //
//            if(card.isBalanceReceived() && !card.isBalanceEqual()) {
//                score = 0;
//                firstLine = "Disputed balance";
//                secondLine += " Cannot obtain trusted balance at the moment. Try to tap and check this banknote later.";
//                return;
//            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Amount getBalance() {
        if (!hasBalanceInfo()) return null;
        return convertToAmount(coinData.getBalanceInInternalUnits());
    }

    @Override
    public String evaluateFeeEquivalent(String fee) {
        if (!coinData.getAmountEquivalentDescriptionAvailable()) return "";
        try {
            Amount feeAmount = new Amount(fee, getFeeCurrency());
            return feeAmount.toEquivalentString(coinData.getRate());
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public String getBalanceEquivalent() {
        if (coinData == null || !coinData.getAmountEquivalentDescriptionAvailable()) return "";
        Amount balance = getBalance();
        if (balance == null) return "";
        return balance.toEquivalentString(coinData.getRate());
    }

    @Override
    public String calculateAddress(byte[] pkUncompressed) {
        KeyPair kp = KeyPair.fromPublicKey(pkUncompressed);
        return kp.getAccountId();
    }

    private static BigDecimal multiplier = new BigDecimal("1000000");

    @Override
    public Amount convertToAmount(InternalAmount internalAmount) {
        BigDecimal d = internalAmount.divide(multiplier);
        return new Amount(d, getBalanceCurrency());
    }

    @Override
    public Amount convertToAmount(String strAmount, String currency) {
        return new Amount(strAmount, currency);
    }

    @Override
    public InternalAmount convertToInternalAmount(Amount amount) {
        BigDecimal d = amount.multiply(multiplier);
        return new InternalAmount(d, "stroops");
    }

    @Override
    public InternalAmount convertToInternalAmount(byte[] bytes) {
        if (bytes == null) return null;
        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) reversed[i] = bytes[bytes.length - i - 1];
        return new InternalAmount(Util.byteArrayToLong(reversed), "stroops");
    }

    @Override
    public byte[] convertToByteArray(InternalAmount internalAmount) {
        byte[] bytes = Util.longToByteArray(internalAmount.longValueExact());
        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) reversed[i] = bytes[bytes.length - i - 1];
        return reversed;
    }

    @Override
    public CoinData createCoinData() {
        return new XlmData();
    }

    @Override
    public String getUnspentInputsDescription() {
        return "";
    }

    @Override
    public SignTask.PaymentToSign constructPayment(Amount amountValue, Amount feeValue, boolean IncFee, String targetAddress) throws Exception {
        checkBlockchainDataExists();

        Transaction transaction = new Transaction.Builder(coinData.getAccountResponse())
                .addOperation(new PaymentOperation.Builder(KeyPair.fromAccountId(targetAddress), new AssetTypeNative(), amountValue.toValueString()).build())
                // A memo allows you to add your own metadata to a transaction. It's
                // optional and does not affect how Stellar treats the transaction.
                .addMemo(Memo.text("TangemCard Transaction"))
                .build();

        if( transaction.getFee()!=convertToInternalAmount(feeValue).intValueExact() )
        {
            throw new Exception("Invalid fee!");
        }

        return new SignTask.PaymentToSign() {

            @Override
            public boolean isSigningMethodSupported(TangemCard.SigningMethod signingMethod) {
                return signingMethod == TangemCard.SigningMethod.Sign_Hash || signingMethod == TangemCard.SigningMethod.Sign_Raw;
            }

            @Override
            public byte[][] getHashesToSign() throws Exception {
                byte[][] dataForSign = new byte[1][];
                dataForSign[0] = transaction.hash();
                return dataForSign;
            }

            @Override
            public byte[] getRawDataToSign() throws Exception {
                throw new Exception("Hashes length must be identical!");
            }

            @Override
            public String getHashAlgToSign() {
                return "sha-256x2";
            }

            @Override
            public byte[] getIssuerTransactionSignature(byte[] dataToSignByIssuer) throws Exception {
                throw new Exception("Issuer validation not supported!");
            }

            @Override
            public byte[] onSignCompleted(byte[] signFromCard) throws Exception {
                // Sign the transaction to prove you are actually the person sending it.
                transaction.sign(signFromCard);

                byte[] txForSend = transaction.toEnvelopeXdrBase64().getBytes();
                notifyOnNeedSendPayment(txForSend);
                return txForSend;
            }
        };
    }

    @Override
    public void requestBalanceAndUnspentTransactions(BlockchainRequestsCallbacks blockchainRequestsCallbacks) {
        final ServerApiStellar serverApi = new ServerApiStellar();

        ServerApiStellar.Listener listener = new ServerApiStellar.Listener() {
            @Override
            public void onSuccess(StellarRequest.Base request) {
                Log.i(TAG, "onSuccess: " + request.getClass().getSimpleName());
                if (!StellarRequest.Balance.class.isInstance(request)) {
                    ctx.setError("Invalid request logic");
                    blockchainRequestsCallbacks.onComplete(false);
                    return;
                }
                StellarRequest.Balance balanceRequest = (StellarRequest.Balance) request;

                coinData.setAccountResponse(balanceRequest.accountResponse);
                AccountResponse.Balance balance=balanceRequest.accountResponse.getBalances()[0];
                coinData.setBalanceInInternalUnits(new InternalAmount(balance.getBalance(), "stroops"));
                if (serverApi.isRequestsSequenceCompleted()) {
                    blockchainRequestsCallbacks.onComplete(!ctx.hasError());
                } else {
                    blockchainRequestsCallbacks.onProgress();
                }
            }


            @Override
            public void onFail(StellarRequest.Base request) {
                Log.i(TAG, "onFail: " + request.getClass().getSimpleName() + " " + request.getError());
                ctx.setError(request.getError());
                if (serverApi.isRequestsSequenceCompleted()) {
                    blockchainRequestsCallbacks.onComplete(false);
                } else {
                    blockchainRequestsCallbacks.onProgress();
                }
            }
        };

        serverApi.setListener(listener);

        serverApi.requestData(ctx, new StellarRequest.Balance(coinData.getWallet()));
    }

    @Override
    public void requestFee(BlockchainRequestsCallbacks blockchainRequestsCallbacks, String targetAddress, Amount amount) throws Exception {
        final int calcSize = 0;
        Log.e(TAG, String.format("Estimated tx size %d", calcSize));
        coinData.minFee = null;
        coinData.maxFee = null;
        coinData.normalFee = null;
        coinData.minFee = new Amount("0.00001", getFeeCurrency());
        coinData.normalFee = new Amount("0.00001", getFeeCurrency());
        coinData.maxFee = new Amount("0.00001", getFeeCurrency());
        blockchainRequestsCallbacks.onComplete(true);
    }

    @Override
    public void requestSendTransaction(BlockchainRequestsCallbacks blockchainRequestsCallbacks, byte[] txForSend) throws IOException {
        final ServerApiStellar serverApi = new ServerApiStellar();

        ServerApiStellar.Listener listener = new ServerApiStellar.Listener() {
            @Override
            public void onSuccess(StellarRequest.Base request) {
                try {
                    if (!StellarRequest.SubmitTransaction.class.isInstance(request)) throw new Exception("Invalid request logic");
                    StellarRequest.SubmitTransaction submitTransactionRequest = (StellarRequest.SubmitTransaction) request;
                    if (submitTransactionRequest.response.isSuccess()) {
                        ctx.setError(null);
                        blockchainRequestsCallbacks.onComplete(true);
                    } else {
                        ctx.setError("Rejected by node");
                        blockchainRequestsCallbacks.onComplete(false);
                    }
                } catch (Exception e) {
                    if (e.getMessage() != null) {
                        ctx.setError(e.getMessage());
                        blockchainRequestsCallbacks.onComplete(false);
                    } else {
                        ctx.setError(e.getClass().getName());
                        blockchainRequestsCallbacks.onComplete(false);
                    }
                }

            }

            @Override
            public void onFail(StellarRequest.Base request) {
                ctx.setError(request.getError());
                blockchainRequestsCallbacks.onComplete(false);
            }
        };
        serverApi.setListener(listener);

        TransactionEnvelope transactionEnvelope = TransactionEnvelope.decode(new XdrDataInputStream(new ByteArrayInputStream(txForSend)));
        org.stellar.sdk.Transaction transaction = org.stellar.sdk.Transaction.fromEnvelopeXdr(transactionEnvelope);
        serverApi.requestData(ctx, new StellarRequest.SubmitTransaction(transaction));

    }

}