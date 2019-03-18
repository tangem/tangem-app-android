package com.ripple.core.types.known.sle.entries;

import com.ripple.core.coretypes.STArray;
import com.ripple.core.coretypes.Vector256;
import com.ripple.core.serialized.enums.LedgerEntryType;
import com.ripple.core.types.known.sle.LedgerEntry;

public class Amendments extends LedgerEntry {
    public Amendments() {
        super(LedgerEntryType.Amendments);
    }

    public STArray majorities() {return get(STArray.Majorities);}
    public Vector256 amendments() {return get(Vector256.Amendments);}

    public void amendments(Vector256 val) { put(Vector256.Amendments, val);}
    public void majorities(STArray val) { put(STArray.Majorities, val);}

    public boolean hasAmendments() {return has(Vector256.Amendments);}
    public boolean hasMajorities() {return has(STArray.Majorities);}

}
