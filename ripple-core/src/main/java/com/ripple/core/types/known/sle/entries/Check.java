package com.ripple.core.types.known.sle.entries;

import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.coretypes.uint.UInt64;
import com.ripple.core.serialized.enums.LedgerEntryType;

public class Check extends IndexedLedgerEntry {
    public Check() {
        super(LedgerEntryType.Check);
    }

    public boolean hasSourceTag() {return has(UInt32.SourceTag);}
    public boolean hasExpiration() {return has(UInt32.Expiration);}
    public boolean hasDestinationTag() {return has(UInt32.DestinationTag);}
    public boolean hasInvoiceID() {return has(Hash256.InvoiceID);}

    public UInt32 sourceTag() {return get(UInt32.SourceTag);}
    public UInt32 sequence() {return get(UInt32.Sequence);}
    public UInt32 expiration() {return get(UInt32.Expiration);}
    public UInt32 destinationTag() {return get(UInt32.DestinationTag);}
    public UInt64 ownerNode() {return get(UInt64.OwnerNode);}
    public UInt64 destinationNode() {return get(UInt64.DestinationNode);}
    public Hash256 invoiceID() {return get(Hash256.InvoiceID);}
    public Amount sendMax() {return get(Amount.SendMax);}
    public AccountID account() {return get(AccountID.Account);}
    public AccountID destination() {return get(AccountID.Destination);}

    public void sourceTag(UInt32 val) { put(UInt32.SourceTag, val);}
    public void sequence(UInt32 val) { put(UInt32.Sequence, val);}
    public void expiration(UInt32 val) { put(UInt32.Expiration, val);}
    public void destinationTag(UInt32 val) { put(UInt32.DestinationTag, val);}
    public void ownerNode(UInt64 val) { put(UInt64.OwnerNode, val);}
    public void destinationNode(UInt64 val) { put(UInt64.DestinationNode, val);}
    public void invoiceID(Hash256 val) { put(Hash256.InvoiceID, val);}
    public void sendMax(Amount val) { put(Amount.SendMax, val);}
    public void destination(AccountID val) { put(AccountID.Destination, val);}
}
