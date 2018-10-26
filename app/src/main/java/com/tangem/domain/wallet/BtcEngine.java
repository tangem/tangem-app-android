package com.tangem.domain.wallet;

import android.net.Uri;
import android.text.InputFilter;

import com.tangem.domain.BitcoinNode;
import com.tangem.domain.BitcoinNodeTestNet;
import com.tangem.domain.cardReader.CardProtocol;
import com.tangem.domain.cardReader.TLV;
import com.tangem.util.BTCUtils;
import com.tangem.util.CryptoUtil;
import com.tangem.util.DecimalDigitsInputFilter;
import com.tangem.util.DerEncodingUtil;
import com.tangem.util.FormatUtil;
import com.tangem.util.Util;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BtcEngine extends CoinEngine {

    public BtcData btcData = null;

    public BtcEngine(TangemContext context) throws Exception {
        super(context);
        if (context.getCoinData() == null) {
            btcData = new BtcData();
            context.setCoinData(btcData);
        } else if (context.getCoinData() instanceof BtcData) {
            btcData = (BtcData) context.getCoinData();
        } else {
            throw new Exception("Invalid type of Blockchain data for BtcEngine");
        }
    }

    public BtcEngine() {
        super();
    }

    private static String[] getBitcoinServiceHosts() {
        return new String[]{
                BitcoinNode.n1.getHost(),
                BitcoinNode.n2.getHost(),
                BitcoinNode.n3.getHost(),
                BitcoinNode.n4.getHost(),
                BitcoinNode.n5.getHost(),
                BitcoinNode.n6.getHost(),
                BitcoinNode.n7.getHost(),
                BitcoinNode.n8.getHost(),
                BitcoinNode.n9.getHost(),
                BitcoinNode.n10.getHost(),
                BitcoinNode.n11.getHost(),
                BitcoinNode.n12.getHost(),
        };
    }

    private static Integer[] getBitcoinServicePorts() {
        return new Integer[]{
                BitcoinNode.n1.getPort(),
                BitcoinNode.n2.getPort(),
                BitcoinNode.n3.getPort(),
                BitcoinNode.n4.getPort(),
                BitcoinNode.n5.getPort(),
                BitcoinNode.n6.getPort(),
                BitcoinNode.n7.getPort(),
                BitcoinNode.n8.getPort(),
                BitcoinNode.n9.getPort(),
                BitcoinNode.n10.getPort(),
                BitcoinNode.n11.getPort(),
                BitcoinNode.n12.getPort(),
        };
    }

    private static String[] getBitcoinTestNetServiceHosts() {
        return new String[]{
                BitcoinNodeTestNet.n1.getHost(),
                BitcoinNodeTestNet.n2.getHost(),
                BitcoinNodeTestNet.n3.getHost(),
                BitcoinNodeTestNet.n4.getHost()};
    }

    private static Integer[] getBitcoinTestNetServicePorts() {
        return new Integer[]{
                BitcoinNodeTestNet.n1.getPort(),
                BitcoinNodeTestNet.n2.getPort(),
                BitcoinNodeTestNet.n3.getPort(),
                BitcoinNodeTestNet.n4.getPort()};
    }

    static int serviceIndex = 0;

    private void checkBlockchainDataExists() throws Exception {
        if (btcData == null) throw new Exception("No blockchain data");
    }

    @Override
    public boolean awaitingConfirmation(){
        if( btcData==null ) return false;
        return btcData.getBalanceUnconfirmed() != 0;
    }

    @Override
    public String getBalanceHTML() {
        if (hasBalanceInfo()) {
            Amount balance = convertToAmount(btcData.getBalanceInInternalUnits());
            return balance.toString();
        } else {
            return "";
        }
    }

    @Override
    public String getBalanceCurrencyHTML() {
        return "BTC";
    }

    @Override
    public String getOfflineBalanceHTML() {
            InternalAmount offlineInternalAmount = convertToInternalAmount(ctx.getCard().getOfflineBalance());
            Amount offlineAmount = convertToAmount(offlineInternalAmount);
       return offlineAmount.toString();
    }

    @Override
    public boolean isBalanceAlterNotZero() {
        return true;
    }

    @Override
    public boolean isBalanceNotZero() {
        if( btcData==null ) return false;
        if (btcData.getBalanceInInternalUnits() == null) return false;
        return btcData.getBalanceInInternalUnits().notZero();
    }

    @Override
    public boolean hasBalanceInfo(){
        if( btcData==null ) return false;
        return btcData.hasBalanceInfo();
    }


    @Override
    public boolean checkUnspentTransaction() throws Exception {
        checkBlockchainDataExists();
        return btcData.getUnspentTransactions().size() != 0;
    }

    @Override
    public String getFeeCurrencyHTML() {
        return "BTC";
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

        if (ctx.getBlockchain() != Blockchain.BitcoinTestNet && ctx.getBlockchain() != Blockchain.Bitcoin) {
            return false;
        }

        if (ctx.getBlockchain() == Blockchain.BitcoinTestNet && (address.startsWith("1") || address.startsWith("3"))) {
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
        return Uri.parse((ctx.getBlockchain() == Blockchain.Bitcoin ? "https://blockchain.info/address/" : "https://testnet.blockchain.info/address/") + ctx.getCard().getWallet());
    }

    @Override
    public Uri getShareWalletUri() {
        if (ctx.getCard().getDenomination() != null) {
            return Uri.parse("bitcoin:" + ctx.getCard().getWallet() + "?amount=" + BTCUtils.satoshiToBtc(ctx.getCard().getDenomination()));
        } else {
            return Uri.parse("bitcoin:" + ctx.getCard().getWallet());
        }
    }

    @Override
    public InputFilter[] getAmountInputFilters() {
        return new InputFilter[] { new DecimalDigitsInputFilter(8) };
    }

    @Override
    public boolean checkNewTransactionAmount(Amount amount){
        if( btcData==null ) return false;
        if (amount.compareTo(convertToAmount(btcData.getBalanceInInternalUnits())) > 0) {
            return false;
        }
        return true;
    }

    @Override
    public boolean checkNewTransactionAmountAndFee(Amount amountValue, Amount feeValue, Boolean isIncludeFee, InternalAmount minFeeInInternalUnits) {
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

        if (isIncludeFee && amount.compareTo(btcData.getBalanceInInternalUnits())>0)
            return false;

        if (!isIncludeFee && amount.add(fee).compareTo(btcData.getBalanceInInternalUnits())>0)
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

        if (btcData.getBalanceUnconfirmed() != 0) {
            balanceValidator.setScore(0);
            balanceValidator.setFirstLine("Transaction in progress");
            balanceValidator.setSecondLine("Wait for confirmation in blockchain");
            return false;
        }

        if (btcData.isBalanceReceived() && btcData.isBalanceEqual()) {
            balanceValidator.setScore(100);
            balanceValidator.setFirstLine("Verified balance");
            balanceValidator.setSecondLine("Balance confirmed in blockchain");
            if (btcData.getBalanceInInternalUnits().isZero()) {
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

        if ((ctx.getCard().getOfflineBalance() != null) && !btcData.isBalanceReceived() && (ctx.getCard().getRemainingSignatures() == ctx.getCard().getMaxSignatures()) && btcData.getBalanceInInternalUnits().notZero() ) {
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
        return convertToAmount(btcData.getBalanceInInternalUnits());
    }

    @Override
    public String evaluateFeeEquivalent(String fee) {
        return getAmountEquivalentDescriptor(ctx.getCard(), fee);
    }

    @Override
    public String getBalanceEquivalent() {
        if( btcData==null ) return "";
        return btcData.getAmountEquivalentDescription(getBalance());
    }

    @Override
    public String calculateAddress(byte[] pkUncompressed) throws NoSuchProviderException, NoSuchAlgorithmException {
        byte netSelectionByte;
        switch (ctx.getBlockchain()) {
            case Bitcoin:
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
        //TODO
        return null;
    }

    @Override
    public Amount convertToAmount(String strAmount) {
        //TODO
        return null;
    }

    @Override
    public InternalAmount convertToInternalAmount(Amount amount) throws Exception {
        //TODO
        return null;
    }

    @Override
    public InternalAmount convertToInternalAmount(byte[] bytes) {
        if (bytes == null) return null;
        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) reversed[i] = bytes[bytes.length - i - 1];
        return new InternalAmount(Util.byteArrayToLong(reversed));
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
        return btcData.getUnspentInputsDescription();
    }

//    @Override
//    public String getAmountDescription(TangemCard card, String amount) throws Exception {
//        return card.getAmountDescription(Double.parseDouble(amount));
//    }

    public static String getAmountEquivalentDescriptionBTC(Double amount, float rate) {
        if ((rate > 0) && (amount > 0)) {
            return String.format("≈ USD %.2f", amount * rate);
        } else {
            return "";
        }
    }

    public String getAmountEquivalentDescriptor(TangemCard card, String value) {

        return getAmountEquivalentDescriptionBTC(Double.parseDouble(value), btcData.getRate());
    }

    @Override
    public byte[] sign(String feeValue, String amountValue, boolean IncFee, String toValue, CardProtocol protocol) throws Exception {

        checkBlockchainDataExists();

        String myAddress = ctx.getCard().getWallet();
        byte[] pbKey = ctx.getCard().getWalletPublicKey();
        String outputAddress = toValue;
        String changeAddress = myAddress;

        // Build script for our address
        List<BtcData.UnspentTransaction> rawTxList = btcData.getUnspentTransactions();
        byte[] outputScriptWeAreAbleToSpend = Transaction.Script.buildOutput(myAddress).bytes;

        // Collect unspent
        ArrayList<UnspentOutputInfo> unspentOutputs = BTCUtils.getOutputs(rawTxList, outputScriptWeAreAbleToSpend);

        long fullAmount = 0;
        for (int i = 0; i < unspentOutputs.size(); ++i) {
            fullAmount += unspentOutputs.get(i).value;
        }


        long fees = FormatUtil.ConvertStringToLong(feeValue);
        long amount = FormatUtil.ConvertStringToLong(amountValue);
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
            byte[] newTX = BTCUtils.buildTXForSign(myAddress, outputAddress, changeAddress, unspentOutputs, i, amount, change);

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

            unspentOutputs.get(i).scriptForBuild = DerEncodingUtil.packSignDer(r, s, pbKey);
        }

        return BTCUtils.buildTXForSend(outputAddress, changeAddress, unspentOutputs, amount, change);
    }

}