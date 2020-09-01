package com.tangem.tap.common.redux.global

import com.tangem.blockchain.common.WalletManager
import com.tangem.commands.Card
import org.rekotlin.StateType
import java.math.BigDecimal

data class GlobalState(
        val card: Card? = null,
        val walletManager: WalletManager? = null,
        val fiatRates: FiatRates = FiatRates(emptyMap()),
) : StateType

data class FiatRates(
        val rates: Map<String, BigDecimal>
) {
    fun getRateForCryptoCurrency(currency: String): BigDecimal? {
        return rates[currency]
    }
}


