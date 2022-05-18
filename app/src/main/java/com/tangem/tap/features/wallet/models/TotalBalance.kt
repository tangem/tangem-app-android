package com.tangem.tap.features.wallet.models

import com.tangem.tap.common.entities.FiatCurrency
import java.math.BigDecimal

data class TotalBalance(
    val state: State,
    val fiatAmount: BigDecimal,
    val fiatCurrency: FiatCurrency,
) {
    enum class State {
        Loading,
        Failed,
        Success
    }
}