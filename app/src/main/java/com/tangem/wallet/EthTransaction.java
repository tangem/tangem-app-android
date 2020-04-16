package com.tangem.wallet;

import android.util.Log;

import com.tangem.util.ByteUtil;
import com.tangem.util.CryptoUtil;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.spongycastle.util.BigIntegers;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;

import static com.tangem.util.ByteUtil.EMPTY_BYTE_ARRAY;

/**
 * Created by Ilia on 07.01.2018.
 */

public class EthTransaction {
    byte[] nonce;
    byte[] gasPrice;
    byte[] gasLimit;
    byte[] receiveAddress;
    byte[] value;
    byte[] data;
    Integer chainId;
    byte[] rlpRaw;
    public ECDSASignatureETH signature;
    byte[] rlpEncoded;

    private static final int CHAIN_ID_INC = 35;
    private static final int LOWER_REAL_V = 27;

    public static EthTransaction create(String to, BigInteger amount, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, Integer chainId) {
        return new EthTransaction(BigIntegers.asUnsignedByteArray(nonce),
                BigIntegers.asUnsignedByteArray(gasPrice),
                BigIntegers.asUnsignedByteArray(gasLimit),
                Hex.decode(to),
                BigIntegers.asUnsignedByteArray(amount),
                null,
                chainId);
    }

    public static EthTransaction create(String to, BigInteger amount, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, Integer chainId, byte[] data) {
        return new EthTransaction(BigIntegers.asUnsignedByteArray(nonce),
                BigIntegers.asUnsignedByteArray(gasPrice),
                BigIntegers.asUnsignedByteArray(gasLimit),
                Hex.decode(to),
                BigIntegers.asUnsignedByteArray(amount),
                data,
                chainId);
    }

    public EthTransaction(byte[] nonce, byte[] gasPrice, byte[] gasLimit, byte[] receiveAddress, byte[] value, byte[] data, Integer chainId) {
        this.nonce = nonce;
        this.gasPrice = gasPrice;
        this.gasLimit = gasLimit;
        this.receiveAddress = receiveAddress;
        if (ByteUtil.isSingleZero(value)) {
            this.value = EMPTY_BYTE_ARRAY;
        } else {
            this.value = value;
        }
        this.data = data;
        this.chainId = chainId;

        if (receiveAddress == null) {
            this.receiveAddress = ByteUtil.EMPTY_BYTE_ARRAY;
        }
    }

    public enum ChainEnum {
        Mainnet(1),
        Morden(2),
        Ropsten(3),
        Rinkeby(4),
        Rootstock_mainnet(30),
        Rootstock_testnet(31),
        Kovan(42),
        Ethereum_Classic_mainnet(61),
        Ethereum_Classic_testnet(62),
        Geth_private_chains(1337),
        Matic_Testnet(8995);

        private int value;

        ChainEnum(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public byte[] getRawHash() {

        byte[] plainMsg = this.getEncodedRaw();
        Keccak256 kec = new Keccak256();
        return kec.digest(plainMsg);
    }

    public byte[] getHash() {

        byte[] plainMsg = this.getEncoded();
        Keccak256 kec = new Keccak256();
        return kec.digest(plainMsg);
    }

    public static int BruteRecoveryID2(ECDSASignatureETH sig, byte[] messageHash, byte[] thisKey) {
        Log.e("ETH_KZ", BTCUtils.toHex(thisKey));
        int recId = -1;
        for (int i = 0; i < 4; i++) {
            byte[] recK = CryptoUtil.recoverPubBytesFromSignature(i, sig, messageHash);

            if (recK == null) {
                continue;
            }

            Log.e("ETH_k " + String.valueOf(i), BTCUtils.toHex(recK));
            if (Arrays.equals(recK, thisKey)) {
                recId = i;
                recId += 27;
                break;
            }
        }
        return recId;
    }

    public int BruteRecoveryID(ECKey.ECDSASignature sig, Sha256Hash messageHash, byte[] thisKey) {
        Log.e("ETH_KZ", BTCUtils.toHex(thisKey));
        int recId = -1;
        for (int i = 0; i < 4; i++) {
            ECKey k = ECKey.recoverFromSignature(i, sig, messageHash, false);

            if (k == null)
                continue;
            byte[] recK = k.getPubKey();
            Log.e("ETH_k " + String.valueOf(i), BTCUtils.toHex(recK));
            if (k != null && Arrays.equals(recK, thisKey)) {
                recId = i;
                break;
            }
        }
        return recId;
    }

    // signed TX
    public byte[] getEncoded() {

        // parse null as 0 for nonce
        byte[] nonce = null;
        if (this.nonce == null || this.nonce.length == 1 && this.nonce[0] == 0) {
            nonce = RLP.encodeElement(null);
        } else {
            nonce = RLP.encodeElement(this.nonce);
        }
        byte[] gasPrice = RLP.encodeElement(this.gasPrice);
        byte[] gasLimit = RLP.encodeElement(this.gasLimit);
        byte[] receiveAddress = RLP.encodeElement(this.receiveAddress);
        byte[] value = RLP.encodeElement(this.value);
        byte[] data = RLP.encodeElement(this.data);

        byte[] v, r, s;

        if (signature != null) {
            int encodeV;
            if (chainId == null) {
                encodeV = signature.v;
            } else {
                encodeV = signature.v - LOWER_REAL_V;
                encodeV += chainId * 2 + CHAIN_ID_INC;
            }
            v = RLP.encodeInt(encodeV);
            r = RLP.encodeElement(BigIntegers.asUnsignedByteArray(signature.r));
            s = RLP.encodeElement(BigIntegers.asUnsignedByteArray(signature.s));
        } else {
            // Since EIP-155 use chainId for v
            v = chainId == null ? RLP.encodeElement(EMPTY_BYTE_ARRAY) : RLP.encodeInt(chainId);
            r = RLP.encodeElement(EMPTY_BYTE_ARRAY);
            s = RLP.encodeElement(EMPTY_BYTE_ARRAY);
        }

        this.rlpEncoded = RLP.encodeList(nonce, gasPrice, gasLimit,
                receiveAddress, value, data, v, r, s);

        //this.hash = this.getHash();

        return rlpEncoded;
    }

    // unsigned TX
    public byte[] getEncodedRaw() {
        // parse null as 0 for nonce
        byte[] nonce = null;
        if (this.nonce == null || this.nonce.length == 1 && this.nonce[0] == 0) {
            nonce = RLP.encodeElement(null);
        } else {
            nonce = RLP.encodeElement(this.nonce);
        }
        byte[] gasPrice = RLP.encodeElement(this.gasPrice);
        byte[] gasLimit = RLP.encodeElement(this.gasLimit);
        byte[] receiveAddress = RLP.encodeElement(this.receiveAddress);
        byte[] value = RLP.encodeElement(this.value);
        byte[] data = RLP.encodeElement(this.data);

        // Since EIP-155 use chainId for v
        if (chainId == null) {
            rlpRaw = RLP.encodeList(nonce, gasPrice, gasLimit, receiveAddress,
                    value, data);
        } else {
            byte[] v, r, s;
            v = RLP.encodeInt(chainId);
            r = RLP.encodeElement(EMPTY_BYTE_ARRAY);
            s = RLP.encodeElement(EMPTY_BYTE_ARRAY);
            rlpRaw = RLP.encodeList(nonce, gasPrice, gasLimit, receiveAddress,
                    value, data, v, r, s);
        }
        return rlpRaw;
    }

}