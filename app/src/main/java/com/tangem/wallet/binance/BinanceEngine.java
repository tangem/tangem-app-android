package com.tangem.wallet.binance;

import android.net.Uri;
import android.text.InputFilter;
import android.util.Log;

import androidx.annotation.NonNull;

import com.tangem.card_common.data.TangemCard;
import com.tangem.card_common.reader.CardProtocol;
import com.tangem.card_common.tasks.SignTask;
import com.tangem.card_common.util.Util;
import com.tangem.data.Blockchain;
import com.tangem.data.network.BinanceApi;
import com.tangem.data.network.Server;
import com.tangem.data.network.ServerApiBinance;
import com.tangem.data.network.model.BinanceFees;
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


public class BinanceEngine extends CoinEngine {
    private static final String TAG = BinanceEngine.class.getSimpleName();

    public BinanceData coinData = null;
    private BinanceDexApiRestClient client = null;

    public BinanceEngine(TangemContext context) throws Exception {
        super(context);
        if (context.getCoinData() == null) {
            coinData = new BinanceData();
            context.setCoinData(coinData);
        } else if (context.getCoinData() instanceof BinanceData) {
            coinData = (BinanceData) context.getCoinData();
        } else {
            throw new Exception("Invalid type of Blockchain data for " + TAG);
        }
        if (ctx.getBlockchain() == Blockchain.Binance) {
            client = BinanceDexApiClientFactory.newInstance().newRestClient(BinanceDexEnvironment.PROD.getBaseUrl());
            coinData.setChainId("Binance-Chain-Tigris");
        } else if (ctx.getBlockchain() == Blockchain.BinanceTestNet) {
            client = BinanceDexApiClientFactory.newInstance().newRestClient(BinanceDexEnvironment.TEST_NET.getBaseUrl());
            coinData.setChainId("Binance-Chain-Nile");
        } else {
            throw new Exception("Invalid blockchain for BinanceEngine");
        }
    }

    public BinanceEngine() {
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
        return false;
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
        return "BNB";
    }

    @Override
    public String getOfflineBalanceHTML() { //TODO:check
        InternalAmount offlineInternalAmount = convertToInternalAmount(ctx.getCard().getOfflineBalance());
        Amount offlineAmount = convertToAmount(offlineInternalAmount);
        return offlineAmount.toDescriptionString(getDecimals());
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
        if (coinData == null) return false;
        return amount.compareTo(coinData.getBalance()) <= 0;
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
                balanceValidator.setScore(0);
                balanceValidator.setFirstLine("Unknown balance");
                balanceValidator.setSecondLine("Balance cannot be verified. Swipe down to refresh.");
                return false;
            }

            if (coinData.isBalanceReceived()) {
                balanceValidator.setScore(100);
                balanceValidator.setFirstLine("Verified balance");
                balanceValidator.setSecondLine("Balance confirmed in blockchain");
                if (coinData.getBalance().isZero()) {
                    balanceValidator.setFirstLine("Empty wallet");
                    balanceValidator.setSecondLine("");
                }
            }

            if ((ctx.getCard().getOfflineBalance() != null) && !coinData.isBalanceReceived() && (ctx.getCard().getRemainingSignatures() == ctx.getCard().getMaxSignatures()) && coinData.getBalance().notZero()) {
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

    public String calculateAddress(byte[] pkCompressed) throws Exception {
        byte[] pubKeyHash = Utils.sha256hash160(pkCompressed);

        if (ctx.getBlockchain() == Blockchain.Binance) {
            return Bech32.encode("bnb", Crypto.convertBits(pubKeyHash, 0, pubKeyHash.length, 8, 5, false));
        } else if (ctx.getBlockchain() == Blockchain.BinanceTestNet) {
            return Bech32.encode("tbnb", Crypto.convertBits(pubKeyHash, 0, pubKeyHash.length, 8, 5, false));
        } else {
            throw new Exception("Invalid blockchain for BinanceEngine");
        }
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
        return new BinanceData();
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

    @Override
    public SignTask.TransactionToSign constructTransaction(Amount amountValue, Amount feeValue, boolean IncFee, String targetAddress) throws Exception {
        checkBlockchainDataExists();

        String amount;

        if (IncFee) {
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
        transfer.setCoin("BNB");
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

            if (ctx.getBlockchain() == Blockchain.Binance) {
                coinData.setValidationNodeDescription(Server.ApiBinance.URL_BINANCE);
            } else if (ctx.getBlockchain() == Blockchain.BinanceTestNet) {
                coinData.setValidationNodeDescription(Server.ApiBinanceTestnet.URL_BINANCE_TESTNET);
            } else {
                throw new Exception("Invalid blockchain for BinanceEngine");
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "FAIL Binance balance exception");
            ctx.setError(e.getMessage());
            blockchainRequestsCallbacks.onComplete(false);
        }
    }

    public void requestFee(BlockchainRequestsCallbacks blockchainRequestsCallbacks, String targetAddress, Amount amount) {
        try {
            String baseUrl;

            if (ctx.getBlockchain() == Blockchain.Binance) {
                baseUrl = Server.ApiBinance.Method.API_V1;
            } else if (ctx.getBlockchain() == Blockchain.BinanceTestNet) {
                baseUrl = Server.ApiBinanceTestnet.Method.API_V1;
            } else {
                throw new Exception("Invalid blockchain for BinanceEngine");
            }

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

    public int pendingTransactionTimeoutInSeconds() {
        return 9;
    }

    public boolean allowSelectFeeLevel() {
        return false;
    }
}
