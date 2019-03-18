package com.ripple.core.types.known.sle.entries;

import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.types.known.tx.Transaction;

import java.util.ArrayList;

public interface IHasOwners {
    ArrayList<Hash256> ownerDirectoryIndexes(Transaction nullableContext);
    AccountID account(Transaction nullableContext);
}
