package com.tangem.domain.wallet;

import android.net.Uri;
import android.text.InputFilter;
import android.util.Log;

import com.tangem.domain.cardReader.CardProtocol;
import com.tangem.domain.cardReader.TLV;
import com.tangem.util.BTCUtils;
import com.tangem.util.CryptoUtil;
import com.tangem.util.DecimalDigitsInputFilter;
import com.tangem.wallet.R;

import org.bitcoinj.core.ECKey;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;

/**
 * Created by Ilia on 15.02.2018.
 */

public class EthEngine extends CoinEngine {

    public EthData coinData = null;

    public EthEngine(TangemContext ctx) throws Exception {
        super(ctx);
        if (ctx.getCoinData() == null) {
            coinData = new EthData();
            ctx.setCoinData(coinData);
        } else if (ctx.getCoinData() instanceof EthData) {
            coinData = (EthData) ctx.getCoinData();
        } else {
            throw new Exception("Invalid type of Blockchain data for EthEngine");
        }
    }

    public EthEngine() {
        super();
    }

    private static int getDecimals() {
        return 18;
    }

    @Override
    public boolean awaitingConfirmation(){
        return false;
    }

    @Override
    public Amount getBalance() {
        if (!hasBalanceInfo()) {
            return null;
        }
        return convertToAmount(coinData.getBalanceInInternalUnits());
    }

    @Override
    public String getBalanceHTML() {
        Amount balance=getBalance();
        if( balance!=null ) {
            return balance.toDescriptionString(getDecimals());
        }else{
            return "";
        }
    }

    @Override
    public String getBalanceCurrency() {
        return "ETH";
    }

    @Override
    public String getOfflineBalanceHTML() {
        InternalAmount offlineInternalAmount = convertToInternalAmount(ctx.getCard().getOfflineBalance());
        Amount offlineAmount = convertToAmount(offlineInternalAmount);
        return offlineAmount.toDescriptionString(getDecimals());
    }

    @Override
    public boolean isBalanceNotZero() {
        if( coinData ==null ) return false;
        if (coinData.getBalanceInInternalUnits() == null) return false;
        return coinData.getBalanceInInternalUnits().notZero();
    }

    @Override
    public String getFeeCurrency() {
        return "ETH";
    }

    public boolean isNeedCheckNode() {
        return false;
    }


    @Override
    public CoinData createCoinData() {
        return new EthData();
    }

    @Override
    public String getUnspentInputsDescription() {
        return "";
    }

//    BigDecimal convertToEth(String value) {
//        BigInteger m = new BigInteger(value, 10);
//        BigDecimal n = new BigDecimal(m);
//        BigDecimal d = n.divide(new BigDecimal("1000000000000000000"));
//        d = d.setScale(8, RoundingMode.DOWN);
//        return d;
//    }

    @Override
    public boolean validateAddress(String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }

        if (!address.startsWith("0x") && !address.startsWith("0X")) {
            return false;
        }

        if (address.length() != 42) {
            return false;
        }

        return true;
    }

//    public String getBalanceValue(TangemCard mCard) {
//        String dec = coinData.getBalanceInInternalUnits();
//        BigDecimal d = convertToEth(dec);
//        String s = d.toString();
//
//        String pattern = "#0.##################"; // If you like 4 zeros
//        DecimalFormat myFormatter = new DecimalFormat(pattern);
//        String output = myFormatter.format(d);
//        return output;
//    }

//    public static String getAmountEquivalentDescription(Amount amount, double rateValue) {
//        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0)
//            return "";
//
//        if (rateValue > 0) {
//            BigDecimal biRate = new BigDecimal(rateValue);
//            BigDecimal exchangeCurs = biRate.multiply(amount);
//            exchangeCurs = exchangeCurs.setScale(2, RoundingMode.DOWN);
//            return "≈ USD  " + exchangeCurs.toString();
//        } else {
//            return "";
//        }
//    }

