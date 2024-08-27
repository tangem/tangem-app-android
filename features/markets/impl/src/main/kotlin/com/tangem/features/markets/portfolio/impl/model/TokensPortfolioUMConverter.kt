package com.tangem.features.markets.portfolio.impl.model

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM.Tokens.AddButtonState
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

/**
 * Converter from [Map] of [UserWallet] and [CryptoCurrencyStatus] to [MyPortfolioUM.Tokens]
 *
 * @author Andrew Khokhlov on 26/08/2024
 */
internal class TokensPortfolioUMConverter(
    private val appCurrency: AppCurrency,
    private val isBalanceHidden: Boolean,
    private val availableNetworks: List<TokenMarketInfo.Network>,
    private val onAddClick: () -> Unit,
    private val onTokenItemClick: (CryptoCurrencyStatus) -> Unit,
) : Converter<Map<UserWallet, CryptoCurrencyStatus>, MyPortfolioUM.Tokens> {

    override fun convert(value: Map<UserWallet, CryptoCurrencyStatus>): MyPortfolioUM.Tokens {
        return MyPortfolioUM.Tokens(
            tokens = PortfolioTokenUMConverter(appCurrency, isBalanceHidden, onTokenItemClick)
                .convertList(value.entries)
                .toImmutableList(),
            buttonState = getAddButtonState(
                availableWalletsWithStatuses = value,
                availableNetworks = availableNetworks,
            ),
            onAddClick = onAddClick,
        )
    }

    private fun getAddButtonState(
        availableWalletsWithStatuses: Map<UserWallet, CryptoCurrencyStatus>,
        availableNetworks: List<TokenMarketInfo.Network>,
    ): AddButtonState {
        val networkIds = availableNetworks.map { it.networkId }

        val allNetworksAdded = availableWalletsWithStatuses
            // User can add currencies only in multi-currency wallets
            .filterKeys(UserWallet::isMultiCurrency)
            .entries
            .groupBy(
                keySelector = { it.key },
                valueTransform = { it.value.currency.network.backendId },
            )
            .mapValues { it.value.toSet() }
            // Each wallets contains all available networks?
            .all { it.value.containsAll(networkIds) }

        return if (allNetworksAdded) AddButtonState.Unavailable else AddButtonState.Available
    }
}
