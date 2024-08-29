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
 * @author Andrew Khokhlov on 26/08/2024
 */
internal class PortfolioTokenUMConverter(
    private val appCurrency: AppCurrency,
    private val isBalanceHidden: Boolean,
    private val onTokenItemClick: (CryptoCurrencyStatus) -> Unit,
) : Converter<Pair<UserWallet, CryptoCurrencyStatus>, PortfolioTokenUM> {

    override fun convert(value: Pair<UserWallet, CryptoCurrencyStatus>): PortfolioTokenUM {
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
                // TODO: https://tangem.atlassian.net/browse/AND-7988
            },
        )
    }
}
