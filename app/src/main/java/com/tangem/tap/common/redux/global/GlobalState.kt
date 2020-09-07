package com.tangem.tap.common.redux.global

import com.tangem.tap.domain.TapWalletManager
import com.tangem.tap.domain.tasks.ScanNoteResponse
import org.rekotlin.StateType
import java.math.BigDecimal

data class GlobalState(
        val scanNoteResponse: ScanNoteResponse? = null,
        val tapWalletManager: TapWalletManager = TapWalletManager(),
        val fiatRates: FiatRates = FiatRates(emptyMap()),
) : StateType

data class FiatRates(
        val rates: Map<String, BigDecimal>
) {
    fun getRateForCryptoCurrency(currency: String): BigDecimal? {
        return rates[currency]
    }
}



