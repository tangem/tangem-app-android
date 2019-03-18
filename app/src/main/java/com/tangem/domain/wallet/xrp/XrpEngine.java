package com.tangem.domain.wallet.xrp;

import android.net.Uri;
import android.text.InputFilter;

import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.types.known.tx.signed.SignedTransaction;
import com.ripple.core.types.known.tx.txns.Payment;
import com.tangem.card_common.data.TangemCard;
import com.tangem.card_common.reader.CardProtocol;
import com.tangem.card_common.tasks.SignTask;
import com.tangem.card_common.util.Util;
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

        byte[] decAddress = XrpBase58.decodeBase58(address);

        if (decAddress == null || decAddress.length == 0) {
            return false;
        }

        byte[] payload = new byte[21];
        System.arraycopy(decAddress, 0, payload, 0, 21);

        byte[] checksum = new byte[4];
        System.arraycopy(decAddress, 21, checksum, 0, 4);

        byte[] calcChecksum = new byte[4];
        System.arraycopy(CryptoUtil.doubleSha256(payload),0,calcChecksum,0,4);

        if (!Arrays.equals(checksum, calcChecksum)) {
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

    public String calculateAddress(byte[] pkCompressed) throws NoSuchAlgorithmException, NoSuchProviderException, IOException {
        byte[] pkForHash = canonicalPubBytes(pkCompressed);

        ByteArrayOutputStream address = new ByteArrayOutputStream();
        address.write((byte) 0x00);
        byte[] accountId = CryptoUtil.sha256ripemd160(pkForHash);
        address.write(accountId);

        byte [] doubleSha256 = CryptoUtil.doubleSha256(address.toByteArray());
        address.write(Arrays.copyOfRange(doubleSha256,0,4));

        return BTCUtils.toHex(address.toByteArray());
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
        }
        catch (Exception e)
        {
            ctx.getCoinData().setWallet("ERROR");
            throw new CardProtocol.TangemException("Can't define wallet address");
        }
    }

    public byte[] canonicalPubBytes(byte[] pkCompressed) {
        byte[] pkForHash = new byte[33];

        if (pkCompressed.length == 32) {
            pkForHash[0] =  (byte) 0xED;
            System.arraycopy(pkCompressed, 0, pkForHash, 1,32);
        } else {
            pkForHash = pkCompressed;
        }
    }
}

    @Override
    public SignTask.TransactionToSign constructTransaction(Amount amountValue, Amount feeValue, boolean IncFee, String targetAddress) throws Exception {
        checkBlockchainDataExists();

        Payment payment = new Payment();

        // Put `as` AccountID field Account, `Object` o
        payment.as(AccountID.Account, "rGZG674DSZJfoY8abMPSgChxZTJZEhyMRm");
        payment.as(AccountID.Destination, "rPMh7Pi9ct699iZUTWaytJUoHcJ7cgyziK");
        payment.as(com.ripple.core.coretypes.Amount.Amount, "1000000000");
        payment.as(UInt32.Sequence, 10);
        payment.as(com.ripple.core.coretypes.Amount.Fee, "10000");

        SignedTransaction signedTx = payment.prepare(canonicalPubBytes(ctx.getCard().getWalletPublicKeyRar()));

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
}
