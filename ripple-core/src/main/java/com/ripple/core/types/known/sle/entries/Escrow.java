package com.ripple.core.types.known.sle.entries;

import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.Blob;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.coretypes.hash.Index;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.coretypes.uint.UInt64;
import com.ripple.core.serialized.enums.LedgerEntryType;
import com.ripple.core.types.known.tx.Transaction;

import java.util.ArrayList;

public class Escrow extends IndexedLedgerEntry {
    public Escrow() {
        super(LedgerEntryType.Escrow);
    }

    @Override
    public void setDefaults() {
        super.setDefaults();
        if (multiParty()) {
            if (!has(UInt64.DestinationNode)) {
                put(UInt64.DestinationNode, UInt64.ZERO);
            }
        }
    }

    @Override
    public ArrayList<Hash256> ownerDirectoryIndexes(Transaction nullableContext) {
        ArrayList<Hash256> indexes = super.ownerDirectoryIndexes(nullableContext);
        if (multiParty()) {
            Hash256 destinationOwnerDir =
                    Index.ownerDirectory(destination());
            indexes.add(Index.directoryNode(
                    destinationOwnerDir, destinationNode()));
        }
        return indexes;
    }

    private boolean multiParty() {
        return !get(AccountID.Account).equals(destination());
    }

    public AccountID account() {return get(AccountID.Account);}
    public AccountID destination() {return get(AccountID.Destination);}
    public Amount amount() {return get(Amount.Amount);}
    public Blob condition() {return get(Blob.Condition);}
    public UInt32 cancelAfter() {return get(UInt32.CancelAfter);}
    public UInt32 destinationTag() {return get(UInt32.DestinationTag);}
    public UInt32 finishAfter() {return get(UInt32.FinishAfter);}
    public UInt32 sourceTag() {return get(UInt32.SourceTag);}
    public UInt64 destinationNode() {return get(UInt64.DestinationNode);}
    public UInt64 ownerNode() {return get(UInt64.OwnerNode);}

    public void amount(Amount val) { put(Amount.Amount, val);}
    public void cancelAfter(UInt32 val) { put(UInt32.CancelAfter, val);}
    public void condition(Blob val) { put(Blob.Condition, val);}
    public void destination(AccountID val) { put(AccountID.Destination, val);}
    public void destinationNode(UInt64 val) { put(UInt64.DestinationNode, val);}
    public void destinationTag(UInt32 val) { put(UInt32.DestinationTag, val);}
    public void finishAfter(UInt32 val) { put(UInt32.FinishAfter, val);}
    public void ownerNode(UInt64 val) { put(UInt64.OwnerNode, val);}
    public void sourceTag(UInt32 val) { put(UInt32.SourceTag, val);}

    public boolean hasCancelAfter() {return has(UInt32.CancelAfter);}
    public boolean hasCondition() {return has(Blob.Condition);}
    public boolean hasDestinationNode() {return has(UInt64.DestinationNode);}
    public boolean hasDestinationTag() {return has(UInt32.DestinationTag);}
    public boolean hasFinishAfter() {return has(UInt32.FinishAfter);}
    public boolean hasSourceTag() {return has(UInt32.SourceTag);}

}
