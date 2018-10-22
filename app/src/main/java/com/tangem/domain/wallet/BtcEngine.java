package com.tangem.domain.wallet;

import android.net.Uri;

import com.tangem.domain.BitcoinNode;
import com.tangem.domain.BitcoinNodeTestNet;
import com.tangem.domain.cardReader.CardProtocol;
import com.tangem.domain.cardReader.TLV;
import com.tangem.util.BTCUtils;
import com.tangem.util.CryptoUtil;
import com.tangem.util.DerEncodingUtil;
import com.tangem.util.FormatUtil;
import com.tangem.util.Util;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.tangem.util.FormatUtil.stringToBigDecimal;

public class BtcEngine extends CoinEngine {

    private static String[] getBitcoinServiceHosts() {
        return new String[]{
                BitcoinNode.n13.getHost(),
                BitcoinNode.n14.getHost(),
                BitcoinNode.n15.getHost(),
                BitcoinNode.n16.getHost(),
//                BitcoinNode.n17.getHost(),
//                BitcoinNode.n18.getHost(),
        };
    }

    private static Integer[] getBitcoinServicePorts() {
        return new Integer[]{
                BitcoinNode.n13.getPort(),
                BitcoinNode.n14.getPort(),
                BitcoinNode.n15.getPort(),
                BitcoinNode.n16.getPort(),
//                BitcoinNode.n17.getPort(),
//                BitcoinNode.n18.getPort(),

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

    public String getNode(TangemCard card) {
        switch (card.getBlockchain()) {
            case Bitcoin:
                return getBitcoinServiceHosts()[serviceIndex]; //
        }
        return null;
    }

    public int getNodePort(TangemCard card) {
        switch (card.getBlockchain()) {
            case Bitcoin:
                return getBitcoinServicePorts()[serviceIndex];//8080;
        }
        return 8080;
    }

    public void switchNode(TangemCard card) {
        serviceIndex++;
        if (serviceIndex > getBitcoinServiceHosts().length - 1) serviceIndex = 0;
    }

    public boolean inOutPutVisible() {
        return true;
    }

    public boolean awaitingConfirmation(TangemCard card) {
        return card.getBalanceUnconfirmed() != 0;
    }

    public String getBalanceWithAlter(TangemCard card) {
        return getBalance(card);
    }

    public boolean isBalanceAlterNotZero(TangemCard card) {
        return true;
    }

    public Long getBalanceLong(TangemCard card) {
        return card.getBalance();
    }

    public boolean isBalanceNotZero(TangemCard card) {
        if (card.getBalance() == null) return false;
        return card.getBalance() > 0;
    }

    public boolean checkAmount(TangemCard card, String amount) throws Exception {
//        DecimalFormat decimalFormat = GetDecimalFormat();
//        BigDecimal amountValue = (BigDecimal) decimalFormat.parse(amount);
        BigDecimal amountValue = stringToBigDecimal(amount, Locale.US);
        BigDecimal maxValue = stringToBigDecimal(getBalanceValue(card), Locale.US);
        if (amountValue.compareTo(maxValue) > 0) {
            return false;
        }
        return true;
    }

    public boolean hasBalanceInfo(TangemCard card) {
        return card.hasBalanceInfo();
    }

    public String getBalanceCurrency(TangemCard card) {
        return "BTC";
    }

    public boolean checkUnspentTransaction(TangemCard card) {
        return card.getUnspentTransactions().size() != 0;
    }

    public String getFeeCurrency() {
        return "BTC";
    }

    public boolean validateAddress(String address, TangemCard card) {
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

        if (card.getBlockchain() != Blockchain.BitcoinTestNet && card.getBlockchain() != Blockchain.Bitcoin) {
            return false;
        }

        if (card.getBlockchain() == Blockchain.BitcoinTestNet && (address.startsWith("1") || address.startsWith("3"))) {
            return false;
        }

        return true;
    }


    public int getTokenDecimals(TangemCard card) {
        return 0;
    }

    public String getContractAddress(TangemCard card) {
        return "";
    }

    public boolean isNeedCheckNode() {
        return true;
    }

    public Uri getShareWalletUriExplorer(TangemCard card) {
        return Uri.parse((card.getBlockchain() == Blockchain.Bitcoin ? "https://blockchain.info/address/" : "https://testnet.blockchain.info/address/") + card.getWallet());
    }

    public Uri getShareWalletUri(TangemCard card) {
        if (card.getDenomination() != null) {
            return Uri.parse("bitcoin:" + card.getWallet() + "?amount=" + BTCUtils.satoshiToBtc(card.getDenomination()));
        } else {
            return Uri.parse("bitcoin:" + card.getWallet());
        }
    }


    public boolean checkAmountValue(TangemCard card, String amountValue, String feeValue, Long minFeeInInternalUnits, Boolean isIncludeFee) {
        Long fee;
        Long amount;
        try {
            amount = card.internalUnitsFromString(amountValue);
            fee = card.internalUnitsFromString(feeValue);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        if (fee == null || amount == null)
            return false;

        if (fee == 0 || amount == 0)
            return false;

        if (isIncludeFee && amount > card.getBalance())
            return false;

        if (!isIncludeFee && amount + fee > card.getBalance())
            return false;

        return true;
    }

    public String evaluateFeeEquivalent(TangemCard card, String fee) {
        return getAmountEquivalentDescriptor(card, fee);
    }

    @Override
    public String getBalanceEquivalent(TangemCard card) {
        Double balance = Double.NaN;
        try {
            Long val = card.getBalance();
            balance = card.amountFromInternalUnits(val);
        } catch (Exception ex) {
            card.setRate(0);
        }

        return card.getAmountEquivalentDescription(balance);
    }

    public String getBalance(TangemCard card) {
        if (card.hasBalanceInfo()) {
            Double balance = card.amountFromInternalUnits(card.getBalance());
            return card.getAmountDescription(balance);
        } else {
            return "";
        }
    }

    public String getBalanceValue(TangemCard card) {
        if (card.hasBalanceInfo()) {
            Double balance = card.getBalance() / (card.getBlockchain().getMultiplier());

            String output = FormatUtil.DoubleToString(balance);
            //String pattern = "#0.000"; // If you like 4 zeros
            //DecimalFormat myFormatter = new DecimalFormat(pattern);
            //String output = myFormatter.format(balance);
            return output;

            //return Double.toString(balance);
        } else {
            return "0";
        }
    }

    public String calculateAddress(TangemCard card, byte[] pkUncompressed) throws NoSuchProviderException, NoSuchAlgorithmException {
        byte netSelectionByte;
        switch (card.getBlockchain()) {
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
    public String convertByteArrayToAmount(TangemCard card, byte[] bytes) throws Exception {
        if (bytes == null) return "";
        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) reversed[i] = bytes[bytes.length - i - 1];
        return FormatUtil.DoubleToString(card.amountFromInternalUnits(Util.byteArrayToLong(reversed)));
    }

    @Override
    public byte[] convertAmountToByteArray(TangemCard card, String amount) throws Exception {
        byte[] bytes = Util.longToByteArray(card.internalUnitsFromString(amount));
        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) reversed[i] = bytes[bytes.length - i - 1];
        return reversed;
    }

    @Override
    public String getAmountDescription(TangemCard card, String amount) throws Exception {
        return card.getAmountDescription(Double.parseDouble(amount));
    }

    public static String getAmountEquivalentDescriptionBTC(Double amount, float rate) {
        if ((rate > 0) && (amount > 0)) {
            return String.format("≈ USD %.2f", amount * rate);
        } else {
            return "";
        }
    }

    public String getAmountEquivalentDescriptor(TangemCard card, String value) {
        return getAmountEquivalentDescriptionBTC(Double.parseDouble(value), card.getRate());
    }

    public byte[] sign(String feeValue, String amountValue, boolean IncFee, String toValue, TangemCard card, CardProtocol protocol) throws Exception {

        String myAddress = card.getWallet();
        byte[] pbKey = card.getWalletPublicKey();
        String outputAddress = toValue;
        String changeAddress = myAddress;

        // Build script for our address
        List<TangemCard.UnspentTransaction> rawTxList = card.getUnspentTransactions();
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

            if (card.getSigningMethod() == TangemCard.SigningMethod.Sign_Raw || card.getSigningMethod() == TangemCard.SigningMethod.Sign_Raw_Validated_By_Issuer) {
                dataForSign[i] = newTX;
            } else {
                dataForSign[i] = doubleHashData;
            }
        }

        byte[] signFromCard;
        if (card.getSigningMethod() == TangemCard.SigningMethod.Sign_Raw || card.getSigningMethod() == TangemCard.SigningMethod.Sign_Raw_Validated_By_Issuer) {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            if (dataForSign.length > 10) throw new Exception("To much hashes in one transaction!");
            for (int i = 0; i < dataForSign.length; i++) {
                if (i != 0 && dataForSign[0].length != dataForSign[i].length)
                    throw new Exception("Hashes length must be identical!");
                bs.write(dataForSign[i]);
            }
            signFromCard = protocol.run_SignRaw(PINStorage.getPIN2(), bs.toByteArray()).getTLV(TLV.Tag.TAG_Signature).Value;
        } else {
            signFromCard = protocol.run_SignHashes(PINStorage.getPIN2(), dataForSign, card.getSigningMethod() == TangemCard.SigningMethod.Sign_Hash_Validated_By_Issuer, null, card.getIssuer()).getTLV(TLV.Tag.TAG_Signature).Value;
            // TODO slice signFromCard to hashes.length parts
        }

        for (int i = 0; i < unspentOutputs.size(); ++i) {
            BigInteger r = new BigInteger(1, Arrays.copyOfRange(signFromCard, 0 + i * 64, 32 + i * 64));
            BigInteger s = new BigInteger(1, Arrays.copyOfRange(signFromCard, 32 + i * 64, 64 + i * 64));
            s = CryptoUtil.toCanonicalised(s);
            byte[] encodingSign = DerEncodingUtil.packSignDer(r, s, pbKey);

            unspentOutputs.get(i).scriptForBuild = encodingSign;
        }

        byte[] realTX = BTCUtils.buildTXForSend(outputAddress, changeAddress, unspentOutputs, amount, change);
        return realTX;
    }

}