package com.ripple.core.types.known.tx.txns;

import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.serialized.enums.TransactionType;
import com.ripple.core.types.known.tx.Transaction;

public class PaymentChannelFund extends Transaction {
    public PaymentChannelFund() {
        super(TransactionType.PaymentChannelFund);
    }
    public boolean hasExpiration() {return has(UInt32.Expiration);}

    public UInt32 expiration() {return get(UInt32.Expiration);}
    public Hash256 channel() {return get(Hash256.Channel);}
    public Amount amount() {return get(Amount.Amount);}

    public void expiration(UInt32 val) { put(UInt32.Expiration, val);}
    public void channel(Hash256 val) { put(Hash256.Channel, val);}
    public void amount(Amount val) { put(Amount.Amount, val);}

}

