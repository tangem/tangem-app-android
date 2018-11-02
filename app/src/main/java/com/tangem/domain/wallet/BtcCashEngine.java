package com.tangem.domain.wallet;

import android.net.Uri;
import android.text.InputFilter;

import com.tangem.domain.cardReader.CardProtocol;
import com.tangem.domain.cardReader.TLV;
import com.tangem.util.BTCUtils;
import com.tangem.util.CryptoUtil;
import com.tangem.util.DecimalDigitsInputFilter;
import com.tangem.util.DerEncodingUtil;
import com.tangem.util.Util;
import com.tangem.wallet.R;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Ilia on 15.02.2018.
 */

public class BtcCashEngine extends CoinEngine {

    public BtcData coinData = null;

    public BtcCashEngine(TangemContext context) throws Exception {
        super(context);
        if (context.getCoinData() == null) {
            coinData = new BtcData();
            context.setCoinData(coinData);
        } else if (context.getCoinData() instanceof BtcData) {
            coinData = (BtcData) context.getCoinData();
        } else {
            throw new Exception("Invalid type of Blockchain data for BtcEngine");
        }
    }

    public BtcCashEngine() {

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
        return coinData.getBalanceUnconfirmed() != 0;
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
        return "BCH";
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

    @Override
    public boolean isExtractPossible() {
        if (!hasBalanceInfo()) {
            ctx.setMessage(R.string.cannot_obtain_data_from_blockchain);
        } else if (!isBalanceNotZero()) {
            ctx.setMessage(R.string.wallet_empty);
        } else if (awaitingConfirmation()) {
            ctx.setMessage(R.string.please_wait_while_previous);
        } else if (coinData.getUnspentTransactions().size() == 0) {
            ctx.setMessage(R.string.please_wait_for_confirmation);
        } else {
            return true;
        }
        return false;
    }

    @Override
    public String getFeeCurrency() {
        return "mBCH";
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

        if (!address.startsWith("1") && !address.startsWith("2") && !address.startsWith("3") && !address.startsWith("n") && !address.startsWith("m")) {
            return false;
        }

        byte[] decAddress = Base58.decodeBase58(address);

        if (decAddress == null || decAddress.length == 0) {
            return false;
        }

        byte[] rip = new byte[21];
        for (int i = 0; i < 21; ++i) {
            rip[i] = decAddress[i];
        }

        byte[] kcv = CryptoUtil.doubleSha256(rip);

        for (int i = 0; i < 4; ++i) {
            if (kcv[i] != decAddress[21 + i])
                return false;
        }

        if (ctx.getBlockchain() != Blockchain.BitcoinCashTestNet && ctx.getBlockchain() != Blockchain.BitcoinCash) {
            return false;
        }

        if (ctx.getBlockchain() == Blockchain.BitcoinCashTestNet && (address.startsWith("1") || address.startsWith("3"))) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isNeedCheckNode() {
        return true;
    }

    @Override
    public Uri getShareWalletUriExplorer() {
        return Uri.parse((ctx.getBlockchain() == Blockchain.BitcoinCash ? "https://bitcoincash.blockexplorer.com/address/" : "https://testnet.blockexplorer.com/address/") + ctx.getCard().getWallet());
    }

    @Override
    public Uri getShareWalletUri() {
        return Uri.parse("bitcoincash:" + ctx.getCard().getWallet());
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

        if (isIncludeFee && (amount.compareTo(coinData.getBalanceInInternalUnits()) > 0 || amount.compareTo(fee)<0))
            return false;

        if (!isIncludeFee && amount.add(fee).compareTo(coinData.getBalanceInInternalUnits()) > 0)
            return false;

        return true;
    }

    @Override
    public boolean validateBalance(BalanceValidator balanceValidator) {
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

        if (coinData.getBalanceUnconfirmed() != 0) {
            balanceValidator.setScore(0);
            balanceValidator.setFirstLine("Transaction in progress");
            balanceValidator.setSecondLine("Wait for confirmation in blockchain");
            return false;
        }

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
    }

    @Override
    public Amount getBalance() {
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
        Amount balance=getBalance();
        if( balance==null ) return "";
        return balance.toEquivalentString(coinData.getRate());
    }

    @Override
    public String calculateAddress(byte[] pkUncompressed) throws NoSuchProviderException, NoSuchAlgorithmException {

        byte netSelectionByte;
        switch (ctx.getBlockchain()) {
            case BitcoinCash:
                netSelectionByte = (byte) 0x00; //0 - MainNet 0x6f - TestNet
                break;
            default:
                netSelectionByte = (byte) 0x6f; //0 - MainNet 0x6f - TestNet
                break;
        }

        byte hash1[] = Util.calculateSHA256(pkUncompressed);
        byte hash2[] = Util.calculateRIPEMD160(hash1);

        ByteBuffer BB = ByteBuffer.allocate(hash2.length + 1);

        BB.put(netSelectionByte);
        BB.put(hash2);

        byte hash3[] = Util.calculateSHA256(BB.array());
        byte hash4[] = Util.calculateSHA256(hash3);

        BB = ByteBuffer.allocate(hash2.length + 5);
        BB.put(netSelectionByte); //BB.put((byte) 0x6f);
        BB.put(hash2);
        BB.put(hash4[0]);
        BB.put(hash4[1]);
        BB.put(hash4[2]);
        BB.put(hash4[3]);

        return org.bitcoinj.core.Base58.encode(BB.array());
    }

    @Override
    public Amount convertToAmount(InternalAmount internalAmount) {
        BigDecimal d = internalAmount.divide(new BigDecimal("100000000"));
        return new Amount(d, getBalanceCurrency());
    }

    @Override
    public Amount convertToAmount(String strAmount, String currency) {
        return new Amount(strAmount, currency);
    }

    @Override
    public InternalAmount convertToInternalAmount(Amount amount) throws Exception {
        BigDecimal d = amount.multiply(new BigDecimal("100000000"));
        return new InternalAmount(d, getBalanceCurrency());
    }

    @Override
    public InternalAmount convertToInternalAmount(byte[] bytes) {
        if (bytes == null) return null;
        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) reversed[i] = bytes[bytes.length - i - 1];
        return new InternalAmount(Util.byteArrayToLong(reversed), "Satoshi");
    }

    @Override
    public byte[] convertToByteArray(InternalAmount internalAmount) throws Exception {
        byte[] bytes = Util.longToByteArray(internalAmount.longValueExact());
        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) reversed[i] = bytes[bytes.length - i - 1];
        return reversed;
    }

