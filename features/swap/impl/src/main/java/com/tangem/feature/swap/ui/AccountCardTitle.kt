package com.tangem.feature.swap.ui

import com.tangem.common.ui.account.AccountIconUM
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.account.Account
import com.tangem.feature.swap.presentation.R

internal fun accountCardTitle(account: Account?, isAccountsMode: Boolean, isFromCard: Boolean): AccountTitleUM {
    val (prefix, placeholder) = if (isFromCard) {
        R.string.swapping_from_account_title to R.string.swapping_from_title_v2
    } else {
        R.string.swapping_to_account_title to R.string.swapping_to_title
    }
    return when {
        account == null || !isAccountsMode -> AccountTitleUM.Text(resourceReference(placeholder))
        account is Account.Prediction -> AccountTitleUM.Account.prediction(prefixText = resourceReference(prefix))
        account is Account.Collectibles -> AccountTitleUM.Account.collectibles(prefixText = resourceReference(prefix))
        account is Account.Money -> AccountTitleUM.Account.moneyAccount(prefixText = resourceReference(prefix))
        else -> AccountTitleUM.Account(
            prefixText = resourceReference(prefix),
            name = account.accountName.toUM().value,
            icon = account.toIconUM(),
        )
    }
}

private fun Account.toIconUM(): AccountIconUM = when (this) {
    is Account.CryptoPortfolio -> CryptoPortfolioIconConverter.convert(icon)
    is Account.Payment -> AccountIconUM.Payment
    is Account.Money -> AccountIconUM.Money
    is Account.Prediction -> AccountIconUM.Prediction
    is Account.Collectibles -> AccountIconUM.Collectibles
}