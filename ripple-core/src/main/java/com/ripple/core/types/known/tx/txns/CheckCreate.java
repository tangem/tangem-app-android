package com.ripple.core.types.known.tx.txns;

import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.serialized.enums.TransactionType;
import com.ripple.core.types.known.tx.Transaction;

public class CheckCreate extends Transaction {
    public CheckCreate() {
        super(TransactionType.CheckCreate);
    }

    public boolean hasExpiration() {return has(UInt32.Expiration);}
    public boolean hasDestinationTag() {return has(UInt32.DestinationTag);}
    public boolean hasInvoiceID() {return has(Hash256.InvoiceID);}

    public void expiration(UInt32 val) { put(UInt32.Expiration, val);}
    public void destinationTag(UInt32 val) { put(UInt32.DestinationTag, val);}
    public void invoiceID(Hash256 val) { put(Hash256.InvoiceID, val);}
    public void sendMax(Amount val) { put(Amount.SendMax, val);}
    public void destination(AccountID val) { put(AccountID.Destination, val); }

    public AccountID destination() {return get(AccountID.Destination);}
    public UInt32 expiration() {return get(UInt32.Expiration);}
    public UInt32 destinationTag() {return get(UInt32.DestinationTag);}
    public Hash256 invoiceID() {return get(Hash256.InvoiceID);}
    public Amount sendMax() {return get(Amount.SendMax);}
}
