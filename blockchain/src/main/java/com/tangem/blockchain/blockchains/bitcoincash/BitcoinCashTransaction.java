package com.tangem.blockchain.blockchains.bitcoincash;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.UnsafeByteArrayOutputStream;
import org.bitcoinj.core.VarInt;
import org.bitcoinj.crypto.TransactionSignature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static org.bitcoinj.core.Utils.uint32ToByteStreamLE;
import static org.bitcoinj.core.Utils.uint64ToByteStreamLE;

// logic from https://github.com/pokkst/bitcoincashj
public class BitcoinCashTransaction extends Transaction {
//    private ArrayList<TransactionInput> inputs;
//    private ArrayList<TransactionOutput> outputs;

//    private long version;
//    private long lockTime;

    public final byte SIGHASH_FORK_ID = 0x40;

    public BitcoinCashTransaction(NetworkParameters params) {
        super(params);
    }

    public synchronized Sha256Hash hashForSignatureWitness(
            int inputIndex,
            byte[] connectedScript,
            Coin prevValue,
            SigHash type,
            boolean anyoneCanPay)
    {
        byte sigHashType = (byte) TransactionSignature.calcSigHashValue(type, anyoneCanPay);
        sigHashType |= SIGHASH_FORK_ID;

        ByteArrayOutputStream bos = new UnsafeByteArrayOutputStream(length == UNKNOWN_LENGTH ? 256 : length + 4);
        try {
            byte[] hashPrevouts = new byte[32];
            byte[] hashSequence = new byte[32];
            byte[] hashOutputs = new byte[32];
            anyoneCanPay = (sigHashType & SIGHASH_ANYONECANPAY_VALUE) == SIGHASH_ANYONECANPAY_VALUE;
            List<TransactionInput> inputs = getInputs();
            List<TransactionOutput> outputs = getOutputs();

            if (!anyoneCanPay) {
                ByteArrayOutputStream bosHashPrevouts = new UnsafeByteArrayOutputStream(256);
                for (int i = 0; i < inputs.size(); ++i) {
                    bosHashPrevouts.write(inputs.get(i).getOutpoint().getHash().getReversedBytes());
                    uint32ToByteStreamLE(inputs.get(i).getOutpoint().getIndex(), bosHashPrevouts);
                }
                hashPrevouts = Sha256Hash.hashTwice(bosHashPrevouts.toByteArray());
            }

            if (!anyoneCanPay && type != SigHash.SINGLE && type != SigHash.NONE) {
                ByteArrayOutputStream bosSequence = new UnsafeByteArrayOutputStream(256);
                for (int i = 0; i < inputs.size(); ++i) {
                    uint32ToByteStreamLE(inputs.get(i).getSequenceNumber(), bosSequence);
                }
                hashSequence = Sha256Hash.hashTwice(bosSequence.toByteArray());
            }

            if (type != SigHash.SINGLE && type != SigHash.NONE) {
                ByteArrayOutputStream bosHashOutputs = new UnsafeByteArrayOutputStream(256);
                for (int i = 0; i < outputs.size(); ++i) {
                    uint64ToByteStreamLE(
                            BigInteger.valueOf(outputs.get(i).getValue().getValue()),
                            bosHashOutputs
                    );
                    bosHashOutputs.write(new VarInt(outputs.get(i).getScriptBytes().length).encode());
                    bosHashOutputs.write(outputs.get(i).getScriptBytes());
                }
                hashOutputs = Sha256Hash.hashTwice(bosHashOutputs.toByteArray());
            } else if (type == SigHash.SINGLE && inputIndex < outputs.size()) {
                ByteArrayOutputStream bosHashOutputs = new UnsafeByteArrayOutputStream(256);
                uint64ToByteStreamLE(
                        BigInteger.valueOf(outputs.get(inputIndex).getValue().getValue()),
                        bosHashOutputs
                );
                bosHashOutputs.write(new VarInt(outputs.get(inputIndex).getScriptBytes().length).encode());
                bosHashOutputs.write(outputs.get(inputIndex).getScriptBytes());
                hashOutputs = Sha256Hash.hashTwice(bosHashOutputs.toByteArray());
            }
            uint32ToByteStreamLE(getVersion(), bos);
            bos.write(hashPrevouts);
            bos.write(hashSequence);
            bos.write(inputs.get(inputIndex).getOutpoint().getHash().getReversedBytes());
            uint32ToByteStreamLE(inputs.get(inputIndex).getOutpoint().getIndex(), bos);
            bos.write(new VarInt(connectedScript.length).encode());
            bos.write(connectedScript);
            uint64ToByteStreamLE(BigInteger.valueOf(prevValue.getValue()), bos);
            uint32ToByteStreamLE(inputs.get(inputIndex).getSequenceNumber(), bos);
            bos.write(hashOutputs);
            uint32ToByteStreamLE(getLockTime(), bos);
            uint32ToByteStreamLE(0x000000ff & sigHashType, bos);
        } catch (IOException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }

        return Sha256Hash.twiceOf(bos.toByteArray());
    }
}
