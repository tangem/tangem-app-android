package com.ripple.core.types.known.tx.txns;

import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.serialized.enums.TransactionType;
import com.ripple.core.types.known.tx.Transaction;

public class CheckCancel extends Transaction {
    public CheckCancel() {
        super(TransactionType.CheckCancel);
    }

    public Hash256 checkID() {return get(Hash256.CheckID);}
    public void checkID(Hash256 val) { put(Hash256.CheckID, val);}
}
