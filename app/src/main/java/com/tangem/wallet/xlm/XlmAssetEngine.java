package com.tangem.wallet.xlm;

import android.net.Uri;
import android.text.InputFilter;
import android.util.Log;

import com.tangem.App;
import com.tangem.data.network.ServerApiStellar;
import com.tangem.data.network.StellarRequest;
import com.tangem.tangem_card.data.TangemCard;
import com.tangem.tangem_card.tasks.SignTask;
import com.tangem.tangem_card.util.Util;
import com.tangem.util.DecimalDigitsInputFilter;
import com.tangem.wallet.BalanceValidator;
import com.tangem.wallet.CoinData;
import com.tangem.wallet.CoinEngine;
import com.tangem.wallet.R;
import com.tangem.wallet.TangemContext;

import org.stellar.sdk.Asset;
import org.stellar.sdk.AssetTypeNative;
import org.stellar.sdk.ChangeTrustOperation;
import org.stellar.sdk.CreateAccountOperation;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Operation;
import org.stellar.sdk.PaymentOperation;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.TransactionEx;
import org.stellar.sdk.responses.operations.OperationResponse;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Created by dvol on 7.01.2019.
 * <p>
 * PS. To create and fill testnet account just open https://friendbot.stellar.org/?addr=XXX in browser
 **/

public class XlmAssetEngine extends CoinEngine {

    private static final String TAG = XlmAssetEngine.class.getSimpleName();

    public XlmAssetData coinData = null;

    public XlmAssetEngine(TangemContext context) throws Exception {
        super(context);
        if (context.getCoinData() == null) {
            coinData = new XlmAssetData();
            context.setCoinData(coinData);
        } else if (context.getCoinData() instanceof XlmAssetData) {
            coinData = (XlmAssetData) context.getCoinData();
        } else {
            throw new Exception("Invalid type of Blockchain data for XlmAssetEngine");
        }
    }

    public XlmAssetEngine() {
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
        return App.pendingTransactionsStorage.hasTransactions(ctx.getCard());
    }

    @Override
    public Amount getBalance() {
        if (!hasBalanceInfo()) return null;
        if (!coinData.isAssetBalanceZero())
            return coinData.getAssetBalance();
        else
            return coinData.getXlmBalance();
    }

    @Override
    public String getBalanceHTML() {
        Amount balance = coinData.getXlmBalance();
        Amount assetBalance = coinData.getAssetBalance();

        if (balance != null) {
            if (!coinData.isAssetBalanceZero()) {
                return " " + assetBalance.toDescriptionString(getDecimals()) + "<br><small><small>" + balance.toDescriptionString(getDecimals()) + " for fee + " + coinData.getReserve().toDescriptionString(getDecimals()) + " reserve</small></small>";
            }
            return " " + balance.toDescriptionString(getDecimals()) + "<br><small><small>+ " + coinData.getReserve().toDescriptionString(getDecimals()) + " reserve</small></small>";
        } else {
            return "";
        }
    }

    @Override
    public String getBalanceCurrency() {
        if (!coinData.isAssetBalanceZero()) {
            return coinData.getAssetBalance().getCurrency();
        } else {
            return "XLM";
        }
    }

    @Override
    public boolean isBalanceNotZero() {
        if (coinData == null) return false;
        return (coinData.getXlmBalance() != null && coinData.getXlmBalance().notZero() &&
                coinData.getAssetBalance() != null && coinData.getAssetBalance().notZero());
    }

    @Override
    public boolean hasBalanceInfo() {
        if (coinData == null) return false;
        return (coinData.getXlmBalance() != null && coinData.getAssetBalance() != null) || (coinData.isError404());
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
        return Uri.parse("https://stellar.expert/explorer/public/account/" + ctx.getCoinData().getWallet());
    }

    @Override
    public Uri getShareWalletUri() {
        //TODO - how to construct payment query intent for stellar?
//        if (ctx.getCard().getDenomination() != null) {
//            return Uri.parse(ctx.getCoinData().getWallet() + "?amount=" + convertToAmount(convertToInternalAmount(ctx.getCard().getDenomination())).toValueString());
//        } else {
        return Uri.parse(ctx.getCoinData().getWallet());
//        }
    }

