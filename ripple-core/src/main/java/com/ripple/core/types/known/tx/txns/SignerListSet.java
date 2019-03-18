package com.ripple.core.types.known.tx.txns;

import com.ripple.core.coretypes.STArray;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.serialized.enums.TransactionType;
import com.ripple.core.types.known.tx.Transaction;

public class SignerListSet extends Transaction {
    public SignerListSet() {
        super(TransactionType.SignerListSet);
    }
    public boolean hasSignerEntries() {return has(STArray.SignerEntries);}

    public UInt32 signerQuorum() {return get(UInt32.SignerQuorum);}
    public STArray signerEntries() {return get(STArray.SignerEntries);}

    public void signerQuorum(UInt32 val) { put(UInt32.SignerQuorum, val);}
    public void signerEntries(STArray val) { put(STArray.SignerEntries, val);}

}
