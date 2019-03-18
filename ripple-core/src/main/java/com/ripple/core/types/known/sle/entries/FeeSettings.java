package com.ripple.core.types.known.sle.entries;

import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.coretypes.uint.UInt64;
import com.ripple.core.serialized.enums.LedgerEntryType;
import com.ripple.core.types.known.sle.LedgerEntry;

public class FeeSettings extends LedgerEntry {
    public FeeSettings() {
        super(LedgerEntryType.Amendments);
    }
    public UInt32 referenceFeeUnits() {return get(UInt32.ReferenceFeeUnits);}
    public UInt32 reserveBase() {return get(UInt32.ReserveBase);}
    public UInt32 reserveIncrement() {return get(UInt32.ReserveIncrement);}
    public UInt64 baseFee() {return get(UInt64.BaseFee);}

    public void baseFee(UInt64 val) { put(UInt64.BaseFee, val);}
    public void referenceFeeUnits(UInt32 val) { put(UInt32.ReferenceFeeUnits, val);}
    public void reserveBase(UInt32 val) { put(UInt32.ReserveBase, val);}
    public void reserveIncrement(UInt32 val) { put(UInt32.ReserveIncrement, val);}

}
