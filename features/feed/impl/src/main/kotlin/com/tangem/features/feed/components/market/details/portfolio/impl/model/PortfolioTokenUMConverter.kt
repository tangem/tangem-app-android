package com.tangem.features.feed.components.market.details.portfolio.impl.model

import com.tangem.common.ui.markets.action.CryptoCurrencyData
import com.tangem.common.ui.markets.action.QuickActionsConverter.quickActions
import com.tangem.common.ui.markets.action.TokenActionsHandler
import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.feed.components.market.details.portfolio.impl.ui.state.PortfolioTokenUM
import com.tangem.utils.converter.Converter

/**
 * Converter from [UserWallet] and [CryptoCurrencyStatus] to [PortfolioTokenUM]
 *
[REDACTED_AUTHOR]
 */
internal class PortfolioTokenUMConverter(
    private val appCurrency: AppCurrency,
    private val isBalanceHidden: Boolean,
    private val onTokenItemClick: (CryptoCurrencyStatus) -> Unit,
    private val tokenActionsHandler: TokenActionsHandler,
    private val isRedesignEnabled: Boolean,
) : Converter<CryptoCurrencyData, PortfolioTokenUM> {

    fun convertV2(
        value: CryptoCurrencyData,
        isQuickActionsShown: Boolean,
        onTokenItemClick: (UserWallet, CryptoCurrencyStatus) -> Unit,
    ): PortfolioTokenUM {
        val tokenItemStateConverter = TokenItemStateConverter(
            appCurrency = appCurrency,
            onItemClick = { _, status -> onTokenItemClick(value.userWallet, status) },
        )
        return PortfolioTokenUM(
            tokenItemState = tokenItemStateConverter.convert(value = value.status),
            walletId = value.userWallet.walletId,
            isBalanceHidden = isBalanceHidden,
            isQuickActionsShown = isQuickActionsShown,
            quickActions = quickActions(
                cryptoData = value,
                tokenActionsHandler = tokenActionsHandler,
                isRedesignEnabled = isRedesignEnabled,
            ),
        )
    }

    override fun convert(value: CryptoCurrencyData): PortfolioTokenUM {
        val tokenItemStateConverter = TokenItemStateConverter(
            appCurrency = appCurrency,
            titleStateProvider = { TokenItemState.TitleState.Content(text = stringReference(value.userWallet.name)) },
            subtitleStateProvider = {
                TokenItemState.SubtitleState.TextContent(value = stringReference(value.status.currency.name))
            },
            onItemClick = { _, status -> onTokenItemClick(status) },
        )

        return PortfolioTokenUM(
            tokenItemState = tokenItemStateConverter.convert(value = value.status),
            walletId = value.userWallet.walletId,
            isBalanceHidden = isBalanceHidden,
            isQuickActionsShown = false,
            quickActions = quickActions(
                cryptoData = value,
                tokenActionsHandler = tokenActionsHandler,
                isRedesignEnabled = isRedesignEnabled,
            ),
        )
    }
}