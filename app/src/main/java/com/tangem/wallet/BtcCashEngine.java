package com.tangem.wallet;

import android.net.Uri;

import com.tangem.domain.cardReader.CardProtocol;
import com.tangem.domain.cardReader.TLV;
import com.tangem.domain.cardReader.Util;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.tangem.wallet.FormatUtil.GetDecimalFormat;

/**
 * Created by Ilia on 15.02.2018.
 */

public class BtcCashEngine extends CoinEngine{
    public String GetNextNode(Tangem_Card mCard)
    {

        return "35.157.238.5";
    }
    public int GetNextNodePort(Tangem_Card mCard)
    {

        return 51001;
    }
    public String GetNode(Tangem_Card mCard)
    {
        return "35.157.238.5";
    }
    public int GetNodePort(Tangem_Card mCard)
    {
        return 51001;
    }

    public void SwitchNode(Tangem_Card mCard)
    {
    }

    public boolean InOutPutVisible()
    {
        return true;
    }

    public boolean AwaitingConfirmation(Tangem_Card card)
    {
        return card.getBalanceUnconfirmed()!=0;
    }

    public String GetBalanceWithAlter(Tangem_Card mCard)
    {
        return GetBalance(mCard);
    }

    public boolean IsBalanceAlterNotZero(Tangem_Card card)
    {
        return true;
    }

    public Long GetBalanceLong(Tangem_Card mCard)
    {
        return mCard.getBalance();
    }

    public boolean IsBalanceNotZero(Tangem_Card card)
    {
        return card.getBalance() > 0;
    }

    public boolean CheckAmount(Tangem_Card card, String amount) throws Exception
    {
        DecimalFormat decimalFormat = GetDecimalFormat();
        BigDecimal amountValue = (BigDecimal) decimalFormat.parse(amount);

        // Convert Balance to BigDecimal
        BigDecimal maxValue = new BigDecimal(GetBalanceValue(card));
        maxValue = maxValue.divide(new BigDecimal(1000));

        //if (use_mCurrency) {
        amountValue = amountValue.divide(new BigDecimal(1000));
        //}

        if (amountValue.compareTo(maxValue) > 0) {
            return false;
        }

        return true;
    }

    public boolean HasBalanceInfo(Tangem_Card card)
    {
        return card.hasBalanceInfo();
    }

    public String GetBalanceCurrency(Tangem_Card card)
    {
        return "mBCH";
    }

    public boolean CheckUnspentTransaction(Tangem_Card mCard)
    {
        return mCard.getUnspentTransactions().size() != 0;
    }

    public String GetFeeCurrency()
    {
        return "mBCH";
    }

    public boolean ValdateAddress(String address, Tangem_Card card){
        if(address == null || address.isEmpty())
        {
            return false;
        }

        if(address.length() < 25)
        {
            return false;
        }

        if(address.length() > 35)
        {
            return false;
        }

        if(!address.startsWith("1") && !address.startsWith("2") && !address.startsWith("3") && !address.startsWith("n") && !address.startsWith("m"))
        {
            return false;
        }

        byte[] decAddress = Base58.decodeBase58(address);

        if(decAddress == null || decAddress.length == 0)
        {
            return false;
        }

        byte[] rip = new byte[21];
        for(int i =0; i < 21; ++i)
        {
            rip[i] = decAddress[i];
        }

        byte[] kcv = CryptoUtil.doubleSha256(rip);

        for(int i =0; i < 4; ++i)
        {
            if(kcv[i] != decAddress[21+i])
                return false;
        }

        if(card.getBlockchain()!=Blockchain.BitcoinCashTestNet && card.getBlockchain()!=Blockchain.BitcoinCash)
        {
            return false;
        }

        if(card.getBlockchain()==Blockchain.BitcoinCashTestNet && (address.startsWith("1") || address.startsWith("3")))
        {
            return false;
        }

        return true;
    }


    public int GetTokenDecimals(Tangem_Card card)
    {
        return 0;
    }

    public String GetContractAddress(Tangem_Card card)
    {
        return "";
    }

    public boolean IsNeedCheckNode()
    {
        return true;
    }

