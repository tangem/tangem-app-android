package com.tangem.wallet;

import android.net.Uri;
import android.util.Log;

import com.google.common.base.Strings;
import com.tangem.cardReader.CardProtocol;
import com.tangem.cardReader.TLV;

import org.bitcoinj.core.ECKey;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Date;

import static com.tangem.wallet.FormatUtil.GetDecimalFormat;

/**
 * Created by Ilia on 20.03.2018.
 */

public class TokenEngine extends CoinEngine{
    public String GetNextNode(Tangem_Card mCard)
    {
        return "abc1.hsmiths.com";
    }
    public int GetNextNodePort(Tangem_Card mCard)
    {
        return 60001;
    }
    public String GetNode(Tangem_Card mCard)
    {
        return "abc1.hsmiths.com";
    }
    public int GetNodePort(Tangem_Card mCard)
    {
        return 60001;
    }
    public void SwitchNode(Tangem_Card mCard)
    {
    }
    public boolean AwaitingConfirmation(Tangem_Card card)
    {
        return false;
    }

    public boolean InOutPutVisible()
    {
        return false;
    }

    public String GetBalanceCurrency(Tangem_Card card)
    {
        String currency = card.getTokenSymbol();
        if(Strings.isNullOrEmpty(currency))
            return "NoN";
        return currency;
    }

    public String GetFeeCurrency()
    {
        return "Gwei";
    }

    BigDecimal convertToEth(String value)
    {
        BigInteger m = new BigInteger(value, 10);
        BigDecimal n = new BigDecimal(m);
        BigDecimal d = n.divide(new BigDecimal("1000000000000000000"));
        d = d.setScale(8, RoundingMode.DOWN);
        return d;
    }


    public int GetTokenDecimals(Tangem_Card card)
    {
        return card.getTokensDecimal();
    }

    public String GetContractAddress(Tangem_Card card)
    {
        return card.getContractAddress();
    }
    public boolean IsNeedCheckNode()
    {
        return false;
    }

    public boolean ValdateAddress(String address, Tangem_Card card) {
        if (address == null || address.isEmpty()) {
            return false;
        }

        if(!address.startsWith("0x")&&!address.startsWith("0X"))
        {
            return false;
        }

        if(address.length()!=42)
        {
            return false;
        }

        return true;
    }

    public String GetBalanceAlterValue(Tangem_Card mCard)
    {
        String dec = mCard.getDecimalBalanceAlter();
        BigDecimal d = convertToEth(dec);
        String s = d.toString();

        String pattern = "#0.000"; // If you like 4 zeros
        DecimalFormat myFormatter = new DecimalFormat(pattern);
        String output = myFormatter.format(d);
        return output;
    }

    public String GetBalanceValue(Tangem_Card mCard)
    {
        if(!HasBalanceInfo(mCard))
            return "-- -- -- " + GetBalanceCurrency(mCard);

        String dec = mCard.getDecimalBalance();
        BigDecimal d = new BigDecimal(dec);
        BigDecimal p = new BigDecimal(10);
        p = p.pow(GetTokenDecimals(mCard));
        BigDecimal l = d.divide(p);

        String pattern = "#0.000"; // If you like 4 zeros
        DecimalFormat myFormatter = new DecimalFormat(pattern);
        String output = myFormatter.format(l);
        return output;
    }

    public boolean CheckAmount(Tangem_Card card, String amount) throws Exception
    {
        DecimalFormat decimalFormat = GetDecimalFormat();
        BigDecimal amountValue = (BigDecimal) decimalFormat.parse(amount); //new BigDecimal(strAmount);
        BigDecimal maxValue = new BigDecimal(GetBalanceValue(card));
        if(amountValue.compareTo(maxValue) > 0 )
        {
            return false;
        }

        return true;
    }

    public Long GetBalanceLong(Tangem_Card mCard)
    {
        return mCard.getBalance();
    }

