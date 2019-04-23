package com.tangem.wallet.binance;

import android.net.Uri;
import android.text.InputFilter;

import com.tangem.card_common.data.TangemCard;
import com.tangem.card_common.reader.CardProtocol;
import com.tangem.card_common.tasks.SignTask;
import com.tangem.card_common.util.Util;
import com.tangem.data.Blockchain;
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
import com.tangem.wallet.binance.client.domain.broadcast.Transfer;
import com.tangem.wallet.binance.client.encoding.Bech32;
import com.tangem.wallet.binance.client.encoding.Crypto;

import org.bitcoinj.core.Utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;


public class BinanceEngine extends CoinEngine {
    private static final String TAG = BinanceEngine.class.getSimpleName();

    public BinanceData coinData = null;

    public BinanceEngine(TangemContext context) throws Exception {
        super(context);
        if (context.getCoinData() == null) {
            coinData = new BinanceData();
            context.setCoinData(coinData);
        } else if (context.getCoinData() instanceof BinanceData) {
            coinData = (BinanceData) context.getCoinData();
        } else {
            throw new Exception("Invalid type of Blockchain data for XrpEngine");
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
    public boolean awaitingConfirmation() { //TODO:check
        return false;
    }

    @Override
    public String getBalanceHTML() { //TODO
        Amount balance = getBalance();
        if (balance != null) {
            return " " + balance.toDescriptionString(getDecimals()) + " <br><small><small>+ " + convertToAmount(coinData.getReserveInInternalUnits()).toDescriptionString(getDecimals()) + " reserve</small></small>";
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
        return Uri.parse("https://testnet-explorer.binance.org/address/" + ctx.getCoinData().getWallet()); //TODO: add mainnet explorer
    }

    public Uri getShareWalletUri() {
        return Uri.parse(ctx.getCoinData().getWallet());
    } //TODO:check

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
            throw new Exception("Invalid blockchain for " + TAG);
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
    public InternalAmount convertToInternalAmount(Amount amount) {;
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
        byte[] bytes = Util.longToByteArray(internalAmount.longValueExact());
        return bytes;
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

        String amount, fee;

        if (IncFee) {
            amount = amountValue.subtract(feeValue).setScale(getDecimals(), RoundingMode.DOWN).toPlainString();
        } else {
            amount = amountValue.setScale(getDecimals(), RoundingMode.DOWN).toPlainString();
        }

        Transfer tx = new Transfer();

        tx.setCoin("BNB");
        tx.setFromAddress(ctx.getCoinData().getWallet());
        tx.setToAddress(targetAddress);
        tx.setAmount(amount);

        BinanceDexApiRestClient client = BinanceDexApiClientFactory.newInstance().newRestClient(BinanceDexEnvironment.TEST_NET.getBaseUrl());


//        XrpPayment payment = new XrpPayment();
//
//        // Put `as` AccountID field Account, `Object` o
//        payment.as(AccountID.Account, coinData.getWallet());
//        payment.as(AccountID.Destination, targetAddress);
//        payment.as(com.ripple.core.coretypes.Amount.Amount, amount);
//        payment.as(UInt32.Sequence, coinData.getSequence());
//        payment.as(com.ripple.core.coretypes.Amount.Fee, fee);
//
//        XrpSignedTransaction signedTx = payment.prepare(canonisePubKey(ctx.getCard().getWalletPublicKeyRar()));
//
//        return new SignTask.TransactionToSign() {
//
//            @Override
//            public boolean isSigningMethodSupported(TangemCard.SigningMethod signingMethod) {
//                return signingMethod == TangemCard.SigningMethod.Sign_Hash;
//            }
//
//            @Override
//            public byte[][] getHashesToSign() throws Exception {
//                byte[][] dataForSign = new byte[1][];
//                if (ctx.getCard().getWalletPublicKeyRar().length == 33)
//                    dataForSign[0] = HashUtils.halfSha512(signedTx.signingData);
//                else if (ctx.getCard().getWalletPublicKeyRar().length == 32)
//                    dataForSign[0] = signedTx.signingData;
//                else
//                    throw new Exception("Invalid pubkey length");
//                return dataForSign;
//            }
//
//            @Override
//            public byte[] getRawDataToSign() throws Exception {
//                throw new Exception("Signing of raw transaction not supported for " + this.getClass().getSimpleName());
//            }
//
//            @Override
//            public String getHashAlgToSign() throws Exception {
//                throw new Exception("Signing of raw transaction not supported for " + this.getClass().getSimpleName());
//            }
//
//            @Override
//            public byte[] getIssuerTransactionSignature(byte[] dataToSignByIssuer) throws Exception {
//                throw new Exception("Transaction validation by issuer not supported in this version");
//            }
//
//            @Override
//            public byte[] onSignCompleted(byte[] signFromCard) throws Exception {
//                if (ctx.getCard().getWalletPublicKeyRar().length == 33) {
//                    int size = signFromCard.length / 2;
//                    BigInteger r = new BigInteger(1, Arrays.copyOfRange(signFromCard, 0, size));
//                    BigInteger s = new BigInteger(1, Arrays.copyOfRange(signFromCard, size, size * 2));
//                    s = CryptoUtil.toCanonicalised(s);
//                    ECDSASignature sig = new ECDSASignature(r, s);
//                    byte[] sigDer = sig.encodeToDER();
//                    if (!ECDSASignature.isStrictlyCanonical(sigDer)) {
//                        throw new IllegalStateException("Signature is not strictly canonical");
//                    }
//                    signedTx.addSign(sigDer);
//                } else if (ctx.getCard().getWalletPublicKeyRar().length == 32)
//                    signedTx.addSign(signFromCard);
//                else
//                    throw new Exception("Invalid pubkey length");
//                byte[] txForSend = BTCUtils.fromHex(signedTx.tx_blob);
//                notifyOnNeedSendTransaction(txForSend);
//                return txForSend;
//            }
//        };
    }
}
