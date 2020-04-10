package com.tangem.wallet.ducatus;

import com.tangem.wallet.CoinEngine;
import com.tangem.wallet.btc.BtcData;

import java.math.BigDecimal;

public class DucatusData extends BtcData {
    public CoinEngine.InternalAmount getBalanceInInternalUnits() {
        if (balanceConfirmed != null) {
            return new CoinEngine.InternalAmount(BigDecimal.valueOf(balanceConfirmed), "Satoshi");
        } else {
            return null;
        }
    }
}
