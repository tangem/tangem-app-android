package com.tangem.features.onramp.swap.entity.utils

import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.account.Account
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.swap.entity.ExchangeCardUM

/** Create empty exchange "from" card */
internal fun createEmptyExchangeFrom(): ExchangeCardUM.Empty {
    return ExchangeCardUM.Empty(
        titleUM = ExchangeCardUM.TitleUM.Text(resourceReference(id = R.string.swapping_from_title)),
        subtitleReference = resourceReference(id = R.string.action_buttons_you_want_to_swap),
    )
}

/** Create empty exchange "to" card */
internal fun createEmptyExchangeTo(): ExchangeCardUM.Empty {
    return ExchangeCardUM.Empty(
        titleUM = ExchangeCardUM.TitleUM.Text(resourceReference(id = R.string.swapping_to_title)),
        subtitleReference = resourceReference(id = R.string.action_buttons_you_want_to_receive),
    )
}

/**
 * Convert from [ExchangeCardUM] to [ExchangeCardUM.Filled]
 *
 * @param selectedTokenItemState token item state
 * @param removeButtonUM         remove button UI model
 */
internal fun ExchangeCardUM.toFilled(
    selectedTokenItemState: TokenItemState,
    account: Account.Crypto?,
    isAccountsMode: Boolean,
    isFromCurrency: Boolean,
    removeButtonUM: ExchangeCardUM.RemoveButtonUM? = null,
): ExchangeCardUM.Filled {
    return ExchangeCardUM.Filled(
        titleUM = if (account != null && isAccountsMode) {
            ExchangeCardUM.TitleUM.Account(
                prefixText = if (isFromCurrency) {
                    resourceReference(R.string.common_from)
                } else {
                    resourceReference(R.string.common_to)
                },
                name = account.accountName.toUM().value,
                icon = CryptoPortfolioIconConverter.convert(account.icon),
            )
        } else {
            titleUM
        },
        tokenItemState = selectedTokenItemState,
        removeButtonUM = removeButtonUM,
    )
}