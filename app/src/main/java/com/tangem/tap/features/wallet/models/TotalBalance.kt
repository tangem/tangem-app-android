package com.tangem.tap.features.wallet.models

import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.features.wallet.redux.ProgressState
import java.math.BigDecimal

data class TotalBalance(
    val state: ProgressState,
    val fiatAmount: BigDecimal,
    val fiatCurrency: FiatCurrency,
)