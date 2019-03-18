package com.ripple.core.types.known.sle;

import com.ripple.core.coretypes.Vector256;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.serialized.enums.LedgerEntryType;

public class LedgerHashes extends LedgerEntry {
    public LedgerHashes() {
        super(LedgerEntryType.LedgerHashes);
    }

    public Vector256 hashes() {
        return get(Vector256.Hashes);
    }

    public void hashes(Vector256 hashes) {
        put(Vector256.Hashes, hashes);
    }

    public UInt32 lastLedgerSequence() {
        return get(UInt32.LastLedgerSequence);
    }

    public UInt32 firstLedgerSequence() {return get(UInt32.FirstLedgerSequence);}

    public void firstLedgerSequence(UInt32 val) { put(UInt32.FirstLedgerSequence, val);}

    public void lastLedgerSequence(UInt32 val) { put(UInt32.LastLedgerSequence, val);}

    public boolean hasFirstLedgerSequence() {return has(UInt32.FirstLedgerSequence);}
    public boolean hasLastLedgerSequence() {return has(UInt32.LastLedgerSequence);}
}
