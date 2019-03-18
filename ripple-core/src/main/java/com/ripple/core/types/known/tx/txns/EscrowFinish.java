package com.ripple.core.types.known.tx.txns;

import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Blob;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.serialized.enums.TransactionType;
import com.ripple.core.types.known.tx.Transaction;

public class EscrowFinish extends Transaction {
    public EscrowFinish() {
        super(TransactionType.EscrowFinish);
    }

    public boolean hasFulfillment() {return has(Blob.Fulfillment);}
    public boolean hasCondition() {return has(Blob.Condition);}

    public UInt32 offerSequence() {return get(UInt32.OfferSequence);}
    public Blob fulfillment() {return get(Blob.Fulfillment);}
    public Blob condition() {return get(Blob.Condition);}
    public AccountID owner() {return get(AccountID.Owner);}

    public void offerSequence(UInt32 val) { put(UInt32.OfferSequence, val);}
    public void fulfillment(Blob val) { put(Blob.Fulfillment, val);}
    public void condition(Blob val) { put(Blob.Condition, val);}
    public void owner(AccountID val) { put(AccountID.Owner, val);}

}
