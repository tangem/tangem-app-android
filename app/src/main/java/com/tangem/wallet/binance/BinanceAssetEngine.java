package com.tangem.wallet.binance;

import android.net.Uri;
import android.text.InputFilter;
import android.util.Log;

import androidx.annotation.NonNull;

import com.tangem.App;
import com.tangem.data.Blockchain;
import com.tangem.data.network.BinanceApi;
import com.tangem.data.network.Server;
import com.tangem.data.network.ServerApiBinance;
import com.tangem.data.network.model.BinanceFees;
import com.tangem.tangem_card.data.TangemCard;
import com.tangem.tangem_card.reader.CardProtocol;
import com.tangem.tangem_card.tasks.SignTask;
import com.tangem.tangem_card.util.Util;
import com.tangem.util.CryptoUtil;
import com.tangem.util.DecimalDigitsInputFilter;
import com.tangem.wallet.BalanceValidator;
import com.tangem.wallet.CoinData;
import com.tangem.wallet.CoinEngine;
import com.tangem.wallet.R;
import com.tangem.wallet.TangemContext;
import com.tangem.wallet.binance.client.BinanceDexApiClientFactory;
import com.tangem.wallet.binance.client.BinanceDexApiRestClient;
import com.tangem.wallet.binance.client.BinanceDexEnvironment;
import com.tangem.wallet.binance.client.domain.broadcast.TransactionOption;
import com.tangem.wallet.binance.client.domain.broadcast.Transfer;
import com.tangem.wallet.binance.client.encoding.Bech32;
import com.tangem.wallet.binance.client.encoding.Crypto;
import com.tangem.wallet.binance.client.encoding.message.MessageType;
import com.tangem.wallet.binance.client.encoding.message.TransactionRequestAssemblerExtSign;
import com.tangem.wallet.binance.client.encoding.message.TransferMessage;

