package com.tangem.domain.wallet.xrp;

import android.net.Uri;
import android.text.InputFilter;
import android.util.Log;

import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.types.known.tx.signed.SignedTransaction;
import com.ripple.core.types.known.tx.txns.Payment;
import com.ripple.encodings.addresses.Addresses;
import com.tangem.card_common.data.TangemCard;
import com.tangem.card_common.reader.CardProtocol;
import com.tangem.card_common.tasks.SignTask;
import com.tangem.card_common.util.Util;
import com.tangem.data.network.ServerApiRipple;
import com.tangem.data.network.model.RippleResponse;
import com.tangem.domain.wallet.BTCUtils;
import com.tangem.domain.wallet.BalanceValidator;
import com.tangem.domain.wallet.CoinData;
import com.tangem.domain.wallet.CoinEngine;
import com.tangem.domain.wallet.TangemContext;
import com.tangem.util.CryptoUtil;
import com.tangem.util.DecimalDigitsInputFilter;
import com.tangem.wallet.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;

public class XrpEngine extends CoinEngine {

    private static final String TAG = XrpEngine.class.getSimpleName();

    public XrpData coinData = null;

    public XrpEngine(TangemContext context) throws Exception {
        super(context);
        if (context.getCoinData() == null) {
            coinData = new XrpData();
            context.setCoinData(coinData);
        } else if (context.getCoinData() instanceof XrpData) {
            coinData = (XrpData) context.getCoinData();
        } else {
            throw new Exception("Invalid type of Blockchain data for XrpEngine");
        }
    }

    public XrpEngine() {
        super();
    }

    private static int getDecimals() {
        return 6;
    }

    private void checkBlockchainDataExists() throws Exception {
        if (coinData == null) throw new Exception("No blockchain data");
    }

