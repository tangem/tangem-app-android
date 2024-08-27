package com.tangem.features.markets.portfolio.impl.model

import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.markets.portfolio.impl.ui.state.PortfolioTokenUM
import com.tangem.utils.converter.Converter

/**
 * Converter from [UserWallet] and [CryptoCurrencyStatus] to [PortfolioTokenUM]
 *
* [REDACTED_AUTHOR]
 */
internal class PortfolioTokenUMConverter(
    private val appCurrency: AppCurrency,
    private val isBalanceHidden: Boolean,
    private val onTokenItemClick: (CryptoCurrencyStatus) -> Unit,
) : Converter<Map.Entry<UserWallet, CryptoCurrencyStatus>, PortfolioTokenUM> {

    override fun convert(value: Map.Entry<UserWallet, CryptoCurrencyStatus>): PortfolioTokenUM {
        val (userWallet, cryptoCurrencyStatus) = value

        val tokenItemStateConverter = TokenItemStateConverter(
            appCurrency = appCurrency,
            titleStateProvider = { TokenItemState.TitleState.Content(text = userWallet.name) },
            subtitleStateProvider = {
                TokenItemState.SubtitleState.TextContent(value = cryptoCurrencyStatus.currency.name)
            },
            onItemClick = onTokenItemClick,
            onItemLongClick = null,
        )

        return PortfolioTokenUM(
            tokenItemState = tokenItemStateConverter.convert(value = cryptoCurrencyStatus),
            isBalanceHidden = isBalanceHidden,
            isQuickActionsShown = false,
            onQuickActionClick = {
// [REDACTED_TODO_COMMENT]
            },
        )
    }
}