    @Override
    public InputFilter[] getAmountInputFilters() {
        return new InputFilter[]{new DecimalDigitsInputFilter(getDecimals())};
    }

    @Override
    public boolean checkNewTransactionAmount(Amount amount) {
        if (coinData == null) return false;

        Amount balance;
        if (!coinData.isAssetBalanceZero()) {
            balance = coinData.getAssetBalance();
        } else {
            balance = coinData.getXlmBalance();
        }

        if (amount.compareTo(balance) > 0) {
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

        if (!coinData.isAssetBalanceZero()) {
            if (amountValue.compareTo(coinData.getAssetBalance()) > 0 || feeValue.compareTo(coinData.getXlmBalance()) > 0)
                return false;
        } else {
            if (isIncludeFee && (amountValue.compareTo(coinData.getXlmBalance()) > 0 || amountValue.compareTo(feeValue) < 0))
                return false;
            if (!isIncludeFee && amountValue.add(feeValue).compareTo(coinData.getXlmBalance()) > 0)
                return false;
        }

        return true;
    }

    @Override
    public boolean validateBalance(BalanceValidator balanceValidator) {
        try {
            if (((ctx.getCard().getOfflineBalance() == null) && !ctx.getCoinData().isBalanceReceived()) || (!ctx.getCoinData().isBalanceReceived() && (ctx.getCard().getRemainingSignatures() != ctx.getCard().getMaxSignatures()))) {
                if (coinData.isError404()) {
                    balanceValidator.setScore(0);
                    balanceValidator.setFirstLine(R.string.balance_validator_first_line_no_account);
                    balanceValidator.setSecondLine(R.string.balance_validator_second_line_create_account_instruction);
                } else {
                    balanceValidator.setScore(0);
                    balanceValidator.setFirstLine(R.string.balance_validator_first_line_unknown_balance);
                    balanceValidator.setSecondLine(R.string.balance_validator_second_line_unverified_balance);
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
                balanceValidator.setFirstLine(R.string.balance_validator_first_line_verified_balance);
                balanceValidator.setSecondLine(R.string.balance_validator_second_line_confirmed_in_blockchain);
                if (coinData.getXlmBalance().isZero()) {
                    balanceValidator.setFirstLine(R.string.balance_validator_first_line_empty_wallet);
                    balanceValidator.setSecondLine(R.string.empty_string);
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

//            if ((ctx.getCard().getOfflineBalance() != null) && !coinData.isBalanceReceived() && ctx.getCard().getRemainingSignatures() == ctx.getCard().getMaxSignatures()) {
//                balanceValidator.setScore(80);
//                balanceValidator.setFirstLine(R.string.balance_validator_first_line_verified_offline);
//                balanceValidator.setSecondLine(R.string.balance_validator_second_line_internet_to_get_balance);
//            }

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
        return new XlmAssetData();
    }

    @Override
    public String getUnspentInputsDescription() {
        return "";
    }


    @Override
    public SignTask.TransactionToSign constructTransaction(Amount amountValue, Amount feeValue, boolean IncFee, String targetAddress) throws Exception {
        checkBlockchainDataExists();

        if (coinData.isAssetBalanceZero() && IncFee) {
            amountValue = new Amount(amountValue.subtract(feeValue), amountValue.getCurrency());
        }

        Operation operation;
        if (coinData.getAssetBalance() == null) {
            operation = new ChangeTrustOperation.Builder(Asset.createNonNativeAsset(ctx.getCard().getTokenSymbol(), KeyPair.fromAccountId(ctx.getCard().getContractAddress())), "900000000000.0000000").build();
        } else {
            if (!coinData.isAssetBalanceZero()) {
                operation = new PaymentOperation.Builder(KeyPair.fromAccountId(targetAddress), Asset.createNonNativeAsset(ctx.getCard().getTokenSymbol(), KeyPair.fromAccountId(ctx.getCard().getContractAddress())), amountValue.toValueString()).build();
            } else {
                if (coinData.isTargetAccountCreated())
                    operation = new PaymentOperation.Builder(KeyPair.fromAccountId(targetAddress), new AssetTypeNative(), amountValue.toValueString()).build();
                else
                    operation = new CreateAccountOperation.Builder(KeyPair.fromAccountId(targetAddress), amountValue.toValueString()).build();
            }
        }
        TransactionEx transaction = TransactionEx.buildEx(120, coinData.getAccountResponse(), operation);


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


    private void checkTargetAccountCreated(BlockchainRequestsCallbacks blockchainRequestsCallbacks, String targetAddress, Amount amount) {
        final ServerApiStellar serverApi = new ServerApiStellar(ctx.getBlockchain());

        ServerApiStellar.Listener listener = new ServerApiStellar.Listener() {
            @Override
            public void onSuccess(StellarRequest.Base request) {
                coinData.setTargetAccountCreated(true);
                blockchainRequestsCallbacks.onComplete(true);
            }


            @Override
            public void onFail(StellarRequest.Base request) {
                Log.i(TAG, "onFail: " + request.getClass().getSimpleName() + " " + request.getError());

                if (request.errorResponse != null && request.errorResponse.getCode() == 404) {
                    coinData.setTargetAccountCreated(false);

                    if (amount.compareTo(coinData.getReserve()) >= 0) { //TODO: take fee inclusion in account, now 1 XLM with fee included will fail after transaction is sent
                        blockchainRequestsCallbacks.onComplete(true);
                    } else {
                        ctx.setError(R.string.confirm_transaction_error_not_enough_xlm_for_create);
                        blockchainRequestsCallbacks.onComplete(false);
                    }
                } else { // suppose account is created if anything goes wrong
                    coinData.setTargetAccountCreated(true);
                    blockchainRequestsCallbacks.onComplete(true);
                }
            }
        };

        serverApi.setListener(listener);

        serverApi.requestData(ctx, new StellarRequest.Balance(targetAddress));
    }

    @Override
    public void requestBalanceAndUnspentTransactions(BlockchainRequestsCallbacks blockchainRequestsCallbacks) {
        final ServerApiStellar serverApi = new ServerApiStellar(ctx.getBlockchain());

        ServerApiStellar.Listener listener = new ServerApiStellar.Listener() {
            @Override
            public void onSuccess(StellarRequest.Base request) {
                Log.i(TAG, "onSuccess: " + request.getClass().getSimpleName());

                if (request instanceof StellarRequest.Balance) {
                    StellarRequest.Balance balanceRequest = (StellarRequest.Balance) request;

                    coinData.setAccountResponse(balanceRequest.accountResponse);
                    coinData.setValidationNodeDescription(serverApi.getCurrentURL());

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
                } else if (request instanceof StellarRequest.Operations) {
                    StellarRequest.Operations operationsRequest = (StellarRequest.Operations) request;

                    for (OperationResponse operationResponse : operationsRequest.operationsList) {
                        if (operationResponse.getSourceAccount().getAccountId().equals(coinData.getWallet())) {
                            coinData.incSentTransactionsCount();
                        }
                    }

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
        serverApi.requestData(ctx, new StellarRequest.Operations(coinData.getWallet()));
    }

    @Override
    public void requestFee(BlockchainRequestsCallbacks blockchainRequestsCallbacks, String targetAddress, Amount amount) throws Exception {
        // TODO: get fee stats?
        coinData.minFee = coinData.normalFee = coinData.maxFee = coinData.getBaseFee();
        checkTargetAccountCreated(blockchainRequestsCallbacks, targetAddress, amount);
    }

    @Override
    public void requestSendTransaction(BlockchainRequestsCallbacks blockchainRequestsCallbacks, byte[] txForSend) throws IOException {
        final ServerApiStellar serverApi = new ServerApiStellar(ctx.getBlockchain());

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

    public boolean allowSelectFeeInclusion() {
        return coinData.isAssetBalanceZero();
    }

    public int pendingTransactionTimeoutInSeconds() {
        return 10;
    }

}