    public boolean IsBalanceAlterNotZero(Tangem_Card card)
    {
        String balance = card.getDecimalBalanceAlter();
        if(balance == null || balance == "")
            return false;

        BigDecimal bi = new BigDecimal(balance);

        if (BigDecimal.ZERO.compareTo(bi) == 0)
            return false;

        return true;
    }

    public boolean IsBalanceNotZero(Tangem_Card card)
    {
        String balance = card.getDecimalBalance();
        if(balance == null || balance == "")
            return false;

        BigDecimal bi = new BigDecimal(balance);

        if (BigDecimal.ZERO.compareTo(bi) == 0)
            return false;

        return true;
    }

    public boolean HasBalanceInfo(Tangem_Card card)
    {
        String balance = card.getDecimalBalance();
        if(balance == null || balance == "")
            return false;

        String balanceEx = card.getDecimalBalanceAlter();
        if(balanceEx == null || balanceEx == "")
            return false;
        return true;
    }

    @Override
    public String GetBalanceEquivalent(Tangem_Card mCard) {
        if(!HasBalanceInfo(mCard)){
            return "-- -- -- ";
        }
        String dec = mCard.getDecimalBalance();
        BigDecimal d = convertToEth(dec);
        return EthEngine.getAmountEquivalentDescriptionETH(d, mCard.getRate());
    }

    @Override
    public String GetBalance(Tangem_Card mCard) {
        if(!HasBalanceInfo(mCard)){
            return "-- -- -- " + GetBalanceCurrency(mCard);
        }

        String output = GetBalanceValue(mCard);
        String s = output + " " + GetBalanceCurrency(mCard);
        return s;
    }




    public String GetBalanceWithAlter(Tangem_Card mCard)
    {
        //return GetBalance(mCard) + "\n(" + GetBalanceAlterValue(mCard) + " ETH)";
        return " " + GetBalance(mCard) + " <br><small><small>  + " + GetBalanceAlterValue(mCard) + " ETH for gas</small></small>";
    }

