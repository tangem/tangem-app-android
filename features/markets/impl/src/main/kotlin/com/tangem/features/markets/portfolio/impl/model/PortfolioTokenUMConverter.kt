package com.tangem.features.markets.portfolio.impl.model

import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.markets.portfolio.impl.loader.PortfolioData
import com.tangem.features.markets.portfolio.impl.ui.state.PortfolioTokenUM
import com.tangem.features.markets.portfolio.impl.ui.state.QuickActionUM
import com.tangem.features.markets.portfolio.impl.ui.state.TokenActionsBSContentUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

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
) : Converter<PortfolioData.CryptoCurrencyData, PortfolioTokenUM> {

    override fun convert(value: PortfolioData.CryptoCurrencyData): PortfolioTokenUM {
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
            quickActions = quickActions(cryptoData = value),
        )
    }

    private fun quickActions(cryptoData: PortfolioData.CryptoCurrencyData): PortfolioTokenUM.QuickActions {
        return PortfolioTokenUM.QuickActions(
            actions = toQuickActions(cryptoData.actions),
            onQuickActionClick = {
                when (it) {
                    QuickActionUM.Buy -> tokenActionsHandler.handle(
                        action = TokenActionsBSContentUM.Action.Buy,
                        cryptoCurrencyData = cryptoData,
                    )
                    is QuickActionUM.Exchange -> tokenActionsHandler.handle(
                        action = TokenActionsBSContentUM.Action.Exchange,
                        cryptoCurrencyData = cryptoData,
                    )
                    QuickActionUM.Receive -> tokenActionsHandler.handle(
                        action = TokenActionsBSContentUM.Action.Receive,
                        cryptoCurrencyData = cryptoData,
                    )
                    QuickActionUM.Stake -> tokenActionsHandler.handle(
                        action = TokenActionsBSContentUM.Action.Stake,
                        cryptoCurrencyData = cryptoData,
                    )
                }
            },
            onQuickActionLongClick = {
                if (it == QuickActionUM.Receive) {
                    tokenActionsHandler.handle(
                        action = TokenActionsBSContentUM.Action.CopyAddress,
                        cryptoCurrencyData = cryptoData,
                    )
                }
            },
        )
    }

    private fun toQuickActions(actions: List<TokenActionsState.ActionState>) = buildList {
        actions.forEach { action ->
            if (action.unavailabilityReason == ScenarioUnavailabilityReason.None) {
                when (action) {
                    is TokenActionsState.ActionState.Buy -> QuickActionUM.Buy
                    is TokenActionsState.ActionState.Swap -> QuickActionUM.Exchange(showBadge = action.showBadge)
                    is TokenActionsState.ActionState.Receive -> QuickActionUM.Receive
                    is TokenActionsState.ActionState.Stake -> QuickActionUM.Stake
                    else -> null
                }?.let(::add)
            }
        }
    }.toImmutableList()
}