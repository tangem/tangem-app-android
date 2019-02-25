package com.tangem.domain.wallet;

/**
 * Created by Ilia on 29.09.2017.
 */

import android.util.Log;

import com.tangem.domain.wallet.btc.BitcoinException;
import com.tangem.domain.wallet.btc.BitcoinOutputStream;
import com.tangem.domain.wallet.btc.BtcData;
import com.tangem.util.CryptoUtil;
import com.tangem.util.FormatUtil;
import com.tangem.tangemcommon.util.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"WeakerAccess", "TryWithIdenticalCatches", "unused"})
public final class BCHUtils {
    static final BigInteger LARGEST_PRIVATE_KEY = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);//SECP256K1_N
    public static final long MIN_FEE_PER_KB = 10000;
    public static final long MAX_ALLOWED_FEE = FormatUtil.parseValue("0.1");
    public static final long MIN_PRIORITY_FOR_NO_FEE = 57600000;
    public static final long MIN_MIN_OUTPUT_VALUE_FOR_NO_FEE = 10000000L;
    public static final int MAX_TX_LEN_FOR_NO_FEE = 10000;
    public static final float EXPECTED_BLOCKS_PER_DAY = 144.0f;//(expected confirmations per day)

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

        int inputPos = currentInputPos;
        byte[] tx = buildPreimage(outputAddress, changeAddress, unspentOutputs, inputPos, amount, change);
        return tx;
    }

    public static byte[] buildTXForSend(String outputAddress, String changeAddress, ArrayList<UnspentOutputInfo> unspentOutputs, long amount, long change) throws BitcoinException, IOException {

        int inputPos = -1;
        byte[] tx = buildBodyTX(outputAddress, changeAddress, unspentOutputs, inputPos, amount, change);
        return tx;
    }

    //BIP 143 as reference + script length added
    public static byte[] buildPreimage(String outputAddress, String changeAddress, ArrayList<UnspentOutputInfo> unspentOutputs, int inputPos, long amount, long change) throws BitcoinException, IOException {

        //nVersion of the transaction (4-byte little endian)
        BitcoinOutputStream forSign = new BitcoinOutputStream();
        //forSign.writeInt32(0x01);
        forSign.write(new byte[]{0x02, 0x00, 0x00, 0x00}); // version

        //inputCount
        byte inputCount = (byte) unspentOutputs.size();

        //hashPrevouts (32-byte hash)
        ByteArrayOutputStream prevouts = new ByteArrayOutputStream();
        for (int i = 0; i < inputCount; ++i) {
            UnspentOutputInfo outPut = unspentOutputs.get(i);
            byte[] txHash = BCHUtils.reverse(Util.hexToBytes(outPut.txHashForBuild));//Little-endian txID
            byte[] txIndex = BCHUtils.reverse(Util.intToByteArray4(outPut.outputIndex));//Little-endian outputIndex
            prevouts.write(txHash);
            prevouts.write(txIndex);
        }
        byte[] hashPrevouts = CryptoUtil.doubleSha256(prevouts.toByteArray());
        forSign.write(hashPrevouts);

        //hashSequence (32-byte hash), ffffffff only
        ByteArrayOutputStream sequences = new ByteArrayOutputStream();
        for (int i = 0; i < inputCount; ++i) {
            sequences.write(new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff});
        }
        byte[] hashSequence = CryptoUtil.doubleSha256(sequences.toByteArray());
        forSign.write(hashSequence);

        //outpoint (32-byte hash + 4-byte little endian)
        UnspentOutputInfo outPut = unspentOutputs.get(inputPos);
        byte[] txHash = BCHUtils.reverse(Util.hexToBytes(outPut.txHashForBuild));//Little-endian txID
        byte[] txIndex = BCHUtils.reverse(Util.intToByteArray4(outPut.outputIndex));//Little-endian outputIndex
        forSign.write(txHash);
        forSign.write(txIndex);

        //scriptCode of the input (serialized as scripts inside CTxOuts)
        byte[] scriptCode = Transaction.Script.buildOutput(changeAddress).bytes; //build change out
        byte[] scriptLength = Util.intToByteArray(scriptCode.length);
        forSign.write(scriptLength);
        forSign.write(scriptCode);

        //value of the output spent by this input (8-byte little endian)
        byte[] outValue = BCHUtils.reverse(Util.longToByteArray8(outPut.value));
        forSign.write(outValue);

        //nSequence of the input (4-byte little endian), ffffffff only
        forSign.write(new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff});

        //hashOutputs (32-byte hash)
        ByteArrayOutputStream outputs = new ByteArrayOutputStream();
        byte[] sendAmount = BCHUtils.reverse(Util.longToByteArray8(amount));
        byte[] sendScript = Transaction.Script.buildOutput(outputAddress).bytes; // build out
        byte[] sendLength = Util.intToByteArray(sendScript.length);
        outputs.write(sendAmount);
        outputs.write(sendLength);
        outputs.write(sendScript);
        //output for change (if any)
        if (change != 0) {
            byte[] changeAmount = BCHUtils.reverse(Util.longToByteArray8(change));
            byte[] changeScript = Transaction.Script.buildOutput(changeAddress).bytes; //build change out
            byte[] changeLength = Util.intToByteArray(changeScript.length);
            outputs.write(changeAmount);
            outputs.write(changeLength);
            outputs.write(changeScript);
        }
        byte[] hashOutputs = CryptoUtil.doubleSha256(outputs.toByteArray());
        forSign.write(hashOutputs);

        //nLocktime of the transaction (4-byte little endian)
        forSign.write(new byte[]{0x00, 0x00, 0x00, 0x00});

        //sighash type of the signature (4-byte little endian)
        forSign.write(new byte[]{0x41, 0x00, 0x00, 0x00});

        byte[] rawData = forSign.toByteArray();

        Log.e("Sign_TX_Body", BCHUtils.toHex(rawData));

        return rawData;

    }
    public static byte[] buildBodyTX(String outputAddress, String changeAddress, ArrayList<UnspentOutputInfo> unspentOutputs, int inputPos, long amount, long change) throws BitcoinException, IOException {

        //0200000000
        BitcoinOutputStream forSign = new BitcoinOutputStream();
        //forSign.writeInt32(0x01);
        forSign.write(new byte[]{0x02, 0x00, 0x00, 0x00}); // version

        //01
        byte inputCount = (byte) unspentOutputs.size();
        forSign.write(inputCount); // input count
        //hex str hash prev btc

        for (int i = 0; i < inputCount; ++i) {
            UnspentOutputInfo outPut = unspentOutputs.get(i);
            int outputIndex = outPut.outputIndex;
            byte[] txHash = BCHUtils.reverse(Util.hexToBytes(outPut.txHashForBuild));//Sha256Hash.hash(rawTxByte);
            forSign.write(txHash);
            forSign.writeInt32(outputIndex); //output index in prev tx
            if (inputPos == -1 || i == inputPos) {
                // hex str 1976a914....88ac
                forSign.write((byte) outPut.scriptForBuild.length);
                forSign.write(outPut.scriptForBuild);
            } else {
                forSign.write(0x00);
            }
            //ffffffff
            forSign.write(new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff}); // sequence
        }


        //02
        byte outputCount = (byte) ((change == 0) ? 1 : 2); // outputCount
        forSign.write(outputCount);

        //8 bytes

        forSign.writeInt64(amount); //amount
        byte[] sendScript = Transaction.Script.buildOutput(outputAddress).bytes; // build out
        //hex str 1976a914....88ac
        forSign.write((byte) sendScript.length);
        forSign.write(sendScript);

        if (change != 0) {
            //8 bytes
            forSign.writeInt64(change); // change
            //hex str 1976a914....88ac
            byte[] chancheScript = Transaction.Script.buildOutput(changeAddress).bytes; //build out
            forSign.write((byte) chancheScript.length);
            forSign.write(chancheScript);

        }

        //00000000
        forSign.write(new byte[]{0x00, 0x00, 0x00, 0x00});

        //forSign.write(new byte[]{0x01, 0x00, 0x00, 0x00});

        byte[] rawData = forSign.toByteArray();

        Log.e("Sign_TX_Body", BCHUtils.toHex(rawData));

        return rawData;
    }

    public static byte[] buildBodyTX(String outputAddress, String changeAddress, int outputIndex, String prevID, long amount, long change, byte[] script) throws BitcoinException, IOException {

        //0200000000
        BitcoinOutputStream forSign = new BitcoinOutputStream();
        forSign.writeInt32(0x01);//write(new byte[]{0x02, 0x00, 0x00, 0x00}); // version

        //01
        byte inputCount = 1;
        forSign.write(inputCount); // input count
        //hex str hash prev btc
        byte[] txHash = BCHUtils.reverse(Util.hexToBytes(prevID));//Sha256Hash.hash(rawTxByte);
        forSign.write(txHash); //previos tx hash

        //00000000
        //byte indexOutput = outputIndex;
        forSign.writeInt32(outputIndex/*indexOutput*/); //output index in prev tx
        //forSign.write(0x00);

        // hex str 1976a914....88ac
        forSign.write((byte) script.length);
        forSign.write(script);

        //ffffffff
        forSign.write(new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff}); // sequence

        //02
        byte outputCount = (byte) ((change == 0) ? 1 : 2); // outputCount
        forSign.write(outputCount);

        //8 bytes

        forSign.writeInt64(amount); //amount
        byte[] sendScript = Transaction.Script.buildOutput(outputAddress).bytes; // build out
        //hex str 1976a914....88ac
        forSign.write((byte) sendScript.length);
        forSign.write(sendScript);

        if (change != 0) {
            //8 bytes
            forSign.writeInt64(change); // change
            //hex str 1976a914....88ac
            byte[] chancheScript = Transaction.Script.buildOutput(changeAddress).bytes; //build out
            forSign.write((byte) chancheScript.length);
            forSign.write(chancheScript);

        }

        //00000000
        forSign.write(new byte[]{0x00, 0x00, 0x00, 0x00});

        byte[] rawData = forSign.toByteArray();

        //Log.e("Sign_TX_Body", BCHUtils.toHex(rawData));
        return rawData;
    }

    public static ArrayList<UnspentOutputInfo> getOutputs(List<BtcData.UnspentTransaction> rawTxList, byte[] outputScriptWeAreAbleToSpend) throws BitcoinException {
        ArrayList<UnspentOutputInfo> unspentOutputs = new ArrayList<>();

        for (BtcData.UnspentTransaction current : rawTxList) {
            byte[] rawTxByte = BCHUtils.fromHex(current.Raw);
            if (rawTxByte == null || current.Raw.isEmpty()) {
                continue;
            }

            Transaction baseTx = new Transaction(rawTxByte);

            if (baseTx.inputs.length == 0 || baseTx.outputs.length == 0)
                throw new IllegalArgumentException("Unable to decode given transaction");

            byte[] txHash = BCHUtils.reverse(CryptoUtil.doubleSha256(rawTxByte));
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

}

