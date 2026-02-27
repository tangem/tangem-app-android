package com.tangem.common.ui.account

import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.utils.converter.Converter

class AccountIconItemStateConverter(
    val size: AccountIconSize = AccountIconSize.Default,
) : Converter<Account, CurrencyIconState.CryptoPortfolio> {

    override fun convert(value: Account): CurrencyIconState.CryptoPortfolio = when (value) {
        is Account.CryptoPortfolio -> when {
            value.icon.value == CryptoPortfolioIcon.Icon.Letter -> CurrencyIconState.CryptoPortfolio.Letter(
                char = value.accountName.toUM().value,
                color = value.icon.color.getUiColor(),
                isGrayscale = false,
                size = size,
            )
            else -> CurrencyIconState.CryptoPortfolio.Icon(
                resId = value.icon.value.getResId(),
                color = value.icon.color.getUiColor(),
                isGrayscale = false,
                size = size,
            )
        }
        is Account.Payment -> TODO("[REDACTED_JIRA]")
    }
}