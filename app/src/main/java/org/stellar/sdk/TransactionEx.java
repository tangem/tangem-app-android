package org.stellar.sdk;

import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.xdr.DecoratedSignature;
import org.stellar.sdk.xdr.PublicKey;
import org.stellar.sdk.xdr.PublicKeyType;
import org.stellar.sdk.xdr.SignatureHint;
import org.stellar.sdk.xdr.Uint256;
import org.stellar.sdk.xdr.XdrDataOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class TransactionEx extends Transaction {
    public TransactionEx(KeyPair sourceAccount, int fee, long sequenceNumber, Operation[] operations, Memo memo, TimeBounds timeBounds) {
        super(sourceAccount, fee, sequenceNumber, operations, memo, timeBounds);
    }

    /**
     * Builds a transaction. It will increment sequence number of the source account.
     */
    public static TransactionEx buildEx(int timeout, AccountResponse sourceAccount, Operation operation)
    {
        long timeoutTimestamp = System.currentTimeMillis() / 1000L + timeout;
        TimeBounds mTimeBounds = new TimeBounds(0, timeoutTimestamp);

        Operation[] operations = new Operation[1];
        operations[0] = operation;
        TransactionEx transaction = new TransactionEx(sourceAccount.getKeypair(), operations.length * 100, sourceAccount.getIncrementedSequenceNumber(), operations, Memo.text(""), mTimeBounds);
        // Increment sequence number when there were no exceptions when creating a transaction
        sourceAccount.incrementSequenceNumber();
        return transaction;
    }

    public PublicKey getXdrPublicKey() {
        PublicKey publicKey = new PublicKey();
        publicKey.setDiscriminant(PublicKeyType.PUBLIC_KEY_TYPE_ED25519);
        Uint256 uint256 = new Uint256();
        uint256.setUint256(mSourceAccount.getPublicKey());
        publicKey.setEd25519(uint256);
        return publicKey;
    }

    public SignatureHint getSignatureHint() {
        try {
            ByteArrayOutputStream publicKeyBytesStream = new ByteArrayOutputStream();
            XdrDataOutputStream xdrOutputStream = new XdrDataOutputStream(publicKeyBytesStream);
            PublicKey.encode(xdrOutputStream, this.getXdrPublicKey());
            byte[] publicKeyBytes = publicKeyBytesStream.toByteArray();
            byte[] signatureHintBytes = Arrays.copyOfRange(publicKeyBytes, publicKeyBytes.length - 4, publicKeyBytes.length);

            SignatureHint signatureHint = new SignatureHint();
            signatureHint.setSignatureHint(signatureHintBytes);
            return signatureHint;
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public void setSign(byte[] signFromCard) {
//        byte[] txHash = this.hash();

        byte[] signatureBytes = signFromCard;//this.sign(txHash);

        org.stellar.sdk.xdr.Signature signature = new org.stellar.sdk.xdr.Signature();
        signature.setSignature(signatureBytes);

        DecoratedSignature decoratedSignature = new DecoratedSignature();
        decoratedSignature.setHint(this.getSignatureHint());
        decoratedSignature.setSignature(signature);

        mSignatures.add(decoratedSignature);
    }
}
