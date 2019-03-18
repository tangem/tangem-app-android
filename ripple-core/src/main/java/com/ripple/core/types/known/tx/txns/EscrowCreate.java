package com.ripple.core.types.known.tx.txns;

import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.Blob;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.serialized.enums.TransactionType;
import com.ripple.core.types.known.tx.Transaction;

public class EscrowCreate extends Transaction {
    public EscrowCreate() {
        super(TransactionType.EscrowCreate);
    }
    public boolean hasDestinationTag() {return has(UInt32.DestinationTag);}
    public boolean hasCancelAfter() {return has(UInt32.CancelAfter);}
    public boolean hasFinishAfter() {return has(UInt32.FinishAfter);}
    public boolean hasCondition() {return has(Blob.Condition);}

    public Amount amount() {return get(Amount.Amount);}
    public AccountID destination() {return get(AccountID.Destination);}
    public UInt32 destinationTag() {return get(UInt32.DestinationTag);}
    public UInt32 cancelAfter() {return get(UInt32.CancelAfter);}
    public UInt32 finishAfter() {return get(UInt32.FinishAfter);}
    public Blob condition() {return get(Blob.Condition);}

    public void destinationTag(UInt32 val) { put(UInt32.DestinationTag, val);}
    public void cancelAfter(UInt32 val) { put(UInt32.CancelAfter, val);}
    public void finishAfter(UInt32 val) { put(UInt32.FinishAfter, val);}
    public void amount(Amount val) { put(Amount.Amount, val);}
    public void condition(Blob val) { put(Blob.Condition, val);}
    public void destination(AccountID val) { put(AccountID.Destination, val);}

}
