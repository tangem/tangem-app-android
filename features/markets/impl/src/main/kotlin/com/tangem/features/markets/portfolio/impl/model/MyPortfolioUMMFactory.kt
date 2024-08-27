package com.tangem.features.markets.portfolio.impl.model

import arrow.core.Either
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM

/**
 * Factory for creating [MyPortfolioUM]
 *
 * @property onAddClick       callback when user wants to add new token
 * @property onTokenItemClick callback when user wants to see actions with token
 *
 * @author Andrew Khokhlov on 25/08/2024
 */
internal class MyPortfolioUMMFactory(
    private val onAddClick: () -> Unit,
    private val onTokenItemClick: (CryptoCurrencyStatus) -> Unit,
) {

    /**
     * Create [MyPortfolioUM] from data set
     *
     * @param walletsWithCurrencyStatuses wallets with currency statuses
     * @param availableNetworks           available networks
     * @param appCurrency                 app currency
     * @param isBalanceHidden             flag that indicates if balance should be hidden
     */
    fun create(
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

        if (availableWalletsWithStatuses.isEmpty()) {
            return MyPortfolioUM.AddFirstToken(onAddClick = onAddClick)
        }

        return TokensPortfolioUMConverter(
            appCurrency = appCurrency,
            isBalanceHidden = isBalanceHidden,
            availableNetworks = availableNetworks,
            onAddClick = onAddClick,
            onTokenItemClick = onTokenItemClick,
        )
            .convert(availableWalletsWithStatuses)
    }

    private fun Map<UserWallet, List<Either<CurrencyStatusError, CryptoCurrencyStatus>>>.filterAvailable(
        networks: List<TokenMarketInfo.Network>,
    ): Map<UserWallet, List<CryptoCurrencyStatus>> {
        val networkIds = networks.map { it.networkId }

        return mapValues { entry ->
            entry.value.mapNotNull { maybeStatus ->
                maybeStatus.getOrNull()
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
}
