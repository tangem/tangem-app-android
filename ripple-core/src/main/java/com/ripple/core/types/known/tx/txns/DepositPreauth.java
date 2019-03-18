package com.ripple.core.types.known.tx.txns;

import com.ripple.core.coretypes.AccountID;
import com.ripple.core.serialized.enums.TransactionType;
import com.ripple.core.types.known.tx.Transaction;

public class DepositPreauth extends Transaction {
    public DepositPreauth() {
        super(TransactionType.DepositPreauth);
    }
    public boolean hasAuthorize() {return has(AccountID.Authorize);}
    public AccountID authorize() {return get(AccountID.Authorize);}
    public void authorize(AccountID val) { put(AccountID.Authorize, val);}

    public boolean hasUnauthorize() {return has(AccountID.Unauthorize);}
    public AccountID unauthorize() {return get(AccountID.Unauthorize);}
    public void unauthorize(AccountID val) { put(AccountID.Unauthorize, val);}

}
