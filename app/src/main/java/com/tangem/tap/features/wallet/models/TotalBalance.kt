package com.tangem.tap.features.wallet.models

import com.tangem.tap.common.redux.global.FiatCurrencyName
import java.math.BigDecimal

data class TotalBalance(
    val state: State,
    val fiatAmount: BigDecimal,
    val fiatCurrencyName: FiatCurrencyName,
) {
    enum class State {
        Loading,
        Failed,
        Success
    }
}