//    public static String getAmountEquivalentDescriptionETH(Double amount, float rate) {
//        if (amount == 0)
//            return "";
//        amount = amount / 100000;
//        if (rate > 0) {
//            return String.format("≈ USD %.2f", amount * rate);
//        } else {
//            return "";
//        }
//
//    }


    @Override
    public String getBalanceEquivalent() {
        Amount balance=getBalance();
        if( balance==null ) return "";
        return balance.toEquivalentString(coinData.getRate());
    }

    @Override
    public Amount convertToAmount(InternalAmount internalAmount) {
        BigDecimal d = internalAmount.divide(new BigDecimal("1000000000000000000"), getDecimals(), RoundingMode.DOWN);
        return new Amount(d, ctx.getBlockchain().getCurrency());
    }

    @Override
    public Amount convertToAmount(String strAmount, String currency) {
        return new Amount(strAmount, currency);
    }

    @Override
    public InternalAmount convertToInternalAmount(Amount amount){
        return new InternalAmount(amount.multiply(new BigDecimal("1000000000000000000")),"wei");
    }

    @Override
    public InternalAmount convertToInternalAmount(byte[] bytes) {
        //throw new Exception("Not implemented");
        return null;
    }


    @Override
    public byte[] convertToByteArray(InternalAmount amount) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public boolean hasBalanceInfo() {
        return coinData.getBalanceInInternalUnits()!=null;
    }

    @Override
    public Uri getShareWalletUri() {
        if (ctx.getCard().getDenomination() != null) {
            return Uri.parse("ethereum:" + ctx.getCard().getWallet());// + "?value=" + mCard.getDenomination() +"e18");
        } else {
            return Uri.parse("ethereum:" + ctx.getCard().getWallet());
        }
    }

    @Override
    public Uri getShareWalletUriExplorer() {
        if (ctx.getCard().getBlockchain() == Blockchain.EthereumTestNet)
            return Uri.parse("https://rinkeby.etherscan.io/address/" + ctx.getCard().getWallet());
        else
            return Uri.parse("https://etherscan.io/address/" + ctx.getCard().getWallet());
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
    public InputFilter[] getAmountInputFilters() {
        return new InputFilter[] { new DecimalDigitsInputFilter(getDecimals()) };
    }

    @Override
    public boolean checkNewTransactionAmount(Amount amount){
        if( coinData ==null ) return false;
        Amount balance=getBalance();
        if (balance==null || amount.compareTo(balance) > 0) {
            return false;
        }
        return true;
    }

    @Override
    public boolean checkNewTransactionAmountAndFee(Amount amount, Amount fee, Boolean isFeeIncluded) {
//        Long fee = null;
//        Long amount = null;
//        try {
//            amount = mCard.internalUnitsFromString(amountValue);
//            fee = mCard.internalUnitsFromString(feeValue);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//
//        if (fee == null || amount == null)
//            return false;
//
//        if (fee == 0 || amount == 0)
//            return false;
//
//
//        if (fee < minFeeInInternalUnits)
//            return false;

        try {
            BigDecimal cardBalance = getBalance();

            if (isFeeIncluded && amount.compareTo(cardBalance) > 0 )
                return false;

            if (!isFeeIncluded && amount.add(fee).compareTo(cardBalance) > 0)
                return false;

        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public boolean validateBalance(BalanceValidator balanceValidator) {
        if (getBalance() == null) {
            balanceValidator.setScore(0);
            balanceValidator.setFirstLine("Unknown balance");
            balanceValidator.setSecondLine("Balance cannot be verified. Swipe down to refresh.");
            return false;
        }

        if (!coinData.getUnconfirmedTXCount().equals(coinData.getConfirmedTXCount())) {
            balanceValidator.setScore(0);
            balanceValidator.setFirstLine("Unguaranteed balance");
            balanceValidator.setSecondLine("Transaction is in progress. Wait for confirmation in blockchain.");
            return false;
        }

        if (coinData.isBalanceReceived()) {
            balanceValidator.setScore(100);
            balanceValidator.setFirstLine("Verified balance");
            balanceValidator.setSecondLine("Balance confirmed in blockchain");
            if (getBalance().isZero()) {
                balanceValidator.setFirstLine("Empty wallet");
                balanceValidator.setSecondLine("");
            }
        }

        if ((ctx.getCard().getOfflineBalance() != null) && !coinData.isBalanceReceived() && (ctx.getCard().getRemainingSignatures() == ctx.getCard().getMaxSignatures()) && getBalance().notZero()) {
            balanceValidator.setScore(80);
            balanceValidator.setFirstLine("Verified offline balance");
            balanceValidator.setSecondLine("Restore internet connection to obtain trusted balance from blockchain");
        }

        return true;

    }

    @Override
    public String evaluateFeeEquivalent(String fee) {
        try {
            Amount feeValue = new Amount(fee, ctx.getBlockchain().getCurrency());
            return feeValue.toEquivalentString(coinData.getRate());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public String calculateAddress(byte[] pkUncompressed) throws NoSuchProviderException, NoSuchAlgorithmException {
        Keccak256 kec = new Keccak256();
        int lenPk = pkUncompressed.length;
        if (lenPk < 2) {
            throw new IllegalArgumentException("Uncompress public key length is invald");
        }
        byte[] cleanKey = new byte[lenPk - 1];
        for (int i = 0; i < cleanKey.length; ++i) {
            cleanKey[i] = pkUncompressed[i + 1];
        }
        byte[] r = kec.digest(cleanKey);

        byte[] address = new byte[20];
        for (int i = 0; i < 20; ++i) {
            address[i] = r[i + 12];
        }

        return String.format("0x%s", BTCUtils.toHex(address));
    }

    @Override
    public byte[] sign(Amount feeValue, Amount amountValue, boolean IncFee, String targetAddress, CardProtocol protocol) throws Exception {

        BigInteger nonceValue = coinData.getConfirmedTXCount();
        byte[] pbKey = ctx.getCard().getWalletPublicKey();
        boolean flag = (ctx.getCard().getSigningMethod() == TangemCard.SigningMethod.Sign_Hash_Validated_By_Issuer);
        Issuer issuer = ctx.getCard().getIssuer();


        BigInteger gigaK = BigInteger.valueOf(1000000000L);

        BigInteger weiFee=convertToInternalAmount(feeValue).toBigIntegerExact();
        BigInteger weiAmount=convertToInternalAmount(amountValue).toBigIntegerExact();

        if (IncFee) {
            weiAmount = weiAmount.subtract(weiFee);
        }

        BigInteger nonce = nonceValue;
        BigInteger gasPrice = weiFee.divide(gigaK).divide(BigInteger.valueOf(21000)).multiply(gigaK);
        BigInteger gasLimit = BigInteger.valueOf(21000);
        Integer chainId = ctx.getBlockchain() == Blockchain.Ethereum ? EthTransaction.ChainEnum.Mainnet.getValue() : EthTransaction.ChainEnum.Rinkeby.getValue();

        String to = targetAddress;

        if (to.startsWith("0x") || to.startsWith("0X")) {
            to = to.substring(2);
        }

        EthTransaction tx = EthTransaction.create(to, weiAmount, nonce, gasPrice, gasLimit, chainId);

        byte[][] hashesForSign = new byte[1][];
        byte[] for_hash = tx.getRawHash();
        hashesForSign[0] = for_hash;

        byte[] signFromCard = null;
        try {
            signFromCard = protocol.run_SignHashes(PINStorage.getPIN2(), hashesForSign, flag, null, issuer).getTLV(TLV.Tag.TAG_Signature).Value;
            // TODO slice signFromCard to hashes.length parts
        } catch (Exception ex) {
            Log.e("ETH", ex.getMessage());
            return null;
        }

        BigInteger r = new BigInteger(1, Arrays.copyOfRange(signFromCard, 0, 32));
        BigInteger s = new BigInteger(1, Arrays.copyOfRange(signFromCard, 32, 64));
        s = CryptoUtil.toCanonicalised(s);

        boolean f = ECKey.verify(for_hash, new ECKey.ECDSASignature(r, s), pbKey);

        if (!f) {
            Log.e("ETH-CHECK", "sign Failed.");
        }

        tx.signature = new ECDSASignatureETH(r, s);
        int v = tx.BruteRecoveryID2(tx.signature, for_hash, pbKey);
        if (v != 27 && v != 28) {
            Log.e("ETH", "invalid v");
            return null;
        }
        tx.signature.v = (byte) v;
        Log.e("ETH_v", String.valueOf(v));

        byte[] realTX = tx.getEncoded();
        return realTX;
    }
}
