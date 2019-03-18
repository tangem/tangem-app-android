package com.ripple.core.types.known.sle.entries;

import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.Blob;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.coretypes.uint.UInt64;
import com.ripple.core.serialized.enums.LedgerEntryType;

public class PayChannel extends IndexedLedgerEntry {
    public PayChannel() {
        super(LedgerEntryType.PayChannel);
    }

    public AccountID account() {return get(AccountID.Account);}
    public AccountID destination() {return get(AccountID.Destination);}
    public Amount amount() {return get(Amount.Amount);}
    public Amount balance() {return get(Amount.Balance);}
    public Blob publicKey() {return get(Blob.PublicKey);}
    public UInt32 cancelAfter() {return get(UInt32.CancelAfter);}
    public UInt32 destinationTag() {return get(UInt32.DestinationTag);}
    public UInt32 expiration() {return get(UInt32.Expiration);}
    public UInt32 settleDelay() {return get(UInt32.SettleDelay);}
    public UInt32 sourceTag() {return get(UInt32.SourceTag);}
    public UInt64 ownerNode() {return get(UInt64.OwnerNode);}

    public void amount(Amount val) { put(Amount.Amount, val);}
    public void balance(Amount val) { put(Amount.Balance, val);}
    public void cancelAfter(UInt32 val) { put(UInt32.CancelAfter, val);}
    public void destination(AccountID val) { put(AccountID.Destination, val);}
    public void destinationTag(UInt32 val) { put(UInt32.DestinationTag, val);}
    public void expiration(UInt32 val) { put(UInt32.Expiration, val);}
    public void ownerNode(UInt64 val) { put(UInt64.OwnerNode, val);}
    public void publicKey(Blob val) { put(Blob.PublicKey, val);}
    public void settleDelay(UInt32 val) { put(UInt32.SettleDelay, val);}
    public void sourceTag(UInt32 val) { put(UInt32.SourceTag, val);}

    public boolean hasCancelAfter() {return has(UInt32.CancelAfter);}
    public boolean hasDestinationTag() {return has(UInt32.DestinationTag);}
    public boolean hasExpiration() {return has(UInt32.Expiration);}
    public boolean hasSourceTag() {return has(UInt32.SourceTag);}
}
