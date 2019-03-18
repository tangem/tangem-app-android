package com.ripple.core.types.known.tx.txns;

import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.serialized.enums.TransactionType;
import com.ripple.core.types.known.tx.Transaction;

public class TicketCreate extends Transaction {
    public TicketCreate() {
        super(TransactionType.TicketCreate);
    }

    public boolean hasExpiration() {return has(UInt32.Expiration);}
    public boolean hasTarget() {return has(AccountID.Target);}

    public UInt32 expiration() {return get(UInt32.Expiration);}
    public AccountID target() {return get(AccountID.Target);}

    public void expiration(UInt32 val) { put(UInt32.Expiration, val);}
    public void target(AccountID val) { put(AccountID.Target, val);}

}
