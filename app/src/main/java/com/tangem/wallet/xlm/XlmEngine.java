package com.tangem.wallet.xlm;

import android.net.Uri;
import android.os.StrictMode;
import android.text.InputFilter;
import android.util.Log;

import com.tangem.card_common.data.TangemCard;
import com.tangem.card_common.tasks.SignTask;
import com.tangem.card_common.util.Util;
import com.tangem.data.Blockchain;
import com.tangem.data.network.ServerApiStellar;
import com.tangem.data.network.StellarRequest;
import com.tangem.util.DecimalDigitsInputFilter;
import com.tangem.wallet.BalanceValidator;
import com.tangem.wallet.CoinData;
import com.tangem.wallet.CoinEngine;
import com.tangem.wallet.R;
import com.tangem.wallet.TangemContext;

import org.stellar.sdk.AssetTypeNative;
import org.stellar.sdk.CreateAccountOperation;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Operation;
import org.stellar.sdk.PaymentOperation;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.TransactionEx;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Created by dvol on 7.01.2019.
 * <p>
 * PS. To create and fill testnet account just open https://friendbot.stellar.org/?addr=XXX in browser
 **/

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
        return 7;
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
            return " " + balance.toDescriptionString(getDecimals()) + "<br><small><small>+ " + coinData.getReserve().toDescriptionString(getDecimals()) + " reserve</small></small>";
        } else {
            return "";
        }
    }

    @Override
    public String getBalanceCurrency() {
        return "XLM";
    }

    @Override
    public boolean isBalanceNotZero() {
        if (coinData == null) return false;
        if (coinData.getBalance() == null) return false;
        return coinData.getBalance().notZero();
    }

    @Override
    public boolean hasBalanceInfo() {
        if (coinData == null) return false;
        return coinData.getBalance() != null;
    }


    @Override
    public boolean isExtractPossible() {
        if (!hasBalanceInfo()) {
            ctx.setMessage(R.string.loaded_wallet_error_obtaining_blockchain_data);
        } else if (!isBalanceNotZero()) {
            ctx.setMessage(R.string.general_wallet_empty);
        } else if (awaitingConfirmation()) {
            ctx.setMessage(R.string.loaded_wallet_message_wait);
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
    public Uri getWalletExplorerUri() {
        return Uri.parse((ctx.getBlockchain() == Blockchain.Stellar ? "http://stellarchain.io/address/" : "http://testnet.stellarchain.io/address/") + ctx.getCoinData().getWallet());
    }

    @Override
    public Uri getShareWalletUri() {
        //TODO - how to construct payment query intent for stellar?
        if (ctx.getCard().getDenomination() != null) {
            return Uri.parse(ctx.getCoinData().getWallet() + "?amount=" + convertToAmount(convertToInternalAmount(ctx.getCard().getDenomination())).toValueString());
        } else {
            return Uri.parse(ctx.getCoinData().getWallet());
        }
    }

    @Override
    public InputFilter[] getAmountInputFilters() {
        return new InputFilter[]{new DecimalDigitsInputFilter(getDecimals())};
    }

    @Override
    public boolean checkNewTransactionAmount(Amount amount) {
        if (coinData == null) return false;
        if (amount.compareTo(coinData.getBalance()) > 0) {
            return false;
        }
        return true;
    }

    @Override
    public boolean checkNewTransactionAmountAndFee(Amount amountValue, Amount feeValue, Boolean isIncludeFee) {
        try {
            checkBlockchainDataExists();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        if (feeValue == null || amountValue == null)
            return false;

        if (feeValue.isZero() || amountValue.isZero())
            return false;

        if (isIncludeFee && (amountValue.compareTo(coinData.getBalance()) > 0 || amountValue.compareTo(feeValue) < 0))
            return false;

        if (!isIncludeFee && amountValue.add(feeValue).compareTo(coinData.getBalance()) > 0)
            return false;

        return true;
    }

    @Override
    public boolean validateBalance(BalanceValidator balanceValidator) {
        try {
            if (((ctx.getCard().getOfflineBalance() == null) && !ctx.getCoinData().isBalanceReceived()) || (!ctx.getCoinData().isBalanceReceived() && (ctx.getCard().getRemainingSignatures() != ctx.getCard().getMaxSignatures()))) {
                if (coinData.isError404()) {
                    balanceValidator.setScore(0);
                    balanceValidator.setFirstLine("No account or network error");
                    balanceValidator.setSecondLine("To create account send 1+ XLM to this address");
                } else {
                    balanceValidator.setScore(0);
                    balanceValidator.setFirstLine("Unknown balance");
                    balanceValidator.setSecondLine("Balance cannot be verified. Swipe down to refresh.");
                    return false;
                }
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
                if (coinData.getBalance().isZero()) {
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

            if ((ctx.getCard().getOfflineBalance() != null) && !coinData.isBalanceReceived() && (ctx.getCard().getRemainingSignatures() == ctx.getCard().getMaxSignatures()) && coinData.getBalance().notZero()) {
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
        return coinData.getBalance();
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

    private static BigDecimal multiplier = new BigDecimal("10000000");

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
    public SignTask.TransactionToSign constructTransaction(Amount amountValue, Amount feeValue, boolean IncFee, String targetAddress) throws Exception {
        checkBlockchainDataExists();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);

        if (IncFee) {
            amountValue = new Amount(amountValue.subtract(feeValue), amountValue.getCurrency());
        }

        Operation operation;
        if (isAccountCreated(targetAddress))
            operation = new PaymentOperation.Builder(KeyPair.fromAccountId(targetAddress), new AssetTypeNative(), amountValue.toValueString()).build();
        else
            operation = new CreateAccountOperation.Builder(KeyPair.fromAccountId(targetAddress), amountValue.toValueString()).build();

        TransactionEx transaction = TransactionEx.buildEx(60, coinData.getAccountResponse(), operation);


        if (transaction.getFee() != convertToInternalAmount(feeValue).intValueExact()) {
            throw new Exception("Invalid fee!");
        }

        return new SignTask.TransactionToSign() {

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
                return transaction.signatureBase();
            }

            @Override
            public String getHashAlgToSign() {
                return "sha-256";
            }

            @Override
            public byte[] getIssuerTransactionSignature(byte[] dataToSignByIssuer) throws Exception {
                throw new Exception("Issuer validation not supported!");
            }

            @Override
            public byte[] onSignCompleted(byte[] signFromCard) throws Exception {
                // Sign the transaction to prove you are actually the person sending it.
                transaction.setSign(signFromCard);

                byte[] txForSend = transaction.toEnvelopeXdrBase64().getBytes();
                notifyOnNeedSendTransaction(txForSend);
                return txForSend;
            }
        };
    }


    // network call inside, don't use on main thread
    private boolean isAccountCreated(String address) {
        final ServerApiStellar serverApi = new ServerApiStellar();

        StellarRequest.Balance request = new StellarRequest.Balance(address);

        try {
            serverApi.doStellarRequest(ctx, request);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return true; // suppose account is created if anything goes wrong TODO:check
        }

        if (request.errorResponse != null && request.errorResponse.getCode() == 404)
            return false;
        else
            return true;
    }

    @Override
    public void requestBalanceAndUnspentTransactions(BlockchainRequestsCallbacks blockchainRequestsCallbacks) {
        final ServerApiStellar serverApi = new ServerApiStellar();

        ServerApiStellar.Listener listener = new ServerApiStellar.Listener() {
            @Override
            public void onSuccess(StellarRequest.Base request) {
                Log.i(TAG, "onSuccess: " + request.getClass().getSimpleName());

                if (request instanceof StellarRequest.Balance) {
                    StellarRequest.Balance balanceRequest = (StellarRequest.Balance) request;

                    coinData.setAccountResponse(balanceRequest.accountResponse);

                    if (serverApi.isRequestsSequenceCompleted()) {
                        blockchainRequestsCallbacks.onComplete(!ctx.hasError());
                    } else {
                        blockchainRequestsCallbacks.onProgress();
                    }
                } else if (request instanceof StellarRequest.Ledgers) {
                    StellarRequest.Ledgers ledgersRequest = (StellarRequest.Ledgers) request;

                    coinData.setLedgerResponse(ledgersRequest.ledgerResponse);

                    if (serverApi.isRequestsSequenceCompleted()) {
                        blockchainRequestsCallbacks.onComplete(!ctx.hasError());
                    } else {
                        blockchainRequestsCallbacks.onProgress();
                    }
                } else {
                    ctx.setError("Invalid request logic");
                    blockchainRequestsCallbacks.onComplete(false);
                }
            }


            @Override
            public void onFail(StellarRequest.Base request) {
                Log.i(TAG, "onFail: " + request.getClass().getSimpleName() + " " + request.getError());

                if (request.errorResponse.getCode() == 404) {
                    coinData.setError404(true);
                } else {
                    ctx.setError(request.getError());
                }

                if (serverApi.isRequestsSequenceCompleted()) {
                    if (ctx.hasError()) {
                        blockchainRequestsCallbacks.onComplete(false);
                    } else {
                        blockchainRequestsCallbacks.onComplete(true);
                    }
                } else {
                    blockchainRequestsCallbacks.onProgress();
                }
            }
        };

        serverApi.setListener(listener);

        serverApi.requestData(ctx, new StellarRequest.Balance(coinData.getWallet()));
        serverApi.requestData(ctx, new StellarRequest.Ledgers());
    }

    @Override
    public void requestFee(BlockchainRequestsCallbacks blockchainRequestsCallbacks, String targetAddress, Amount amount) throws Exception {
        // TODO: get fee stats?
        coinData.minFee = coinData.normalFee = coinData.maxFee = coinData.getBaseFee();
        blockchainRequestsCallbacks.onComplete(true);
    }

    @Override
    public void requestSendTransaction(BlockchainRequestsCallbacks blockchainRequestsCallbacks, byte[] txForSend) throws IOException {
        final ServerApiStellar serverApi = new ServerApiStellar();

        ServerApiStellar.Listener listener = new ServerApiStellar.Listener() {
            @Override
            public void onSuccess(StellarRequest.Base request) {
                try {
                    if (!StellarRequest.SubmitTransaction.class.isInstance(request))
                        throw new Exception("Invalid request logic");
                    StellarRequest.SubmitTransaction submitTransactionRequest = (StellarRequest.SubmitTransaction) request;
                    if (submitTransactionRequest.response.isSuccess()) {
                        ctx.setError(null);
                        blockchainRequestsCallbacks.onComplete(true);
                    } else {
                        if (submitTransactionRequest.response.getExtras() != null && submitTransactionRequest.response.getExtras().getResultCodes() != null) {
                            String trResult = submitTransactionRequest.response.getExtras().getResultCodes().getTransactionResultCode();
                            if (submitTransactionRequest.response.getExtras().getResultCodes().getOperationsResultCodes() != null && submitTransactionRequest.response.getExtras().getResultCodes().getOperationsResultCodes().size() > 0) {
                                trResult += "/" + submitTransactionRequest.response.getExtras().getResultCodes().getOperationsResultCodes().get(0);
                            }
                            ctx.setError(trResult);
                        } else {
                            ctx.setError("transaction failed");
                        }
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

        Transaction transaction = TransactionEx.fromEnvelopeXdr(new String(txForSend));
        coinData.incSequenceNumber();
        serverApi.requestData(ctx, new StellarRequest.SubmitTransaction(transaction));

    }

    public boolean needMultipleLinesForBalance() {
        return true;
    }

    public boolean allowSelectFeeLevel() {
        return false;
    }

    public int pendingTransactionTimeoutInSeconds() {
        return 10;
    }

}