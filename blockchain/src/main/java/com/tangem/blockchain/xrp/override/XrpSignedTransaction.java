package com.tangem.blockchain.xrp.override;

import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.Blob;
import com.ripple.core.coretypes.STObject;
import com.ripple.core.coretypes.hash.HalfSha512;
import com.ripple.core.coretypes.hash.prefixes.HashPrefix;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.serialized.BytesList;
import com.ripple.core.serialized.MultiSink;
import com.ripple.core.types.known.tx.Transaction;
import com.ripple.core.types.known.tx.signed.SignedTransaction;

import java.util.Arrays;

public class XrpSignedTransaction extends SignedTransaction {
    private XrpSignedTransaction(Transaction of) {
        txn = (Transaction) STObject.fromBytes(of.toBytes());
    }

    protected XrpSignedTransaction() {
    }

    public static XrpSignedTransaction fromTx(Transaction tx) {
        return new XrpSignedTransaction(tx);
    }

    public void prepare(byte[] pubKeyBytes) {
        prepare(pubKeyBytes, null, null, null);
    }

    public void prepare(byte[] pubKeyBytes,
                        Amount fee,
                        UInt32 Sequence,
                        UInt32 lastLedgerSequence) {

        Blob pubKey = new Blob(pubKeyBytes);

        // This won't always be specified
        if (lastLedgerSequence != null) {
            txn.put(UInt32.LastLedgerSequence, lastLedgerSequence);
        }
        if (Sequence != null) {
            txn.put(UInt32.Sequence, Sequence);
        }
        if (fee != null) {
            txn.put(Amount.Fee, fee);
        }

        txn.signingPubKey(pubKey);

        if (Transaction.CANONICAL_FLAG_DEPLOYED) {
            txn.setCanonicalSignatureFlag();
        }

        txn.checkFormat();
        signingData = txn.signingData();
        if (previousSigningData != null && Arrays.equals(signingData, previousSigningData)) {
            return;
        }
    }

    public void addSign(byte[] signature) {
        try {
            txn.txnSignature(new Blob(signature));

            BytesList blob = new BytesList();
            HalfSha512 id = HalfSha512.prefixed256(HashPrefix.transactionID);

            txn.toBytesSink(new MultiSink(blob, id));
            tx_blob = blob.bytesHex();
            hash = id.finish();
        } catch (Exception e) {
            // electric paranoia
            previousSigningData = null;
            throw new RuntimeException(e);
        } /*else {*/
        previousSigningData = signingData;
        // }
    }
}