    public Uri getShareWalletURIExplorer(Tangem_Card mCard)
    {
        return Uri.parse((mCard.getBlockchain() == Blockchain.BitcoinCash ? "https://bitcoincash.blockexplorer.com/address/" : "https://testnet.blockexplorer.com/address/") + mCard.getWallet());
    }
    public Uri getShareWalletURI(Tangem_Card mCard)
    {
        return Uri.parse("bitcoincash:" + mCard.getWallet());
    }
    public boolean CheckAmountValie(Tangem_Card mCard, String amountValue, String feeValue, Long minFeeInInternalUnits)
    {
        Long fee = null;
        Long amount = null;
        try {
            amount = mCard.InternalUnitsFromString(amountValue);
            fee = mCard.InternalUnitsFromString(feeValue);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        if(fee == null || amount == null)
            return false;

        if(fee == 0 || amount ==0)
            return false;

        if(fee > amount)
            return false;

        if(fee < minFeeInInternalUnits)
            return false;

        return true;
    }

    public String EvaluteFeeEquivalent(Tangem_Card mCard, String fee)
    {
        return GetAmountEqualentDescriptor(mCard, fee);
    }

    @Override
    public String GetBalanceEquivalent(Tangem_Card mCard) {
        Double balance = Double.NaN;
        try{
            Long val = mCard.getBalance();
            balance = mCard.AmountFromInternalUnits(val);
        }
        catch(Exception ex)
        {
            mCard.setRate(0);
        }

        return mCard.getAmountEquivalentDescription(balance);
    }

    public String GetBalance(Tangem_Card mCard)
    {
        if (mCard.hasBalanceInfo()) {
            Double balance = mCard.AmountFromInternalUnits(mCard.getBalance());
            return mCard.getAmountDescription(balance);
        } else {
            return "-- -- -- " + mCard.getBlockchain().getCurrency();
        }
    }

    public String GetBalanceValue(Tangem_Card mCard)
    {
        if (mCard.hasBalanceInfo()) {
            Double balance = mCard.getBalance() / (mCard.getBlockchain().getMultiplier() / 1000.0);

            String output = FormatUtil.DoubleToString(balance);
            //String pattern = "#0.000"; // If you like 4 zeros
            //DecimalFormat myFormatter = new DecimalFormat(pattern);
            //String output = myFormatter.format(balance);
            return output;

            //return Double.toString(balance);
        }
        else
        {
            return "0";
        }
    }

    public String calculateAddress(Tangem_Card mCard, byte[] pkUncompressed) throws NoSuchProviderException, NoSuchAlgorithmException {

        byte netSelectionByte;
        switch (mCard.getBlockchain()) {
            case BitcoinCash:
                netSelectionByte = (byte) 0x00; //0 - MainNet 0x6f - TestNet
                break;
            default :
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
    public String ConvertByteArrayToAmount(Tangem_Card mCard, byte[] bytes) throws Exception {
        byte[] reversed=new byte[bytes.length];
        for(int i=0; i<bytes.length; i++) reversed[i]=bytes[bytes.length-i-1];
        return FormatUtil.DoubleToString(1000.0*mCard.AmountFromInternalUnits(Util.byteArrayToLong(reversed)));
    }

    @Override
    public byte[] ConvertAmountToByteArray(Tangem_Card mCard, String amount) throws Exception {
        byte[] bytes=Util.longToByteArray(mCard.InternalUnitsFromString(amount));
        byte[] reversed=new byte[bytes.length];
        for(int i=0; i<bytes.length; i++) reversed[i]=bytes[bytes.length-i-1];
        return reversed;
    }

    @Override
    public String GetAmountDescription(Tangem_Card mCard, String amount) throws Exception {
        return mCard.getAmountDescription(Double.parseDouble(amount)/1000.0);
    }

    public static String getAmountEquivalentDescriptionBTC(Double amount, float rate) {
        if (rate > 0) {
            return String.format("≈ USD %.2f", amount * rate);
        } else {
            return "≈ USD  ---";
        }
    }

    public String GetAmountEqualentDescriptor(Tangem_Card mCard, String value)
    {
        return getAmountEquivalentDescriptionBTC(Double.parseDouble(value)/1000.0, mCard.getRate());
    }

    public byte[] Sign(String feeValue, String amountValue, String toValue, Tangem_Card mCard, CardProtocol protocol) throws Exception {

        String myAddress = mCard.getWallet();
        byte[] pbKey = mCard.getWalletPublicKeyRar(); //ALWAYS USING COMPRESS KEY
        String outputAddress = toValue;
        String changeAddress = myAddress;

        // Build script for our address
        List<Tangem_Card.UnspentTransaction> rawTxList = mCard.getUnspentTransactions();
        byte[] outputScriptWeAreAbleToSpend = Transaction.Script.buildOutput(myAddress).bytes;

        // Collect unspent
        ArrayList<UnspentOutputInfo> unspentOutputs = BTCUtils.getOutputs(rawTxList, outputScriptWeAreAbleToSpend);

        long fullAmount = 0;
        for (int i = 0; i < unspentOutputs.size(); ++i) {
            fullAmount += unspentOutputs.get(i).value;
        }


        long fees = FormatUtil.ConvertStringToLong(feeValue);
        long amount = FormatUtil.ConvertStringToLong(amountValue);
        amount = amount - fees;

        long change = fullAmount - fees - amount;

        if (amount + fees > fullAmount) {
            throw new Exception(String.format("Balance (%d) < amount (%d) + (%d)", fullAmount, change, amount));
        }

        byte[][] dataForSign = new byte[unspentOutputs.size()][];

        for (int i = 0; i < unspentOutputs.size(); ++i) {
            byte[] newTX = BTCUtils.buildTXForSign(myAddress, outputAddress, changeAddress, unspentOutputs, i, amount, change);

            byte[] hashData = Util.calculateSHA256(newTX);
            byte[] doubleHashData = Util.calculateSHA256(hashData);

            unspentOutputs.get(i).bodyDoubleHash = doubleHashData;
            unspentOutputs.get(i).bodyHash = hashData;

            if(mCard.getSigningMethod() == Tangem_Card.SigningMethod.Sign_Raw || mCard.getSigningMethod() == Tangem_Card.SigningMethod.Sign_Raw_Validated_By_Issuer)
            {
                dataForSign[i] = newTX;
            }
            else
            {
                dataForSign[i] = doubleHashData;
            }

        }

        byte[] signFromCard = null;
        if(mCard.getSigningMethod() == Tangem_Card.SigningMethod.Sign_Raw || mCard.getSigningMethod() == Tangem_Card.SigningMethod.Sign_Raw_Validated_By_Issuer)
        {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            if (dataForSign.length > 10) throw new Exception("To much hashes in one transaction!");
            for (int i = 0; i < dataForSign.length; i++) {
                if (i != 0 && dataForSign[0].length != dataForSign[i].length)
                    throw new Exception("Hashes length must be identical!");
                bs.write(dataForSign[i]);
            }
            signFromCard = protocol.run_SignRaw(PINStorage.getPIN2(), bs.toByteArray()).getTLV(TLV.Tag.TAG_Signature).Value;
        }
        else {
            signFromCard = protocol.run_SignHashes(PINStorage.getPIN2(), dataForSign, mCard.getSigningMethod() == Tangem_Card.SigningMethod.Sign_Hash_Validated_By_Issuer, null, mCard.getIssuer()).getTLV(TLV.Tag.TAG_Signature).Value;
            // TODO slice signFromCard to hashes.length parts
        }

        LastSignStorage.setLastSignDate(mCard.getWallet(), new Date());


        for (int i = 0; i < unspentOutputs.size(); ++i) {
            BigInteger r = new BigInteger(1, Arrays.copyOfRange(signFromCard, 0 + i * 64, 32 + i * 64));
            BigInteger s = new BigInteger(1, Arrays.copyOfRange(signFromCard, 32 + i * 64, 64 + i * 64));
            s = CryptoUtil.toCanonicalised(s);
            byte[] encodingSign = DerEncodingUtil.packSignDerBitcoinCash(r, s, pbKey);

            unspentOutputs.get(i).scriptForBuild = encodingSign;
        }

        byte[] realTX = BTCUtils.buildTXForSend(outputAddress, changeAddress, unspentOutputs, amount, change);
        return realTX;
    }
}
