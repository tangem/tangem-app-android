package com.ripple.core.types.known.tx.txns;

import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.serialized.enums.TransactionType;
import com.ripple.core.types.known.tx.Transaction;

public class EscrowCancel extends Transaction {
    public EscrowCancel() {
        super(TransactionType.EscrowCancel);
    }

    public UInt32 offerSequence() {return get(UInt32.OfferSequence);}
    public void offerSequence(UInt32 val) { put(UInt32.OfferSequence, val);}
    public AccountID owner() {return get(AccountID.Owner);}
    public void owner(AccountID val) { put(AccountID.Owner, val);}

}
