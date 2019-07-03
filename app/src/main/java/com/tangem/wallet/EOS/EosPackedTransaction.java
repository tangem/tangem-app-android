package com.tangem.wallet.EOS;

import io.jafka.jeos.core.common.transaction.PackedTransaction;

public class EosPackedTransaction extends PackedTransaction {
    private Long expirationSec;

    public Long getExpirationSec() {
        return expirationSec;
    }

    public void setExpirationSec(Long expirationSec) {
        this.expirationSec = expirationSec;
    }
}
