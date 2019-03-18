package com.ripple.core.types.known.sle.entries;

import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.uint.UInt64;
import com.ripple.core.serialized.enums.LedgerEntryType;

public class DepositPreauthLe extends IndexedLedgerEntry {
    public DepositPreauthLe() {
        super(LedgerEntryType.DepositPreauth);
    }
    public UInt64 ownerNode() {return get(UInt64.OwnerNode);}
    public void ownerNode(UInt64 val) { put(UInt64.OwnerNode, val);}

    public AccountID account() {return get(AccountID.Account);}

    public AccountID authorize() {return get(AccountID.Authorize);}
    public void authorize(AccountID val) { put(AccountID.Authorize, val);}
}