    @Override
    public boolean awaitingConfirmation() {
        if (coinData == null) return false;
        return coinData.hasUnconfirmed();
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
        return "XRP";
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
        return coinData.hasBalanceInfo();
    }

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
        return "XRP";
    }

    @Override
    public boolean validateAddress(String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }

        if (address.length() < 25) {
            return false;
        }

        if (address.length() > 35) {
            return false;
        }

        if (!address.startsWith("r")) {
            return false;
        }
        try {
            Addresses.decodeAccountID(address);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isNeedCheckNode() {
        return true;
    }

    @Override
    public Uri getWalletExplorerUri() {
        return Uri.parse("https://xrpscan.com/account/" + ctx.getCoinData().getWallet());
    }

    public Uri getShareWalletUri() {
        return Uri.parse(ctx.getCoinData().getWallet());
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

            if (coinData.hasUnconfirmed()) {
                balanceValidator.setScore(0);
                balanceValidator.setFirstLine("Transaction in progress");
                balanceValidator.setSecondLine("Wait for confirmation in blockchain");
                return false;
            }

            if (coinData.isBalanceReceived()) {// && coinData.isBalanceEqual()) { TODO:check
                balanceValidator.setScore(100);
                balanceValidator.setFirstLine("Verified balance");
                balanceValidator.setSecondLine("Balance confirmed in blockchain");
                if (coinData.getBalanceInInternalUnits().isZero()) {
                    balanceValidator.setFirstLine("Empty wallet");
                    balanceValidator.setSecondLine("");
                }
            }

            if ((ctx.getCard().getOfflineBalance() != null) && !coinData.isBalanceReceived() && (ctx.getCard().getRemainingSignatures() == ctx.getCard().getMaxSignatures()) && coinData.getBalanceInInternalUnits().notZero()) {
                balanceValidator.setScore(80);
                balanceValidator.setFirstLine("Verified offline balance");
                balanceValidator.setSecondLine("Can't obtain balance from blockchain. Restore internet connection to be more confident. ");
            }

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

    public String calculateAddress(byte[] pkCompressed) {
        byte[] canonisedPubKey = canonisePubKey(pkCompressed);
        byte[] accountId = CryptoUtil.sha256ripemd160(canonisedPubKey);
        String address = Addresses.encodeAccountID(accountId);

        boolean valid = validateAddress(address);

        return address;
    }

    @Override
    public Amount convertToAmount(InternalAmount internalAmount) {
        BigDecimal d = internalAmount.divide(new BigDecimal("1000000"));
        return new Amount(d, getBalanceCurrency());
    }

    @Override
    public Amount convertToAmount(String strAmount, String currency) {
        return new Amount(strAmount, currency);
    }

    @Override
    public InternalAmount convertToInternalAmount(Amount amount) {
        BigDecimal d = amount.multiply(new BigDecimal("1000000"));
        return new InternalAmount(d, "Drops");
    }

    @Override
    public InternalAmount convertToInternalAmount(byte[] bytes) {
        if (bytes == null) return null;
        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) reversed[i] = bytes[bytes.length - i - 1];
        return new InternalAmount(Util.byteArrayToLong(reversed), "Drops");
    }

    @Override
    public byte[] convertToByteArray(InternalAmount internalAmount) {
        byte[] bytes = Util.longToByteArray(internalAmount.longValueExact());
//        byte[] reversed = new byte[bytes.length]; TODO: check if needed
//        for (int i = 0; i < bytes.length; i++) reversed[i] = bytes[bytes.length - i - 1];
//        return reversed;
        return bytes;
    }

    @Override
    public CoinData createCoinData() {
        return new XrpData();
    }

    @Override
    public String getUnspentInputsDescription() {
        return "";
    }

    @Override
    public void defineWallet() throws CardProtocol.TangemException {
        try {
            String wallet = calculateAddress(ctx.getCard().getWalletPublicKeyRar());
            ctx.getCoinData().setWallet(wallet);
        } catch (Exception e) {
            ctx.getCoinData().setWallet("ERROR");
            throw new CardProtocol.TangemException("Can't define wallet address");
        }
    }

    public byte[] canonisePubKey(byte[] pkCompressed) {
        byte[] canonicalPubKey = new byte[33];

        if (pkCompressed.length == 32) {
            canonicalPubKey[0] = (byte) 0xED;
            System.arraycopy(pkCompressed, 0, canonicalPubKey, 1, 32);
        } else {
            canonicalPubKey = pkCompressed;
        }
        return canonicalPubKey;
    }

    @Override
    public SignTask.TransactionToSign constructTransaction(Amount amountValue, Amount feeValue, boolean IncFee, String targetAddress) throws Exception {
        checkBlockchainDataExists();

        Payment payment = new Payment();

        // Put `as` AccountID field Account, `Object` o
        payment.as(AccountID.Account, coinData.getWallet());
        payment.as(AccountID.Destination, targetAddress);
        payment.as(com.ripple.core.coretypes.Amount.Amount, amountValue);
        payment.as(UInt32.Sequence, coinData.getSequence());
        payment.as(com.ripple.core.coretypes.Amount.Fee, feeValue);

        SignedTransaction signedTx = payment.prepare(canonisePubKey(ctx.getCard().getWalletPublicKeyRar()));

        return new SignTask.TransactionToSign() {

            @Override
            public boolean isSigningMethodSupported(TangemCard.SigningMethod signingMethod) {
                return signingMethod == TangemCard.SigningMethod.Sign_Hash;
            }

            @Override
            public byte[][] getHashesToSign() {
                byte[][] dataForSign = new byte[1][];
                dataForSign[0] = signedTx.signingData;
                return dataForSign;
            }

            @Override
            public byte[] getRawDataToSign() throws Exception {
                throw new Exception("Signing of raw transaction not supported for " + this.getClass().getSimpleName());
            }

            @Override
            public String getHashAlgToSign() throws Exception {
                throw new Exception("Signing of raw transaction not supported for " + this.getClass().getSimpleName());
            }

            @Override
            public byte[] getIssuerTransactionSignature(byte[] dataToSignByIssuer) throws Exception {
                throw new Exception("Transaction validation by issuer not supported in this version");
            }

            @Override
            public byte[] onSignCompleted(byte[] signFromCard) {
                signedTx.addSign(signFromCard);
                return BTCUtils.fromHex(signedTx.tx_blob);
            }
        };
    }

    public void requestBalanceAndUnspentTransactions(BlockchainRequestsCallbacks blockchainRequestsCallbacks) {
        final ServerApiRipple serverApiRipple = new ServerApiRipple();

        ServerApiRipple.ResponseListener rippleListener = new ServerApiRipple.ResponseListener() {
            @Override
            public void onSuccess(String method, RippleResponse rippleResponse) {
                Log.i(TAG, "onSuccess: " + method);
                switch (method) {
                    case ServerApiRipple.RIPPLE_ACCOUNT_INFO: {
                        try {
                            String walletAddress = rippleResponse.getResult().getAccount_data().getAccount();
                            if (!walletAddress.equals(coinData.getWallet())) {
                                throw new Exception("Invalid wallet address in answer!");
                            }

                            coinData.setBalanceConfirmed(Long.parseLong(rippleResponse.getResult().getAccount_data().getBalance()));
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, "FAIL RIPPLE_ACCOUNT_INFO Exception");
                        }
                    }
                    break;

                    case ServerApiRipple.RIPPLE_ACCOUNT_UNCONFIRMED: {
                        try {
                            String walletAddress = rippleResponse.getResult().getAccount_data().getAccount();
                            if (!walletAddress.equals(coinData.getWallet())) {
                                throw new Exception("Invalid wallet address in answer!");
                            }

                            coinData.setBalanceReceived(true);
                            coinData.setBalanceUnconfirmed(Long.parseLong(rippleResponse.getResult().getAccount_data().getBalance()));
                            coinData.setSequence(rippleResponse.getResult().getAccount_data().getSequence());
                            coinData.setValidationNodeDescription(ServerApiRipple.lastNode);

//                    //check pending
//                    if (App.pendingTransactionsStorage.hasTransactions(ctx.getCard())) {
//                        for (PendingTransactionsStorage.TransactionInfo pendingTx : App.pendingTransactionsStorage.getTransactions(ctx.getCard()).getTransactions()) {
//                            String pendingId = CalculateTxHash(pendingTx.getTx());
//                            int x = 0;
//                            for (TxData walletTx : adaliteResponse.getRight().getCaTxList()) {
//                                if (walletTx.getCtbId().equals(pendingId)) {
//                                    App.pendingTransactionsStorage.removeTransaction(ctx.getCard(), pendingTx.getTx());
//                                }
//                            }
//                        }
//                    }

                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, "FAIL RIPPLE_ACCOUNT_INFO Exception");
                        }
                    }
                    break;
                }

                if (serverApiRipple.isRequestsSequenceCompleted()) {
                    blockchainRequestsCallbacks.onComplete(!ctx.hasError());
                } else {
                    blockchainRequestsCallbacks.onProgress();
                }
            }

            @Override
            public void onFail(String method, String message) {
                Log.i(TAG, "onFail: " + method + " " + message);
                ctx.setError(message);
                if (serverApiRipple.isRequestsSequenceCompleted()) {
                    blockchainRequestsCallbacks.onComplete(false);
                } else {
                    blockchainRequestsCallbacks.onProgress();
                }
            }
        };

        serverApiRipple.setResponseListener(rippleListener);

        serverApiRipple.requestData(ServerApiRipple.RIPPLE_ACCOUNT_INFO, coinData.getWallet(), "");
        serverApiRipple.requestData(ServerApiRipple.RIPPLE_ACCOUNT_UNCONFIRMED, coinData.getWallet(), "");
    }

    @Override
    public void requestFee(BlockchainRequestsCallbacks blockchainRequestsCallbacks, String targetAddress, Amount amount) {
        final ServerApiRipple serverApiRipple = new ServerApiRipple();

        ServerApiRipple.ResponseListener rippleListener = new ServerApiRipple.ResponseListener() {
            @Override
            public void onSuccess(String method, RippleResponse rippleResponse) {
                BigDecimal minFee = new BigDecimal(rippleResponse.getResult().getDrops().getMinimum_fee()).divide(new BigDecimal(getDecimals()));
                BigDecimal normalFee = new BigDecimal(rippleResponse.getResult().getDrops().getOpen_ledger_fee()).divide(new BigDecimal(getDecimals()));
                BigDecimal maxFee = new BigDecimal(rippleResponse.getResult().getDrops().getMedian_fee()).divide(new BigDecimal(getDecimals()));

                coinData.minFee = new Amount(minFee.setScale(getDecimals(), RoundingMode.UP), getFeeCurrency());
                coinData.normalFee = new Amount(normalFee.setScale(getDecimals(), RoundingMode.UP), getFeeCurrency());
                coinData.maxFee = new Amount(maxFee.setScale(getDecimals(), RoundingMode.UP), getFeeCurrency());

                blockchainRequestsCallbacks.onComplete(true);
            }

            @Override
            public void onFail(String method, String message) {
                Log.i(TAG, "onFail: " + method + " " + message);
                ctx.setError(message);
                blockchainRequestsCallbacks.onComplete(false);
            }
        };

        serverApiRipple.setResponseListener(rippleListener);

        serverApiRipple.requestData(ServerApiRipple.RIPPLE_FEE, "", "");
    }

    @Override
    public void requestSendTransaction(BlockchainRequestsCallbacks blockchainRequestsCallbacks, byte[] txForSend) {
        final String  txStr = BTCUtils.toHex(txForSend);

        final ServerApiRipple serverApiRipple = new ServerApiRipple();

        final ServerApiRipple.ResponseListener responseListener = new ServerApiRipple.ResponseListener() {
            @Override
            public void onSuccess(String method, RippleResponse rippleResponse) {
                try {
                    if (rippleResponse.getResult().getEngine_result_code() != null) {
                        if (rippleResponse.getResult().getEngine_result_code() == 0) {
                            ctx.setError(null);
                            blockchainRequestsCallbacks.onComplete(true);
                        } else {
                            ctx.setError(rippleResponse.getResult().getEngine_result_message());
                            blockchainRequestsCallbacks.onComplete(false);
                        }
                    } else {
                        ctx.setError(rippleResponse.getResult().getError() + " - " + rippleResponse.getResult().getError_exception());
                        blockchainRequestsCallbacks.onComplete(false);
                    }
                } catch (Exception e) {
                    if (e.getMessage() != null) {
                        ctx.setError(e.getMessage());
                        blockchainRequestsCallbacks.onComplete(false);
                    } else {
                        ctx.setError(e.getClass().getName());
                        blockchainRequestsCallbacks.onComplete(false);
                        Log.e(TAG, rippleResponse.toString());
                    }
                }
            }

            @Override
            public void onFail(String method, String message) {
                ctx.setError(message);
                blockchainRequestsCallbacks.onComplete(false);
            }
        };
        serverApiRipple.setResponseListener(responseListener);

        serverApiRipple.requestData(ServerApiRipple.RIPPLE_SUBMIT, "", txStr);
    }

}