    public  String calculateAddress(Tangem_Card mCard, byte[] pkUncompressed) throws NoSuchProviderException, NoSuchAlgorithmException {
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
    public String ConvertByteArrayToAmount(Tangem_Card mCard, byte[] bytes) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public byte[] ConvertAmountToByteArray(Tangem_Card mCard, String amount) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public String GetAmountDescription(Tangem_Card mCard, String amount) throws Exception {
        throw new Exception("Not implemented");
    }


    public String GetAmountEqualentDescriptor(Tangem_Card mCard, String value)
    {
        BigDecimal d = new BigDecimal(value);
        return EthEngine.getAmountEquivalentDescriptionETH(d, mCard.getRate());
    }

    public String GetFeeEqualentDescriptor(Tangem_Card mCard, String value)
    {
        BigDecimal d = new BigDecimal(value);
        return EthEngine.getAmountEquivalentDescriptionETH(d, mCard.getRateAlter());
    }

    public Uri getShareWalletURIExplorer(Tangem_Card mCard)
    {
        return Uri.parse("https://etherscan.io/token/"+GetContractAddress(mCard)+"?a=" + mCard.getWallet());
    }

    public Uri getShareWalletURI(Tangem_Card mCard)
    {
        return Uri.parse("" + mCard.getWallet());
    }

    public boolean CheckUnspentTransaction(Tangem_Card mCard)
    {
        return true;
    }

    public boolean CheckAmountValie(Tangem_Card mCard, String amountValue, String feeValue, Long minFeeInInternalUnits)
    {
        Long fee = null;
        BigDecimal amount = null;
        try {
            amount = new BigDecimal(GetBalanceAlterValue(mCard));//mCard.InternalUnitsFromString(amountValue);
            fee = mCard.InternalUnitsFromString(feeValue);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        if(fee == null || amount == null)
            return false;

        if(fee == 0 || amount.compareTo(BigDecimal.ZERO) == 0)
            return false;


        if(fee < minFeeInInternalUnits)
            return false;


        BigDecimal tmpFee = new BigDecimal(feeValue);
        BigDecimal tmpAmount = amount;
        tmpAmount = tmpAmount.multiply(new BigDecimal("1000000000"));

        if (tmpFee.compareTo(tmpAmount) > 0)
            return false;

        return true;
    }

    public String EvaluteFeeEquivalent(Tangem_Card mCard, String fee)
    {
        BigDecimal gweFee = new BigDecimal(fee);
        gweFee = gweFee.divide(new BigDecimal("1000000000"));
        gweFee = gweFee.setScale(18, RoundingMode.DOWN);
        return GetFeeEqualentDescriptor(mCard, gweFee.toString());
    }

    public byte[] Sign(String feeValue, String amountValue, String toValue, Tangem_Card mCard, CardProtocol protocol) throws Exception {

        BigInteger nonceValue = mCard.GetConfirmTXCount();
        byte[] pbKey = mCard.getWalletPublicKey();
        boolean flag = (mCard.getSigningMethod()== Tangem_Card.SigningMethod.Sign_Hash_Validated_By_Issuer);
        Issuer issuer = mCard.getIssuer();


        BigInteger fee = new BigInteger(feeValue, 10);

        BigDecimal amountDecValue = new BigDecimal(amountValue);

        int d = GetTokenDecimals(mCard);
        BigDecimal amountDec = new BigDecimal("10");
        amountDec = amountDec.pow(d);
        amountDec = amountDecValue.multiply(amountDec);

        //amountDec = amountDec.multiply(new BigDecimal("1000000000"));


        BigInteger amount = amountDec.toBigInteger(); //new BigInteger(amountValue, 10);




        //amount = amount.subtract(fee);

        BigInteger nonce = nonceValue;
        BigInteger gasPrice = fee.divide(BigInteger.valueOf(21000));
        BigInteger gasLimit = BigInteger.valueOf(60000);
        Integer chainId = ETH_Transaction.ChainEnum.Mainnet.getValue();
        BigInteger amountZero = BigInteger.ZERO;

        Long multiplicator = 1000000000L;

        gasPrice = gasPrice.multiply(BigInteger.valueOf(multiplicator));

        String to = toValue;

        if (to.startsWith("0x") || to.startsWith("0X")) {
            to = to.substring(2);
        }

        String contractAddress = GetContractAddress(mCard);

        if (contractAddress.startsWith("0x") || contractAddress.startsWith("0X")) {
            contractAddress = contractAddress.substring(2);
        }

        String amountLeadZero = amount.toString(16);
        if (amountLeadZero.startsWith("0x") || amountLeadZero.startsWith("0X")) {
            amountLeadZero = amountLeadZero.substring(2);
        }

        while(amountLeadZero.length() < 64)
        {
            amountLeadZero = "0" + amountLeadZero;
        }

        String cmd = "a9059cbb000000000000000000000000"+to+amountLeadZero; //TODO only for BAT


        byte[] data = BTCUtils.fromHex(cmd);
        ETH_Transaction tx = ETH_Transaction.create(contractAddress, amountZero, nonce, gasPrice, gasLimit, chainId, data);

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

        LastSignStorage.setLastSignDate(mCard.getWallet(), new Date());

        BigInteger r = new BigInteger(1, Arrays.copyOfRange(signFromCard, 0, 32));
        BigInteger s = new BigInteger(1, Arrays.copyOfRange(signFromCard, 32, 64));
        s = CryptoUtil.toCanonicalised(s);

        boolean f = ECKey.verify(for_hash, new ECKey.ECDSASignature(r, s), pbKey);

        if(!f)
        {
            Log.e("ETH-CHECK", "Sign Failed.");
        }

        tx.signature = new ECDSASignature_ETH(r, s);
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
