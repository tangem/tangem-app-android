package com.ripple.core.types.known.tx.txns;

import com.ripple.core.coretypes.AccountID;
import com.ripple.core.serialized.enums.TransactionType;
import com.ripple.core.types.known.tx.Transaction;

public class SetRegularKey extends Transaction {
    public SetRegularKey() {
        super(TransactionType.SetRegularKey);
    }
    public boolean hasRegularKey() {return has(AccountID.RegularKey);}
    public AccountID regularKey() {return get(AccountID.RegularKey);}
    public void regularKey(AccountID val) { put(AccountID.RegularKey, val);}
}
