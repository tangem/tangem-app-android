package com.tangem.common.ui.account

import androidx.compose.runtime.Immutable
import com.tangem.domain.models.account.CryptoPortfolioIcon.Color
import com.tangem.domain.models.account.CryptoPortfolioIcon.Icon

@Immutable
sealed class AccountIconUM {
    data class CryptoPortfolio(val value: Icon, val color: Color) : AccountIconUM()

    data object Payment : AccountIconUM()

    data object Virtual : AccountIconUM() {
        val icon: Icon = Icon.Safe
        val color: Color = Color.VitalGreen
    }
}