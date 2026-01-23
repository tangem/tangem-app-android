package com.tangem.common.ui.account

import com.tangem.domain.models.account.CryptoPortfolioIcon.Color
import com.tangem.domain.models.account.CryptoPortfolioIcon.Icon

sealed class AccountIconUM {
    data class CryptoPortfolio(val value: Icon, val color: Color) : AccountIconUM()

    data object Payment : AccountIconUM()
}