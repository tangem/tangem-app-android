package com.tangem.wallet;

/**
 * Created by Ilia on 29.09.2017.
 */

import android.util.Log;

import com.tangem.domain.cardReader.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

@SuppressWarnings({"WeakerAccess", "TryWithIdenticalCatches", "unused"})
public final class BTCUtils {
    static final BigInteger LARGEST_PRIVATE_KEY = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);//SECP256K1_N
    public static final long MIN_FEE_PER_KB = 10000;
    public static final long MAX_ALLOWED_FEE = FormatUtil.parseValue("0.1");
    public static final long MIN_PRIORITY_FOR_NO_FEE = 57600000;
    public static final long MIN_MIN_OUTPUT_VALUE_FOR_NO_FEE = 10000000L;
    public static final int MAX_TX_LEN_FOR_NO_FEE = 10000;
    public static final float EXPECTED_BLOCKS_PER_DAY = 144.0f;//(expected confirmations per day)

    public static long calcMinimumFee(int txLen, Collection<UnspentOutputInfo> unspentOutputInfos, long minOutput) {
        if (isZeroFeeAllowed(txLen, unspentOutputInfos, minOutput)) {
            return 0;
        }
        return MIN_FEE_PER_KB * (1 + txLen / 1000);
    }

    public static boolean isZeroFeeAllowed(int txLen, Collection<UnspentOutputInfo> unspentOutputInfos, long minOutput) {
        if (txLen < MAX_TX_LEN_FOR_NO_FEE && minOutput > MIN_MIN_OUTPUT_VALUE_FOR_NO_FEE) {
            long priority = 0;
            for (UnspentOutputInfo output : unspentOutputInfos) {
                if (output.confirmations > 0) {
                    priority += output.confirmations * output.value;
                }
            }
            priority /= txLen;
            if (priority > MIN_PRIORITY_FOR_NO_FEE) {
                return true;
            }
        }
        return false;
    }

    public static int getMaximumTxSize(Collection<UnspentOutputInfo> unspentOutputInfos, int outputsCount, boolean compressedPublicKey) throws BitcoinException {
        if (unspentOutputInfos == null || unspentOutputInfos.isEmpty()) {
            throw new BitcoinException(BitcoinException.ERR_NO_INPUT, "No information about tx inputs provided");
        }
        int maxInputScriptLen = 73 + (compressedPublicKey ? 33 : 65);
        return 9 + unspentOutputInfos.size() * (41 + maxInputScriptLen) + outputsCount * 33;
    }

    public static String publicKeyToAddress(byte[] publicKey) {
        return publicKeyToAddress(false, publicKey);
    }

    public static String publicKeyToAddress(boolean testNet, byte[] publicKey) {
        try {
            byte[] hashedPublicKey = CryptoUtil.sha256ripemd160(publicKey);
            byte[] addressBytes = new byte[1 + hashedPublicKey.length + 4];
            addressBytes[0] = (byte) (testNet ? 111 : 0);
            System.arraycopy(hashedPublicKey, 0, addressBytes, 1, hashedPublicKey.length);
            MessageDigest digestSha = MessageDigest.getInstance("SHA-256");
            digestSha.update(addressBytes, 0, addressBytes.length - 4);
            byte[] check = digestSha.digest(digestSha.digest());
            System.arraycopy(check, 0, addressBytes, hashedPublicKey.length + 1, 4);
            return Base58.encodeBase58(addressBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toHex(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


    public static byte[] buildTXForSign(String myAddress, String outputAddress, String changeAddress, ArrayList<UnspentOutputInfo> unspentOutputs, int currentInputPos, long amount, long change) throws BitcoinException, IOException {
        byte[] myScript = Transaction.Script.buildOutput(myAddress).bytes;
        unspentOutputs.get(currentInputPos).scriptForBuild = myScript;
        int inputPos = currentInputPos;
        byte[] body = buildBodyTX(outputAddress, changeAddress, unspentOutputs, inputPos, amount, change);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(body);
        os.write(new byte[]{0x01, 0x00, 0x00, 0x00});
        byte[] tx = os.toByteArray();
        return tx;
    }

    public static byte[] buildTXForSend(String outputAddress, String changeAddress, ArrayList<UnspentOutputInfo> unspentOutputs, long amount, long change) throws BitcoinException, IOException {
        int inputPos = -1;
        byte[] body = buildBodyTX(outputAddress, changeAddress, unspentOutputs, inputPos, amount, change);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(body);
        byte[] tx = os.toByteArray();
        return tx;
    }

    public static byte[] buildBodyTX(String outputAddress, String changeAddress, ArrayList<UnspentOutputInfo> unspentOutputs, int inputPos, long amount, long change) throws BitcoinException, IOException {

        //0200000000
        BitcoinOutputStream forSign = new BitcoinOutputStream();
        forSign.writeInt32(0x01);//write(new byte[]{0x02, 0x00, 0x00, 0x00}); // version

        //01
        byte inputCount = (byte)unspentOutputs.size();
        forSign.write(inputCount); // input count
        //hex str hash prev btc

        for(int i = 0; i < inputCount; ++i)
        {
            UnspentOutputInfo outPut = unspentOutputs.get(i);
            int outputIndex = outPut.outputIndex;
            byte[] txHash = BTCUtils.reverse(Util.hexToBytes(outPut.txHashForBuild));//Sha256Hash.hash(rawTxByte);
            forSign.write(txHash);
            forSign.writeInt32(outputIndex); //output index in prev tx
            if(inputPos ==-1 || i == inputPos)
            {
                // hex str 1976a914....88ac
                forSign.write((byte)outPut.scriptForBuild.length);
                forSign.write(outPut.scriptForBuild);
            }
            else
            {
                forSign.write(0x00);
            }
            //ffffffff
            forSign.write(new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff}); // sequence
        }


        //02
        byte outputCount = (byte)((change==0) ? 1 : 2); // outputCount
        forSign.write(outputCount);

        //8 bytes

        forSign.writeInt64(amount); //amount
        byte[] sendScript = Transaction.Script.buildOutput(outputAddress).bytes; // build out
        //hex str 1976a914....88ac
        forSign.write((byte)sendScript.length);
        forSign.write(sendScript);

        if(change!=0){
            //8 bytes
            forSign.writeInt64(change); // change
            //hex str 1976a914....88ac
            byte[] chancheScript = Transaction.Script.buildOutput(changeAddress).bytes; //build out
            forSign.write((byte)chancheScript.length);
            forSign.write(chancheScript);

        }

        //00000000
        forSign.write(new byte[]{0x00, 0x00, 0x00, 0x00});

        //forSign.write(new byte[]{0x01, 0x00, 0x00, 0x00});

        byte[] rawData = forSign.toByteArray();

        Log.e("Sign_TX_Body", BTCUtils.toHex(rawData));

        return rawData;
    }

    int calculateSize(int inputCount, int outputCount)
    {
        int size = 0;
        size += 4; // header
        size += 1; //inputCount

        //hex str hash prev btc

        for(int i = 0; i < inputCount; ++i)
        {
            size += 32; //prevtx
            size += 4; //outputIndex;
            size += 1; //scriptLength
            // size+=script;
            size += 4; //ffffffff
        }

        size+=1; //outputCount
        size+=8; //amount
        size+=1;
        // size+=script;

        if(outputCount > 1)
        {
            size+=8;
            size+=1;
            //scriptLen;
        }

        size+=4;
        return size;
    }


    public static byte[] buildBodyTX(String outputAddress, String changeAddress, int outputIndex, String prevID, long amount, long change, byte[] script) throws BitcoinException, IOException {

        //0200000000
        BitcoinOutputStream forSign = new BitcoinOutputStream();
        forSign.writeInt32(0x01);//write(new byte[]{0x02, 0x00, 0x00, 0x00}); // version

        //01
        byte inputCount = 1;
        forSign.write(inputCount); // input count
        //hex str hash prev btc
        byte[] txHash = BTCUtils.reverse(Util.hexToBytes(prevID));//Sha256Hash.hash(rawTxByte);
        forSign.write(txHash); //previos tx hash

        //00000000
        //byte indexOutput = outputIndex;
        forSign.writeInt32(outputIndex/*indexOutput*/); //output index in prev tx
        //forSign.write(0x00);

        // hex str 1976a914....88ac
        forSign.write((byte)script.length);
        forSign.write(script);

        //ffffffff
        forSign.write(new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff}); // sequence

        //02
        byte outputCount = (byte)((change==0) ? 1 : 2); // outputCount
        forSign.write(outputCount);

        //8 bytes

        forSign.writeInt64(amount); //amount
        byte[] sendScript = Transaction.Script.buildOutput(outputAddress).bytes; // build out
        //hex str 1976a914....88ac
        forSign.write((byte)sendScript.length);
        forSign.write(sendScript);

        if(change!=0){
            //8 bytes
            forSign.writeInt64(change); // change
            //hex str 1976a914....88ac
            byte[] chancheScript = Transaction.Script.buildOutput(changeAddress).bytes; //build out
            forSign.write((byte)chancheScript.length);
            forSign.write(chancheScript);

        }

        //00000000
        forSign.write(new byte[]{0x00, 0x00, 0x00, 0x00});

        byte[] rawData = forSign.toByteArray();

        //Log.e("Sign_TX_Body", BTCUtils.toHex(rawData));
        return rawData;
    }

    public static ArrayList<byte[]> getPrevTX(String hex) throws BitcoinException {
        byte[] rawTxByte = fromHex(hex);
        Transaction baseTx = new Transaction(rawTxByte);
        ArrayList<byte[]> prevHashes = new ArrayList<byte[]>();
        for(int i =0; i < baseTx.inputs.length; ++i)
        {
            Transaction.Input input = baseTx.inputs[i];
            prevHashes.add(input.outPoint.hash);
        }
        return prevHashes;
    }

    public static boolean isInput(String myAddress, String hex) throws BitcoinException {
        byte[] rawTxByte = fromHex(hex);
        Transaction baseTx = new Transaction(rawTxByte);
        byte[] myScript = Transaction.Script.buildOutput(myAddress).bytes;
        for(int i =0; i < baseTx.inputs.length; ++i)
        {
            Transaction.Input input = baseTx.inputs[i];
            byte[] script = input.script.bytes;

            // find outputs
            if (Arrays.equals(myScript, script)){
                return true;
            }
        }
        return false;
    }
    public static ArrayList<UnspentOutputInfo> getOutputs(List<Tangem_Card.UnspentTransaction> rawTxList, byte[] outputScriptWeAreAbleToSpend) throws BitcoinException {
        ArrayList<UnspentOutputInfo> unspentOutputs = new ArrayList<>();

        for(Tangem_Card.UnspentTransaction current: rawTxList)
        {
            byte[] rawTxByte = BTCUtils.fromHex(current.Raw);
            if (rawTxByte == null)
            {
                continue;
            }

            Transaction baseTx = new Transaction(rawTxByte);

            if(baseTx.inputs.length == 0 || baseTx.outputs.length == 0)
                throw new IllegalArgumentException("Unable to decode given transaction");

            byte[] txHash = BTCUtils.reverse(CryptoUtil.doubleSha256(rawTxByte));
            String txHashForBuild = current.txID;
            byte[] sign = null;

            for (int outputIndex = 0; outputIndex < baseTx.outputs.length; outputIndex++) {
                Transaction.Output output = baseTx.outputs[outputIndex];

                // find outputs
                if (Arrays.equals(outputScriptWeAreAbleToSpend, output.script.bytes)) {
                    unspentOutputs.add(new UnspentOutputInfo(txHash, output.script, output.value, outputIndex, -1, txHashForBuild, sign));
                }
            }

        }

        return unspentOutputs;
    }

    public static byte[] fromHex(String s) {
        if (s != null) {
            try {
                StringBuilder sb = new StringBuilder(s.length());
                for (int i = 0; i < s.length(); i++) {
                    char ch = s.charAt(i);
                    if (!Character.isWhitespace(ch)) {
                        sb.append(ch);
                    }
                }
                s = sb.toString();
                int len = s.length();
                byte[] data = new byte[len / 2];
                for (int i = 0; i < len; i += 2) {
                    int hi = (Character.digit(s.charAt(i), 16) << 4);
                    int low = Character.digit(s.charAt(i + 1), 16);
                    if (hi >= 256 || low < 0 || low >= 16) {
                        return null;
                    }
                    data[i / 2] = (byte) (hi | low);
                }
                return data;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public static byte[] reverse(byte[] bytes) {
        byte[] result = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = bytes[bytes.length - i - 1];
        }
        return result;
    }

    public static byte[] reverseInPlace(byte[] bytes) {
        int len = bytes.length / 2;
        for (int i = 0; i < len; i++) {
            byte t = bytes[i];
            bytes[i] = bytes[bytes.length - i - 1];
            bytes[bytes.length - i - 1] = t;
        }
        return bytes;
    }

    public static int findSpendableOutput(Transaction tx, String forAddress, long minAmount) throws BitcoinException {
        byte[] outputScriptWeAreAbleToSpend = Transaction.Script.buildOutput(forAddress).bytes;
        int indexOfOutputToSpend = -1;
        for (int indexOfOutput = 0; indexOfOutput < tx.outputs.length; indexOfOutput++) {
            Transaction.Output output = tx.outputs[indexOfOutput];
            if (Arrays.equals(outputScriptWeAreAbleToSpend, output.script.bytes)) {
                indexOfOutputToSpend = indexOfOutput;
                break;//only one input is supported for now
            }
        }
        if (indexOfOutputToSpend == -1) {
            throw new BitcoinException(BitcoinException.ERR_NO_SPENDABLE_OUTPUTS_FOR_THE_ADDRESS, "No spendable standard outputs for " + forAddress + " have found", forAddress);
        }
        final long spendableOutputValue = tx.outputs[indexOfOutputToSpend].value;
        if (spendableOutputValue < minAmount) {
            throw new BitcoinException(BitcoinException.ERR_INSUFFICIENT_FUNDS, "Unspent amount is too small: " + spendableOutputValue, spendableOutputValue);
        }
        return indexOfOutputToSpend;
    }

    public static void verify(Transaction.Script[] scripts, Transaction spendTx) throws Transaction.Script.ScriptInvalidException {
        for (int i = 0; i < scripts.length; i++) {
            Stack<byte[]> stack = new Stack<>();
            spendTx.inputs[i].script.run(stack);//load signature+public key
            scripts[i].run(i, spendTx, stack); //verify that this transaction able to spend that output
            if (Transaction.Script.verifyFails(stack)) {
                throw new Transaction.Script.ScriptInvalidException("Signature is invalid");
            }
        }
    }

    public static class FeeChangeAndSelectedOutputs {
        public final long amountForRecipient, change, fee;
        public final ArrayList<UnspentOutputInfo> outputsToSpend;

        public FeeChangeAndSelectedOutputs(long fee, long change, long amountForRecipient, ArrayList<UnspentOutputInfo> outputsToSpend) {
            this.fee = fee;
            this.change = change;
            this.amountForRecipient = amountForRecipient;
            this.outputsToSpend = outputsToSpend;
        }
    }

    public static FeeChangeAndSelectedOutputs calcFeeChangeAndSelectOutputsToSpend(List<UnspentOutputInfo> unspentOutputs, long amountToSend, long extraFee, final boolean isPublicKeyCompressed) throws BitcoinException {
        long fee = 0;//calculated below
        long change = 0;
        long valueOfUnspentOutputs;
        ArrayList<UnspentOutputInfo> outputsToSpend = new ArrayList<>();
        if (amountToSend <= 0) {
            //transfer all funds from these addresses to outputAddress
            change = 0;
            valueOfUnspentOutputs = 0;
            for (UnspentOutputInfo outputInfo : unspentOutputs) {
                outputsToSpend.add(outputInfo);
                valueOfUnspentOutputs += outputInfo.value;
            }
            final int txLen = BTCUtils.getMaximumTxSize(unspentOutputs, 1, isPublicKeyCompressed);
            fee = BTCUtils.calcMinimumFee(txLen, unspentOutputs, valueOfUnspentOutputs - MIN_FEE_PER_KB * (1 + txLen / 1000));
            amountToSend = valueOfUnspentOutputs - fee - extraFee;
        } else {
            valueOfUnspentOutputs = 0;
            for (UnspentOutputInfo outputInfo : unspentOutputs) {
                outputsToSpend.add(outputInfo);
                valueOfUnspentOutputs += outputInfo.value;
                long updatedFee = MIN_FEE_PER_KB;
                for (int i = 0; i < 3; i++) {
                    fee = updatedFee;
                    change = valueOfUnspentOutputs - fee - extraFee - amountToSend;
                    final int txLen = BTCUtils.getMaximumTxSize(unspentOutputs, change > 0 ? 2 : 1, isPublicKeyCompressed);
                    updatedFee = BTCUtils.calcMinimumFee(txLen, unspentOutputs, change > 0 ? Math.min(amountToSend, change) : amountToSend);
                    if (updatedFee == fee) {
                        break;
                    }
                }
                fee = updatedFee;
                if (valueOfUnspentOutputs >= amountToSend + fee + extraFee) {
                    break;
                }
            }

        }
        if (amountToSend > valueOfUnspentOutputs - fee) {
            throw new BitcoinException(BitcoinException.ERR_INSUFFICIENT_FUNDS, "Not enough funds", valueOfUnspentOutputs - fee);
        }
        if (outputsToSpend.isEmpty()) {
            throw new BitcoinException(BitcoinException.ERR_NO_INPUT, "No outputs to spend");
        }
        if (fee + extraFee > MAX_ALLOWED_FEE) {
            throw new BitcoinException(BitcoinException.ERR_FEE_IS_TOO_BIG, "Fee is too big", fee);
        }
        if (fee < 0 || extraFee < 0) {
            throw new BitcoinException(BitcoinException.ERR_FEE_IS_LESS_THEN_ZERO, "Incorrect fee", fee);
        }
        if (change < 0) {
            throw new BitcoinException(BitcoinException.ERR_CHANGE_IS_LESS_THEN_ZERO, "Incorrect change", change);
        }
        if (amountToSend < 0) {
            throw new BitcoinException(BitcoinException.ERR_AMOUNT_TO_SEND_IS_LESS_THEN_ZERO, "Incorrect amount to send", amountToSend);
        }
        return new FeeChangeAndSelectedOutputs(fee + extraFee, change, amountToSend, outputsToSpend);

    }
}
