package com.ripple.core.types.known.sle.entries;

import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.coretypes.uint.UInt64;
import com.ripple.core.serialized.enums.LedgerEntryType;

public class Ticket extends IndexedLedgerEntry {
    public Ticket() {
        super(LedgerEntryType.Ticket);
    }

    public AccountID account() {return get(AccountID.Account);}
    public AccountID target() {return get(AccountID.Target);}
    public UInt32 expiration() {return get(UInt32.Expiration);}
    public UInt32 sequence() {return get(UInt32.Sequence);}
    public UInt64 ownerNode() {return get(UInt64.OwnerNode);}

    public void expiration(UInt32 val) { put(UInt32.Expiration, val);}
    public void ownerNode(UInt64 val) { put(UInt64.OwnerNode, val);}
    public void sequence(UInt32 val) { put(UInt32.Sequence, val);}
    public void target(AccountID val) { put(AccountID.Target, val);}

    public boolean hasExpiration() {return has(UInt32.Expiration);}
    public boolean hasTarget() {return has(AccountID.Target);}

}
