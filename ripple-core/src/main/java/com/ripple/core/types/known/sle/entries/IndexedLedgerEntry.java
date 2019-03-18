package com.ripple.core.types.known.sle.entries;

import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.coretypes.hash.Index;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.coretypes.uint.UInt64;
import com.ripple.core.fields.Field;
import com.ripple.core.serialized.enums.LedgerEntryType;
import com.ripple.core.types.known.sle.ThreadedLedgerEntry;
import com.ripple.core.types.known.tx.Transaction;

import java.util.ArrayList;

public abstract class IndexedLedgerEntry extends ThreadedLedgerEntry implements IHasOwners {
    public IndexedLedgerEntry(LedgerEntryType type) {
        super(type);
    }

    @Override
    public void setDefaults() {
        super.setDefaults();
        if (!has(Field.OwnerNode)) {
            put(UInt64.OwnerNode, UInt64.ZERO);
        }
    }

    @Override
    public AccountID account(Transaction nullableContext) {
        AccountID account = account();
        if (account == null) {
            if (nullableContext != null) {
                account = nullableContext.account();
            } else {
                throw new IllegalStateException("Cant determine account for: " + prettyJSON());
            }
        }
        return account;
    }

    @Override
    public ArrayList<Hash256> ownerDirectoryIndexes(Transaction nullableContext) {
        Hash256 ownerDir = Index.ownerDirectory(account(nullableContext));
        ArrayList<Hash256> indexes = new ArrayList<>();
        indexes.add(Index.directoryNode(ownerDir, ownerNode()));
        return indexes;
    }

    private UInt64 ownerNode() {
        return get(UInt64.OwnerNode);
    }

    private AccountID account() {
        return get(AccountID.Account);
    }
}
