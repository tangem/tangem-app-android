package com.ripple.core.types.known.tx.txns;

import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.serialized.enums.TransactionType;
import com.ripple.core.types.known.tx.Transaction;

public class CheckCash extends Transaction {
    public CheckCash() {
        super(TransactionType.CheckCash);
    }

    public boolean hasAmount() {return has(Amount.Amount);}
    public boolean hasDeliverMin() {return has(Amount.DeliverMin);}

    public Hash256 checkID() {return get(Hash256.CheckID);}
    public Amount amount() {return get(Amount.Amount);}
    public Amount deliverMin() {return get(Amount.DeliverMin);}

    public void checkID(Hash256 val) { put(Hash256.CheckID, val);}
    public void amount(Amount val) { put(Amount.Amount, val);}
    public void deliverMin(Amount val) { put(Amount.DeliverMin, val);}
}