    @Override
    public CoinData createCoinData() {
        return new BtcData();
    }

    @Override
    public String getUnspentInputsDescription() {
        return coinData.getUnspentInputsDescription();
    }

//    @Override
//    public String getAmountDescription(TangemCard mCard, String amount) throws Exception {
//        return mCard.getAmountDescription(Double.parseDouble(amount));
//    }

    @Override
    public byte[] sign(Amount feeValue, Amount amountValue, boolean IncFee, String targetAddress, CardProtocol protocol) throws Exception {

        checkBlockchainDataExists();

        String myAddress = ctx.getCard().getWallet();
        byte[] pbKey = ctx.getCard().getWalletPublicKeyRar(); //ALWAYS USING COMPRESS KEY

        // Build script for our address
        List<BtcData.UnspentTransaction> rawTxList = coinData.getUnspentTransactions();
        byte[] outputScriptWeAreAbleToSpend = Transaction.Script.buildOutput(myAddress).bytes;

        // Collect unspent
        ArrayList<UnspentOutputInfo> unspentOutputs = BTCUtils.getOutputs(rawTxList, outputScriptWeAreAbleToSpend);

        long fullAmount = 0;
        for (int i = 0; i < unspentOutputs.size(); ++i) {
            fullAmount += unspentOutputs.get(i).value;
        }


        long fees = convertToInternalAmount(feeValue).longValueExact();
        long amount = convertToInternalAmount(amountValue).longValueExact();
        long change = fullAmount - amount;
        if (IncFee) {
            amount = amount - fees;
        } else {
            change = change - fees;
        }

        if (amount + fees > fullAmount) {
            throw new CardProtocol.TangemException_WrongAmount(String.format("Balance (%d) < change (%d) + amount (%d)", fullAmount, change, amount));
        }

        byte[][] dataForSign = new byte[unspentOutputs.size()][];

        for (int i = 0; i < unspentOutputs.size(); ++i) {
            byte[] newTX = BTCUtils.buildTXForSign(myAddress, targetAddress, myAddress, unspentOutputs, i, amount, change);

            byte[] hashData = Util.calculateSHA256(newTX);
            byte[] doubleHashData = Util.calculateSHA256(hashData);

            unspentOutputs.get(i).bodyDoubleHash = doubleHashData;
            unspentOutputs.get(i).bodyHash = hashData;

            if (ctx.getCard().getSigningMethod() == TangemCard.SigningMethod.Sign_Raw || ctx.getCard().getSigningMethod() == TangemCard.SigningMethod.Sign_Raw_Validated_By_Issuer) {
                dataForSign[i] = newTX;
            } else {
                dataForSign[i] = doubleHashData;
            }

        }

        byte[] signFromCard;
        if (ctx.getCard().getSigningMethod() == TangemCard.SigningMethod.Sign_Raw || ctx.getCard().getSigningMethod() == TangemCard.SigningMethod.Sign_Raw_Validated_By_Issuer) {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            if (dataForSign.length > 10) throw new Exception("To much hashes in one transaction!");
            for (int i = 0; i < dataForSign.length; i++) {
                if (i != 0 && dataForSign[0].length != dataForSign[i].length)
                    throw new Exception("Hashes length must be identical!");
                bs.write(dataForSign[i]);
            }
            signFromCard = protocol.run_SignRaw(PINStorage.getPIN2(), bs.toByteArray()).getTLV(TLV.Tag.TAG_Signature).Value;
        } else {
            signFromCard = protocol.run_SignHashes(PINStorage.getPIN2(), dataForSign, ctx.getCard().getSigningMethod() == TangemCard.SigningMethod.Sign_Hash_Validated_By_Issuer, null, ctx.getCard().getIssuer()).getTLV(TLV.Tag.TAG_Signature).Value;
            // TODO slice signFromCard to hashes.length parts
        }

        for (int i = 0; i < unspentOutputs.size(); ++i) {
            BigInteger r = new BigInteger(1, Arrays.copyOfRange(signFromCard, i * 64, 32 + i * 64));
            BigInteger s = new BigInteger(1, Arrays.copyOfRange(signFromCard, 32 + i * 64, 64 + i * 64));
            s = CryptoUtil.toCanonicalised(s);

            unspentOutputs.get(i).scriptForBuild = DerEncodingUtil.packSignDerBitcoinCash(r, s, pbKey);
        }

        return BTCUtils.buildTXForSend(targetAddress, myAddress, unspentOutputs, amount, change);
    }
}
