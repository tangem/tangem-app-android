package com.ripple.core.types.known.sle;

import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.STObject;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.fields.Field;
import com.ripple.core.formats.LEFormat;
import com.ripple.core.serialized.enums.LedgerEntryType;
import com.ripple.core.types.known.sle.entries.AccountRoot;
import com.ripple.core.types.known.sle.entries.DirectoryNode;
import com.ripple.core.types.known.sle.entries.Offer;
import com.ripple.core.types.known.sle.entries.RippleState;

import java.util.TreeSet;

public class LedgerEntry extends STObject {
    public LedgerEntry(LedgerEntryType type) {
        setFormat(LEFormat.formats.get(type));
        put(Field.LedgerEntryType, type);
    }

    public LedgerEntryType ledgerEntryType() {return ledgerEntryType(this);}
    public Hash256 index() { return get(Hash256.index); }
    public UInt32 flags() {return get(UInt32.Flags);}
    public Hash256 ledgerIndex() {return get(Hash256.LedgerIndex);}

    public void flags(UInt32 val) {put(Field.Flags, val);}
    public void ledgerIndex(Hash256 val) {put(Field.LedgerIndex, val);}

    public boolean hasLedgerIndex() {return has(Hash256.LedgerIndex);}

    public TreeSet<AccountID> owners() {
        TreeSet<AccountID> owners = new TreeSet<>();

        if (has(Field.LowLimit)) {
            owners.add(get(Amount.LowLimit).issuer());
        }
        if (has(Field.HighLimit)) {
            owners.add(get(Amount.HighLimit).issuer());
        }
        if (has(Field.Account)) {
            owners.add(get(AccountID.Account));
        }

        return owners;
    }

    public void index(Hash256 index) {
        put(Hash256.index, index);
    }

    public void setDefaults() {
        if (flags() == null) {
            flags(UInt32.ZERO);
        }
    }
}