import org.bitcoinj.core.Utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class BinanceAssetEngine extends CoinEngine {
    private static final String TAG = BinanceAssetEngine.class.getSimpleName();

    public BinanceAssetData coinData = null;
    private BinanceDexApiRestClient client = null;

    public BinanceAssetEngine(TangemContext context) throws Exception {
        super(context);
        if (context.getCoinData() == null) {
            coinData = new BinanceAssetData();
            context.setCoinData(coinData);
        } else if (context.getCoinData() instanceof BinanceAssetData) {
            coinData = (BinanceAssetData) context.getCoinData();
        } else {
            throw new Exception("Invalid type of Blockchain data for " + TAG);
        }
        client = BinanceDexApiClientFactory.newInstance().newRestClient(BinanceDexEnvironment.PROD.getBaseUrl());
        coinData.setChainId("Binance-Chain-Tigris");
    }

    public BinanceAssetEngine() {
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
        return App.pendingTransactionsStorage.hasTransactions(ctx.getCard());
    }

    @Override
    public String getBalanceHTML() {
        Amount balance = coinData.getBalance();
        Amount assetBalance = coinData.getAssetBalance();
        if (balance != null) {
            if (assetBalance != null) {
                return assetBalance.toDescriptionString(getDecimals()) + "<br><small><small>+ " + balance.toDescriptionString(getDecimals()) + " for fee</small></small>";
            }
            return balance.toDescriptionString(getDecimals());
        } else {
            return "";
        }
    }

    @Override
    public String getBalanceCurrency() {
        if (hasBalanceInfo()) {
            if (coinData.getAssetBalance() != null) {
                return ctx.getCard().tokenSymbol;
            } else {
                return getFeeCurrency();
            }
        } else {
            return getFeeCurrency();
        }
    }

    @Override
    public boolean isBalanceNotZero() {
        if (!hasBalanceInfo())
            return false;
        return (coinData.getBalance() != null && coinData.getBalance().notZero()) ||
                (coinData.getAssetBalance() != null && coinData.getAssetBalance().notZero());
    }

    @Override
    public boolean hasBalanceInfo() {
        if (coinData == null) return false;
        return coinData.hasBalanceInfo() || coinData.isError404();
    }

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
        return "BNB";
    }

    @Override
    public boolean validateAddress(String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }

        try {
            Crypto.decodeAddress(address);
        } catch (Exception e) {
            return false;
        }

        if (ctx.getBlockchain() == Blockchain.Binance && !address.startsWith("bnb1")) {
            return false;
        }

        if (ctx.getBlockchain() == Blockchain.BinanceTestNet && !address.startsWith("tbnb1")) {
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
        if (ctx.getBlockchain() == Blockchain.Binance) {
            return Uri.parse("https://explorer.binance.org/address/" + ctx.getCoinData().getWallet());
        } else if (ctx.getBlockchain() == Blockchain.BinanceTestNet) {
            return Uri.parse("https://testnet-explorer.binance.org/address/" + ctx.getCoinData().getWallet());
        } else {
            Log.e(TAG, "Invalid blockchain for BinanceEngine");
            return Uri.parse("https://explorer.binance.org/address/" + ctx.getCoinData().getWallet());
        }
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
        if (!hasBalanceInfo()) return false;
        Amount balance;
        try {
            if (amount.getCurrency().equals(ctx.getCard().tokenSymbol)) {
                balance = coinData.getAssetBalance();
            } else if (amount.getCurrency().equals(getFeeCurrency())) {
                balance = coinData.getBalance();
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return amount.compareTo(balance) <= 0;
    }

    @Override
    public boolean checkNewTransactionAmountAndFee(Amount amount, Amount fee, Boolean isFeeIncluded) {
        if (!hasBalanceInfo()) return false;

        try {
            Amount balance = coinData.getBalance();

            if (fee == null || amount == null || fee.isZero() || amount.isZero())
                return false;


            if (amount.getCurrency().equals(ctx.getCard().tokenSymbol)) {
                // token transaction
                if (fee.compareTo(balance) > 0)
                    return false;
            } else if (amount.getCurrency().equals(getFeeCurrency())) {
                // standard ETH transaction
                if (isFeeIncluded && (amount.compareTo(balance) > 0 || fee.compareTo(balance) > 0))
                    return false;

                if (!isFeeIncluded && amount.add(fee).compareTo(balance) > 0)
                    return false;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
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
                    balanceValidator.setSecondLine(R.string.balance_validator_second_line_create_account);
                } else {
                    balanceValidator.setScore(0);
                    balanceValidator.setFirstLine(R.string.balance_validator_first_line_unknown_balance);
                    balanceValidator.setSecondLine(R.string.balance_validator_second_line_unverified_balance);
                    return false;
                }
            }

            if (coinData.isBalanceReceived()) {
                balanceValidator.setScore(100);
                balanceValidator.setFirstLine(R.string.balance_validator_first_line_verified_balance);
                balanceValidator.setSecondLine(R.string.balance_validator_second_line_confirmed_in_blockchain);
                if (coinData.getBalance().isZero() && (coinData.getAssetBalance() == null || coinData.getAssetBalance().isZero())) {
                    balanceValidator.setFirstLine(R.string.balance_validator_first_line_empty_wallet);
                    balanceValidator.setSecondLine(R.string.empty_string);
                }
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
        if (coinData.getAssetBalance() != null) {
            return coinData.getAssetBalance();
        } else {
            return coinData.getBalance();
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
        if (!hasBalanceInfo()) {
            return "";
        }
        try {
            if (coinData.getAssetBalance().notZero()) {
                return "";
            } else {
                return coinData.getBalance().toEquivalentString(coinData.getRateAlter());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public String calculateAddress(byte[] pkCompressed) throws Exception {
        byte[] pubKeyHash = Utils.sha256hash160(pkCompressed);
        return Bech32.encode("bnb", Crypto.convertBits(pubKeyHash, 0, pubKeyHash.length, 8, 5, false));
    }

    @Override
    public Amount convertToAmount(InternalAmount internalAmount) {
        return new Amount(internalAmount, getBalanceCurrency());
    }

    @Override
    public Amount convertToAmount(String strAmount, String currency) {
        return new Amount(strAmount, currency);
    }

    @Override
    public InternalAmount convertToInternalAmount(Amount amount) {
        return new InternalAmount(amount, getBalanceCurrency());
    }

    @Override
    public InternalAmount convertToInternalAmount(byte[] bytes) {
        if (bytes == null) return null;
        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) reversed[i] = bytes[bytes.length - i - 1];
        return new InternalAmount(Util.byteArrayToLong(reversed), getBalanceCurrency());
    }

    @Override
    public byte[] convertToByteArray(InternalAmount internalAmount) {
        return Util.longToByteArray(internalAmount.longValueExact());
    }

    @Override
    public CoinData createCoinData() {
        return new BinanceAssetData();
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
            coinData.setAssetSymbol(ctx.getCard().tokenSymbol);
        } catch (Exception e) {
            ctx.getCoinData().setWallet("ERROR");
            throw new CardProtocol.TangemException("Can't define wallet address");
        }
    }

    @Override
    public SignTask.TransactionToSign constructTransaction(Amount amountValue, Amount feeValue, boolean IncFee, String targetAddress) throws Exception {
        checkBlockchainDataExists();

        String amount;

        if (IncFee && amountValue.getCurrency().equals(getFeeCurrency())) { //Coin transfer only
            amount = amountValue.subtract(feeValue).setScale(getDecimals(), RoundingMode.DOWN).toPlainString();
        } else {
            amount = amountValue.setScale(getDecimals(), RoundingMode.DOWN).toPlainString();
        }

        byte[] pubKey = ctx.getCard().getWalletPublicKeyRar();
        byte[] pubKeyPrefix = MessageType.PubKey.getTypePrefixBytes();
        byte[] pubKeyForSign = new byte[pubKey.length + pubKeyPrefix.length + 1];
        System.arraycopy(pubKeyPrefix, 0, pubKeyForSign, 0, pubKeyPrefix.length);
        pubKeyForSign[pubKeyPrefix.length] = (byte) 33;
        System.arraycopy(pubKey, 0, pubKeyForSign, pubKeyPrefix.length + 1, pubKey.length);

        Transfer transfer = new Transfer();
        transfer.setCoin(amountValue.getCurrency().equals(getFeeCurrency()) ? getFeeCurrency() : ctx.getCard().getContractAddress());
        transfer.setFromAddress(ctx.getCoinData().getWallet());
        transfer.setToAddress(targetAddress);
        transfer.setAmount(amount);

        TransactionOption options = TransactionOption.DEFAULT_INSTANCE;

        TransactionRequestAssemblerExtSign txAssembler = client.prepareTransfer(transfer, coinData, pubKeyForSign, options, true);
        // TransactionRequestAssembler.buildTransfer as reference
        TransferMessage msgBean = txAssembler.createTransferMessage(transfer);
        byte[] msg = txAssembler.encodeTransferMessage(msgBean);
        byte[] dataForSign = txAssembler.prepareForSign(msgBean);

        return new SignTask.TransactionToSign() {

            @Override
            public boolean isSigningMethodSupported(TangemCard.SigningMethod signingMethod) {
                return signingMethod == TangemCard.SigningMethod.Sign_Hash || signingMethod == TangemCard.SigningMethod.Sign_Raw;
            }

            @Override
            public byte[][] getHashesToSign() throws NoSuchAlgorithmException {
                byte[][] hashForSign = new byte[1][];
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                hashForSign[0] = digest.digest(dataForSign);
                return hashForSign;
            }

            @Override
            public byte[] getRawDataToSign() {
                return dataForSign;
            }

            @Override
            public String getHashAlgToSign() {
                return "sha-256";
            }

            @Override
            public byte[] getIssuerTransactionSignature(byte[] dataToSignByIssuer) throws Exception {
                throw new Exception("Transaction validation by issuer not supported in this version");
            }

            @Override
            public byte[] onSignCompleted(byte[] signFromCard) throws Exception {
                int size = signFromCard.length / 2;
                BigInteger r = new BigInteger(1, Arrays.copyOfRange(signFromCard, 0, size));
                BigInteger s = new BigInteger(1, Arrays.copyOfRange(signFromCard, size, size * 2));
                s = CryptoUtil.toCanonicalised(s);

                byte[] resultSig = new byte[64];
                System.arraycopy(Utils.bigIntegerToBytes(r, 32), 0, resultSig, 0, 32);
                System.arraycopy(Utils.bigIntegerToBytes(s, 32), 0, resultSig, 32, 32);

                // TransactionRequestAssembler.buildTransfer as reference
                byte[] signature = txAssembler.encodeSignature(resultSig);
                byte[] txForSend = txAssembler.encodeStdTx(msg, signature);

                notifyOnNeedSendTransaction(txForSend);
                return txForSend;
            }
        };
    }

    public void requestBalanceAndUnspentTransactions(BlockchainRequestsCallbacks blockchainRequestsCallbacks) {
        try {
            ServerApiBinance serverApiBinance = new ServerApiBinance();

            ServerApiBinance.ResponseListener responseListener = new ServerApiBinance.ResponseListener() {
                @Override
                public void onSuccess() {
                    blockchainRequestsCallbacks.onComplete(true);
                }

                @Override
                public void onFail() {
                    blockchainRequestsCallbacks.onComplete(false);
                }
            };

            serverApiBinance.setResponseListener(responseListener);
            serverApiBinance.getBalance(ctx, client);

            coinData.setValidationNodeDescription(Server.ApiBinance.URL_BINANCE);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "FAIL Binance balance exception");
            ctx.setError(e.getMessage());
            blockchainRequestsCallbacks.onComplete(false);
        }
    }

    public void requestFee(BlockchainRequestsCallbacks blockchainRequestsCallbacks, String targetAddress, Amount amount) {
        try {
            String baseUrl = Server.ApiBinance.Method.API_V1;

            Retrofit retrofitBinance = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            BinanceApi binanceApi = retrofitBinance.create(BinanceApi.class);
            Call<List<BinanceFees>> call = binanceApi.binanceFees();
            call.enqueue(new Callback<List<BinanceFees>>() {
                @Override
                public void onResponse(@NonNull Call<List<BinanceFees>> call, @NonNull Response<List<BinanceFees>> response) {
                    if (response.code() == 200) {
                        for (BinanceFees fee : response.body()) {
                            if (fee.getFixed_fee_params() != null) {
                                Long longFee = Long.valueOf(fee.getFixed_fee_params().getFee());
                                Amount feeAmount = new Amount(BigDecimal.valueOf(longFee).divide(BigDecimal.valueOf(100000000)).setScale(8, RoundingMode.DOWN), getFeeCurrency());
                                coinData.minFee = coinData.normalFee = coinData.maxFee = feeAmount;
                                Log.i(TAG, "requestFee onResponse " + response.code());
                                blockchainRequestsCallbacks.onComplete(true);
                            }
                        }
                    } else {
                        ctx.setError(response.code());
                        Log.e(TAG, "requestFee onResponse " + response.code());
                        blockchainRequestsCallbacks.onComplete(false);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<BinanceFees>> call, @NonNull Throwable t) {
                    ctx.setError(t.getMessage());
                    Log.e(TAG, "requestFee onFailure " + t.getMessage());
                    blockchainRequestsCallbacks.onComplete(false);
                }
            });
        } catch (Exception e) {
            ctx.setError(e.getMessage());
            e.printStackTrace();
            Log.e(TAG, "FAIL Binance fee exception");
            blockchainRequestsCallbacks.onComplete(false);
        }
    }

    public void requestSendTransaction(BlockchainRequestsCallbacks blockchainRequestsCallbacks, byte[] txForSend) {
//        RequestBody requestBody = TransactionRequestAssemblerExtSign.createRequestBody(txForSend);
        try {
            ServerApiBinance serverApiBinance = new ServerApiBinance();

            ServerApiBinance.ResponseListener responseListener = new ServerApiBinance.ResponseListener() {
                @Override
                public void onSuccess() {
                    ctx.setError(null);
                    blockchainRequestsCallbacks.onComplete(true);
                }

                @Override
                public void onFail() {
                    ctx.setError("Broadcast error");
                    blockchainRequestsCallbacks.onComplete(false);
                }
            };

            serverApiBinance.setResponseListener(responseListener);
            serverApiBinance.sendTransaction(txForSend, client);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            ctx.setError(e.getMessage());
            blockchainRequestsCallbacks.onComplete(false);
        }
    }

    @Override
    public int pendingTransactionTimeoutInSeconds() {
        return 9;
    }

    @Override
    public boolean allowSelectFeeLevel() {
        return false;
    }

    @Override
    public boolean allowSelectFeeInclusion() {
        return coinData.getAssetBalance() == null;
    }

    @Override
    public boolean needMultipleLinesForBalance() {
        return true;
    }
}
