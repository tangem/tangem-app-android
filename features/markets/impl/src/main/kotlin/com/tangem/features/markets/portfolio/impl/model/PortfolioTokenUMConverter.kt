package com.tangem.features.markets.portfolio.impl.model

import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.markets.portfolio.impl.loader.PortfolioData
import com.tangem.features.markets.portfolio.impl.ui.state.PortfolioTokenUM
import com.tangem.features.markets.portfolio.impl.ui.state.QuickActionUM
import com.tangem.features.markets.portfolio.impl.ui.state.TokenActionsBSContentUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

/**
 * Converter from [UserWallet] and [CryptoCurrencyStatus] to [PortfolioTokenUM]
 *
* [REDACTED_AUTHOR]
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
            titleStateProvider = { TokenItemState.TitleState.Content(text = value.userWallet.name) },
            subtitleStateProvider = {
                TokenItemState.SubtitleState.TextContent(value = value.status.currency.name)
            },
            onItemClick = onTokenItemClick,
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
            actions = listOfNotNull(
                QuickActionUM.Buy.takeIf {
                    cryptoData.actions.any {
                        it is TokenActionsState.ActionState.Buy &&
                            it.unavailabilityReason == ScenarioUnavailabilityReason.None
                    }
                },
                QuickActionUM.Exchange.takeIf {
                    cryptoData.actions.any {
                        it is TokenActionsState.ActionState.Swap &&
                            it.unavailabilityReason == ScenarioUnavailabilityReason.None
                    }
                },
                QuickActionUM.Receive.takeIf {
                    cryptoData.actions.any {
                        it is TokenActionsState.ActionState.Receive &&
                            it.unavailabilityReason == ScenarioUnavailabilityReason.None
                    }
                },
            ).toImmutableList(),
            onQuickActionClick = {
                when (it) {
                    QuickActionUM.Buy -> tokenActionsHandler.handle(
                        action = TokenActionsBSContentUM.Action.Buy,
                        cryptoCurrencyData = cryptoData,
                    )
                    QuickActionUM.Exchange -> tokenActionsHandler.handle(
                        action = TokenActionsBSContentUM.Action.Exchange,
                        cryptoCurrencyData = cryptoData,
                    )
                    QuickActionUM.Receive -> tokenActionsHandler.handle(
                        action = TokenActionsBSContentUM.Action.Receive,
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
}
