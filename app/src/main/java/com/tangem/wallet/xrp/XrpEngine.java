package com.tangem.wallet.xrp;

import android.net.Uri;
import android.text.InputFilter;
import android.util.Log;

import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.crypto.ecdsa.ECDSASignature;
import com.ripple.encodings.addresses.Addresses;
import com.ripple.utils.HashUtils;
import com.tangem.App;
import com.tangem.data.network.ServerApiRipple;
import com.tangem.data.network.model.RippleResponse;
import com.tangem.tangem_card.data.TangemCard;
import com.tangem.tangem_card.reader.CardProtocol;
import com.tangem.tangem_card.tasks.SignTask;
import com.tangem.tangem_card.util.Util;
import com.tangem.util.CryptoUtil;
import com.tangem.util.DecimalDigitsInputFilter;
import com.tangem.wallet.BTCUtils;
import com.tangem.wallet.BalanceValidator;
import com.tangem.wallet.CoinData;
import com.tangem.wallet.CoinEngine;
import com.tangem.wallet.R;
import com.tangem.wallet.TangemContext;

import java.math.BigDecimal;
import java.math.BigInteger;
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
        return coinData.hasUnconfirmed() || App.pendingTransactionsStorage.hasTransactions(ctx.getCard());
    }

    @Override
    public String getBalanceHTML() {
        Amount balance = getBalance();
        if (balance != null) {
            return " " + balance.toDescriptionString(getDecimals()) + "<br><small><small>+ " + convertToAmount(coinData.getReserveInInternalUnits()).toDescriptionString(getDecimals()) + " reserve</small></small>";
        } else {
            return "";
        }
    }

    @Override
    public String getBalanceCurrency() {
        return "XRP";
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
        return coinData.hasBalanceInfo() || coinData.isAccountNotFound();
    }

    public boolean isExtractPossible() {
        if (coinData == null || !coinData.hasBalanceInfo()) {
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
        return Uri.parse("ripple:" + ctx.getCoinData().getWallet());
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
            if (coinData.isAccountNotFound()) {
                balanceValidator.setScore(0);
                balanceValidator.setFirstLine(R.string.balance_validator_first_line_account_not_found);
                balanceValidator.setSecondLine(R.string.balance_validator_second_line_create_account_xrp);
                return false;
            }

            if (((ctx.getCard().getOfflineBalance() == null) && !ctx.getCoinData().isBalanceReceived()) || (!ctx.getCoinData().isBalanceReceived() && (ctx.getCard().getRemainingSignatures() != ctx.getCard().getMaxSignatures()))) {
                balanceValidator.setScore(0);
                balanceValidator.setFirstLine(R.string.balance_validator_first_line_unknown_balance);
                balanceValidator.setSecondLine(R.string.balance_validator_second_line_unverified_balance);
                return false;
            }

            if (coinData.hasUnconfirmed()) {
                balanceValidator.setScore(0);
                balanceValidator.setFirstLine(R.string.balance_validator_first_line_transaction_in_progress);
                balanceValidator.setSecondLine(R.string.balance_validator_second_line_wait_for_confirmation);
                return false;
            }

            if (coinData.isBalanceReceived()) {
                balanceValidator.setScore(100);
                balanceValidator.setFirstLine(R.string.balance_validator_first_line_verified_balance);
                balanceValidator.setSecondLine(R.string.balance_validator_second_line_confirmed_in_blockchain);
                if (coinData.getBalanceInInternalUnits().isZero()) {
                    balanceValidator.setFirstLine(R.string.balance_validator_first_line_empty_wallet);
                    balanceValidator.setSecondLine(R.string.empty_string);
                }
            }

//            if ((ctx.getCard().getOfflineBalance() != null) && !coinData.isBalanceReceived() && ctx.getCard().getRemainingSignatures() == ctx.getCard().getMaxSignatures()) {
//                balanceValidator.setScore(80);
//                balanceValidator.setFirstLine(R.string.balance_validator_first_line_verified_offline);
//                balanceValidator.setSecondLine(R.string.balance_validator_second_line_internet_to_get_balance);
//            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Amount getBalance() {
        if (coinData == null || !coinData.hasBalanceInfo()) return null;
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

    public String calculateAddress(byte[] pkCompressed) throws Exception {
        byte[] canonisedPubKey = canonisePubKey(pkCompressed);
        byte[] accountId = CryptoUtil.sha256ripemd160(canonisedPubKey);

        return Addresses.encodeAccountID(accountId);
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

    public byte[] canonisePubKey(byte[] pkCompressed) throws Exception {
        byte[] canonicalPubKey = new byte[33];

        if (pkCompressed.length == 32) {
            canonicalPubKey[0] = (byte) 0xED;
            System.arraycopy(pkCompressed, 0, canonicalPubKey, 1, 32);
        } else if (pkCompressed.length == 33)
            canonicalPubKey = pkCompressed;
        else
            throw new Exception("Invalid pubkey length");
        return canonicalPubKey;
    }

    @Override
    public SignTask.TransactionToSign constructTransaction(Amount amountValue, Amount feeValue, boolean IncFee, String targetAddress) throws Exception {
        checkBlockchainDataExists();

        if (!coinData.isTargetAccountCreated() && amountValue.compareTo(BigDecimal.valueOf(20)) < 0) {
            throw new Exception("Target account is not created. Amount should be 20 XRP or more");
        }

        String amount, fee;

        if (IncFee) {
            amount = convertToInternalAmount(amountValue).subtract(convertToInternalAmount(feeValue)).setScale(0).toPlainString();
        } else {
            amount = Long.toString(convertToInternalAmount(amountValue).longValueExact());
        }

        fee = Long.toString(convertToInternalAmount(feeValue).longValueExact());

        XrpPayment payment = new XrpPayment();

        // Put `as` AccountID field Account, `Object` o
        payment.as(AccountID.Account, coinData.getWallet());
        payment.as(AccountID.Destination, targetAddress);
        payment.as(com.ripple.core.coretypes.Amount.Amount, amount);
        payment.as(UInt32.Sequence, coinData.getSequence());
        payment.as(com.ripple.core.coretypes.Amount.Fee, fee);

        XrpSignedTransaction signedTx = payment.prepare(canonisePubKey(ctx.getCard().getWalletPublicKeyRar()));

        return new SignTask.TransactionToSign() {

            @Override
            public boolean isSigningMethodSupported(TangemCard.SigningMethod signingMethod) {
                return signingMethod == TangemCard.SigningMethod.Sign_Hash;
            }

            @Override
            public byte[][] getHashesToSign() throws Exception {
                byte[][] dataForSign = new byte[1][];
                if (ctx.getCard().getWalletPublicKeyRar().length == 33)
                    dataForSign[0] = HashUtils.halfSha512(signedTx.signingData);
                else if (ctx.getCard().getWalletPublicKeyRar().length == 32)
                    dataForSign[0] = signedTx.signingData;
                else
                    throw new Exception("Invalid pubkey length");
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
            public byte[] onSignCompleted(byte[] signFromCard) throws Exception {
                if (ctx.getCard().getWalletPublicKeyRar().length == 33) {
                    int size = signFromCard.length / 2;
                    BigInteger r = new BigInteger(1, Arrays.copyOfRange(signFromCard, 0, size));
                    BigInteger s = new BigInteger(1, Arrays.copyOfRange(signFromCard, size, size * 2));
                    s = CryptoUtil.toCanonicalised(s);
                    ECDSASignature sig = new ECDSASignature(r, s);
                    byte[] sigDer = sig.encodeToDER();
                    if (!ECDSASignature.isStrictlyCanonical(sigDer)) {
                        throw new IllegalStateException("Signature is not strictly canonical");
                    }
                    signedTx.addSign(sigDer);
                } else if (ctx.getCard().getWalletPublicKeyRar().length == 32)
                    signedTx.addSign(signFromCard);
                else
                    throw new Exception("Invalid pubkey length");
                byte[] txForSend = BTCUtils.fromHex(signedTx.tx_blob);
                notifyOnNeedSendTransaction(txForSend);
                return txForSend;
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
                            if (rippleResponse.getResult().getAccount_data() != null) {
                                String walletAddress = rippleResponse.getResult().getAccount_data().getAccount();
                                if (!walletAddress.equals(coinData.getWallet())) {
                                    throw new Exception("Invalid wallet address in answer!");
                                }
                                coinData.setBalanceConfirmed(Long.parseLong(rippleResponse.getResult().getAccount_data().getBalance()));
                            } else if (rippleResponse.getResult().getError_code().equals(19)) // "Account not found"
                                coinData.setAccountNotFound(true);
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
                            coinData.setValidationNodeDescription(serverApiRipple.getCurrentURL());

//                    //check pending
//                    if (App.pendingTransactionsStorage.hasTransactions(ctx.getCard())) {
//                        for (PendingTransactionsStorage.TransactionInfo pendingTx : App.pendingTransactionsStorage.getTransactions(ctx.getCard()).getTransactions()) {
//                            String pendingId = CalculateTxHash(pendingTx.getTx());
//                            int x = 0;
//                            for (AdaliteTxData walletTx : adaliteResponse.getRight().getCaTxList()) {
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

                    case ServerApiRipple.RIPPLE_SERVER_STATE: {
                        try {
                            coinData.setReserve(rippleResponse.getResult().getState().getValidated_ledger().getReserve_base());
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
                Log.e(TAG, "onFail: " + method + " " + message);
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
        serverApiRipple.requestData(ServerApiRipple.RIPPLE_SERVER_STATE, "", "");
    }

    @Override
    public void requestFee(BlockchainRequestsCallbacks blockchainRequestsCallbacks, String targetAddress, Amount amount) {
        final ServerApiRipple serverApiRipple = new ServerApiRipple();

        ServerApiRipple.ResponseListener rippleListener = new ServerApiRipple.ResponseListener() {
            @Override
            public void onSuccess(String method, RippleResponse rippleResponse) {
                Log.i(TAG, "onSuccess: " + method);
                switch (method) {
                    case ServerApiRipple.RIPPLE_ACCOUNT_INFO: {
                        try {
                            if (rippleResponse.getResult().getError_code().equals(19)) { // "Account not found"
                                coinData.setTargetAccountCreated(false);
                            } else {
                                coinData.setTargetAccountCreated(true);
                            }
                        } catch (Exception e) {
                            coinData.setTargetAccountCreated(true); //expected behaviour, if account exists, there should be no error code -> null pointer
                        }

                        if (serverApiRipple.isRequestsSequenceCompleted()) {
                            blockchainRequestsCallbacks.onComplete(!ctx.hasError());
                        } else {
                            blockchainRequestsCallbacks.onProgress();
                        }
                    }
                    break;

                    case ServerApiRipple.RIPPLE_FEE: {
                        try {
                            InternalAmount minFee = new InternalAmount(Long.valueOf(rippleResponse.getResult().getDrops().getMinimum_fee()), "Drops");
                            InternalAmount normalFee = new InternalAmount(Long.valueOf(rippleResponse.getResult().getDrops().getOpen_ledger_fee()), "Drops");
                            InternalAmount maxFee = new InternalAmount(Long.valueOf(rippleResponse.getResult().getDrops().getMedian_fee()), "Drops");

                            coinData.minFee = convertToAmount(minFee);
                            coinData.normalFee = convertToAmount(normalFee);
                            coinData.maxFee = convertToAmount(maxFee);

                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, "FAIL RIPPLE_FEE Exception");
                            ctx.setError(e.getMessage());
                        }

                        if (serverApiRipple.isRequestsSequenceCompleted()) {
                            blockchainRequestsCallbacks.onComplete(!ctx.hasError());
                        } else {
                            blockchainRequestsCallbacks.onProgress();
                        }
                    }
                    break;
                }
            }

            @Override
            public void onFail(String method, String message) {
                Log.i(TAG, "onFail: " + method + " " + message);
                ctx.setError(message);
                blockchainRequestsCallbacks.onComplete(false);
            }
        };

        serverApiRipple.setResponseListener(rippleListener);

        serverApiRipple.requestData(ServerApiRipple.RIPPLE_ACCOUNT_INFO, targetAddress, "");
        serverApiRipple.requestData(ServerApiRipple.RIPPLE_FEE, "", "");
    }

    @Override
    public void requestSendTransaction(BlockchainRequestsCallbacks blockchainRequestsCallbacks, byte[] txForSend) {
        final String txStr = BTCUtils.toHex(txForSend);

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

    @Override
    public int pendingTransactionTimeoutInSeconds() {
        return 10;
    }

    public boolean needMultipleLinesForBalance() {
        return true;
    }

}
