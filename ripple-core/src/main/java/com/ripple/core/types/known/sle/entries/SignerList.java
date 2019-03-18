package com.ripple.core.types.known.sle.entries;

import com.ripple.core.coretypes.STArray;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.coretypes.uint.UInt64;
import com.ripple.core.fields.Field;
import com.ripple.core.serialized.enums.LedgerEntryType;

public class SignerList extends IndexedLedgerEntry {
    public SignerList() {
        super(LedgerEntryType.SignerList);
    }

    @Override
    public void setDefaults() {
        super.setDefaults();
        if (!has(Field.SignerListID)) {
            put(UInt32.SignerListID, UInt32.ZERO);
        }
    }

    public STArray signerEntries() {return get(STArray.SignerEntries);}
    public UInt32 signerListID() {return get(UInt32.SignerListID);}
    public UInt32 signerQuorum() {return get(UInt32.SignerQuorum);}
    public UInt64 ownerNode() {return get(UInt64.OwnerNode);}

    public void ownerNode(UInt64 val) { put(UInt64.OwnerNode, val);}
    public void signerEntries(STArray val) { put(STArray.SignerEntries, val);}
    public void signerListID(UInt32 val) { put(UInt32.SignerListID, val);}
    public void signerQuorum(UInt32 val) { put(UInt32.SignerQuorum, val);}

}
