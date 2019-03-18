package com.ripple.core.types.known.tx.txns;

import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.Blob;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.serialized.enums.TransactionType;
import com.ripple.core.types.known.tx.Transaction;

public class PaymentChannelCreate extends Transaction {
    public PaymentChannelCreate() {
        super(TransactionType.PaymentChannelCreate);
    }

    public boolean hasDestinationTag() {return has(UInt32.DestinationTag);}
    public boolean hasCancelAfter() {return has(UInt32.CancelAfter);}

    public void destinationTag(UInt32 val) { put(UInt32.DestinationTag, val);}
    public void cancelAfter(UInt32 val) { put(UInt32.CancelAfter, val);}
    public void settleDelay(UInt32 val) { put(UInt32.SettleDelay, val);}
    public void amount(Amount val) { put(Amount.Amount, val);}
    public void publicKey(Blob val) { put(Blob.PublicKey, val);}
    public void destination(AccountID val) { put(AccountID.Destination, val);}

    public UInt32 destinationTag() {return get(UInt32.DestinationTag);}
    public UInt32 cancelAfter() {return get(UInt32.CancelAfter);}
    public UInt32 settleDelay() {return get(UInt32.SettleDelay);}
    public Amount amount() {return get(Amount.Amount);}
    public Blob publicKey() {return get(Blob.PublicKey);}
    public AccountID destination() {return get(AccountID.Destination);}

}
