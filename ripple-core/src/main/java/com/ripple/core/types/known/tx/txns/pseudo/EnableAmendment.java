package com.ripple.core.types.known.tx.txns.pseudo;

import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.serialized.enums.TransactionType;
import com.ripple.core.types.known.tx.Transaction;

public class EnableAmendment extends Transaction {
    public EnableAmendment() {
        super(TransactionType.EnableAmendment);
    }

    public UInt32 ledgerSequence() {return get(UInt32.LedgerSequence);}
    public void ledgerSequence(UInt32 val) { put(UInt32.LedgerSequence, val);}
    public Hash256 amendment() {return get(Hash256.Amendment);}
    public void amendment(Hash256 val) { put(Hash256.Amendment, val);}
}
