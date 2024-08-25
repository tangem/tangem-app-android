package com.tangem.features.markets.portfolio.impl.model

import arrow.core.Either
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM.Tokens.AddButtonState
import com.tangem.features.markets.portfolio.impl.ui.state.PortfolioTokenUM
import com.tangem.features.markets.portfolio.impl.ui.state.QuickActionUM
import kotlinx.collections.immutable.toImmutableList

/**
[REDACTED_AUTHOR]
 */
internal class MyPortfolioUMMapper(
    private val onAddClick: () -> Unit,
    private val onTokenItemClick: (CryptoCurrencyStatus) -> Unit,
    private val onTokenItemLongClick: (CryptoCurrencyStatus) -> Unit,
) {

    fun map(
        walletsWithCurrencyStatuses: Map<UserWallet, List<Either<CurrencyStatusError, CryptoCurrencyStatus>>>,
        availableNetworks: List<TokenMarketInfo.Network>?,
        appCurrency: AppCurrency,
        isBalanceHidden: Boolean,
    ): MyPortfolioUM {
        if (availableNetworks == null) return MyPortfolioUM.Loading

        if (availableNetworks.isEmpty()) return MyPortfolioUM.Unavailable

        val availableWalletsWithStatuses = walletsWithCurrencyStatuses
            .filterAvailable(networks = availableNetworks)
            .flatMapStatuses()

        if (availableWalletsWithStatuses.isEmpty()) return MyPortfolioUM.AddFirstToken(onAddClick = onAddClick)

        return availableWalletsWithStatuses.toTokensState(availableNetworks, appCurrency, isBalanceHidden)
    }

    private fun Map<UserWallet, List<Either<CurrencyStatusError, CryptoCurrencyStatus>>>.filterAvailable(
        networks: List<TokenMarketInfo.Network>,
    ): Map<UserWallet, List<CryptoCurrencyStatus>> {
        val networkIds = networks.map { it.networkId }

        return mapValues { entry ->
            entry.value.mapNotNull { maybeStatus ->
                maybeStatus
                    .getOrNull()
                    ?.takeIf { networkIds.contains(it.currency.network.backendId) }
            }
        }
    }

    private fun Map<UserWallet, List<CryptoCurrencyStatus>>.flatMapStatuses(): Map<UserWallet, CryptoCurrencyStatus> {
        return this
            .flatMap { entry ->
                entry.value.map { entry.key to it }
            }
            .toMap()
    }

    private fun Map<UserWallet, CryptoCurrencyStatus>.toTokensState(
        availableNetworks: List<TokenMarketInfo.Network>,
        appCurrency: AppCurrency,
        isBalanceHidden: Boolean,
    ): MyPortfolioUM.Tokens {
        val tokenItemStateConverter = TokenItemStateConverter(
            appCurrency = appCurrency,
            onItemClick = onTokenItemClick,
            onItemLongClick = onTokenItemLongClick,
        )

        return MyPortfolioUM.Tokens(
            tokens = map {
                PortfolioTokenUM(
                    tokenItemState = tokenItemStateConverter.convert(value = it.toPair()),
                    isBalanceHidden = isBalanceHidden,
                    isQuickActionsShown = false,
                    onQuickActionClick = {
                        when (it) {
                            QuickActionUM.Buy -> TODO()
                            QuickActionUM.Exchange -> TODO()
                            QuickActionUM.Receive -> TODO()
                        }
                    },
                )
            }
                .toImmutableList(),
            buttonState = getAddButtonState(availableWalletsWithStatuses = this, availableNetworks = availableNetworks),
            onAddClick = onAddClick,
        )
    }

    private fun getAddButtonState(
        availableWalletsWithStatuses: Map<UserWallet, CryptoCurrencyStatus>,
        availableNetworks: List<TokenMarketInfo.Network>,
    ): AddButtonState {
        val networkIds = availableNetworks.map { it.networkId }

        val allNetworksAdded = availableWalletsWithStatuses.entries
            .groupBy(
                keySelector = { it.key },
                valueTransform = { it.value.currency.network.backendId },
            )
            .mapValues { it.value.toSet() }
            .all { it.value.containsAll(networkIds) }

        return if (allNetworksAdded) AddButtonState.Unavailable else AddButtonState.Available
    }
}