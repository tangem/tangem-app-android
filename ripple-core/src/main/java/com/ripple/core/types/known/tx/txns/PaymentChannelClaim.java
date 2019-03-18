package com.ripple.core.types.known.tx.txns;

import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.Blob;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.serialized.enums.TransactionType;
import com.ripple.core.types.known.tx.Transaction;

public class PaymentChannelClaim extends Transaction {
    public PaymentChannelClaim() {
        super(TransactionType.PaymentChannelClaim);
    }

    public boolean hasAmount() {return has(Amount.Amount);}
    public boolean hasBalance() {return has(Amount.Balance);}
    public boolean hasPublicKey() {return has(Blob.PublicKey);}
    public boolean hasSignature() {return has(Blob.Signature);}

    public void channel(Hash256 val) { put(Hash256.Channel, val);}
    public void amount(Amount val) { put(Amount.Amount, val);}
    public void balance(Amount val) { put(Amount.Balance, val);}
    public void publicKey(Blob val) { put(Blob.PublicKey, val);}
    public void signature(Blob val) { put(Blob.Signature, val);}

    public Hash256 channel() {return get(Hash256.Channel);}
    public Amount amount() {return get(Amount.Amount);}
    public Amount balance() {return get(Amount.Balance);}
    public Blob publicKey() {return get(Blob.PublicKey);}
    public Blob signature() {return get(Blob.Signature);}

}
