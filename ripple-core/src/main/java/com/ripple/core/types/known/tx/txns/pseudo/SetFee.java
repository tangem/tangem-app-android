package com.ripple.core.types.known.tx.txns.pseudo;

import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.coretypes.uint.UInt64;
import com.ripple.core.serialized.enums.TransactionType;
import com.ripple.core.types.known.tx.Transaction;

public class SetFee extends Transaction {
    public SetFee() {
        super(TransactionType.SetFee);
    }
    public boolean hasLedgerSequence() {return has(UInt32.LedgerSequence);}

    public void ledgerSequence(UInt32 val) { put(UInt32.LedgerSequence, val);}
    public void referenceFeeUnits(UInt32 val) { put(UInt32.ReferenceFeeUnits, val);}
    public void reserveBase(UInt32 val) { put(UInt32.ReserveBase, val);}
    public void reserveIncrement(UInt32 val) { put(UInt32.ReserveIncrement, val);}
    public void baseFee(UInt64 val) { put(UInt64.BaseFee, val);}

    public UInt32 ledgerSequence() {return get(UInt32.LedgerSequence);}
    public UInt32 referenceFeeUnits() {return get(UInt32.ReferenceFeeUnits);}
    public UInt32 reserveBase() {return get(UInt32.ReserveBase);}
    public UInt32 reserveIncrement() {return get(UInt32.ReserveIncrement);}
    public UInt64 baseFee() {return get(UInt64.BaseFee);}
}
