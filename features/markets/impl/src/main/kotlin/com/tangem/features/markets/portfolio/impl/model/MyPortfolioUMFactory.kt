package com.tangem.features.markets.portfolio.impl.model

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.markets.portfolio.impl.loader.PortfolioData
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM.Tokens.AddButtonState
import com.tangem.features.markets.portfolio.impl.ui.state.PortfolioTokenUM
import com.tangem.utils.Provider
import kotlinx.collections.immutable.ImmutableList

/**
 * Factory for creating [MyPortfolioUM]
 *
 * @property onAddClick       callback when user wants to add new token
 * @property onTokenItemClick callback when user wants to see actions with token
 *
* [REDACTED_AUTHOR]
 */
internal class MyPortfolioUMFactory(
    private val onAddClick: () -> Unit,
    private val addToPortfolioBSContentUMFactory: AddToPortfolioBSContentUMFactory,
    private val tokenActionsHandler: TokenActionsHandler,
    private val currentState: Provider<MyPortfolioUM>,
    private val updateTokens: ((ImmutableList<PortfolioTokenUM>) -> ImmutableList<PortfolioTokenUM>) -> Unit,
) {

    fun create(portfolioData: PortfolioData, portfolioUIData: PortfolioUIData): MyPortfolioUM {
        val addToPortfolioData = portfolioUIData.addToPortfolioData

        val hasAvailableNetworks = addToPortfolioData.availableNetworks?.isEmpty() == true
        val isOnlySingleWalletsAdded = portfolioData.walletsWithCurrencies.keys.all { !it.isMultiCurrency }
        if (hasAvailableNetworks || isOnlySingleWalletsAdded) return MyPortfolioUM.Unavailable

        val walletsWithCurrencies = if (addToPortfolioData.availableNetworks == null) {
            portfolioData.walletsWithCurrencies
        } else {
            portfolioData.walletsWithCurrencies.filterAvailableNetworks(networks = addToPortfolioData.availableNetworks)
        }

        val isPortfolioEmpty = walletsWithCurrencies.flatMap { it.value }.isEmpty()
        if (isPortfolioEmpty) {
            val hasMultiWallets = walletsWithCurrencies.filterKeys(UserWallet::isMultiCurrency).isNotEmpty()

            return if (hasMultiWallets) {
                MyPortfolioUM.AddFirstToken(
                    addToPortfolioBSConfig = createAddToPortfolioBSConfig(
                        portfolioData = portfolioData,
                        portfolioUIData = portfolioUIData,
                    ),
                    onAddClick = onAddClick,
                )
            } else {
                MyPortfolioUM.Unavailable
            }
        }

        return TokensPortfolioUMConverter(
            appCurrency = portfolioData.appCurrency,
            isBalanceHidden = portfolioData.isBalanceHidden,
            addButtonState = walletsWithCurrencies.getAddButtonState(
                availableNetworks = addToPortfolioData.availableNetworks,
            ),
            bsConfig = createAddToPortfolioBSConfig(portfolioData = portfolioData, portfolioUIData = portfolioUIData),
            onAddClick = onAddClick,
            quickActionsIntents = tokenActionsHandler,
            currentState = currentState,
            updateTokens = updateTokens,
        )
            .convert(walletsWithCurrencies)
    }

    private fun createAddToPortfolioBSConfig(
        portfolioData: PortfolioData,
        portfolioUIData: PortfolioUIData,
    ): TangemBottomSheetConfig {
        val selectedWallet = portfolioData.walletsWithCurrencies.keys
            .firstOrNull { it.walletId == portfolioUIData.selectedWalletId }
            ?: portfolioData.walletsWithCurrencies.keys.firstOrNull { it.isMultiCurrency }
            ?: error("walletsWithCurrencies don't contain selected wallet or any multi-currency wallet")

        val availableNetworks = portfolioUIData.addToPortfolioData.availableNetworks.orEmpty()

        val alreadyAddedNetworks = requireNotNull(
            value = portfolioData.walletsWithCurrencies.filterAvailableNetworks(availableNetworks)[selectedWallet],
            lazyMessage = { "walletsWithCurrencies don't contain ${selectedWallet.walletId}" },
        )
            .map { it.status.currency.network.backendId }
            .toSet()

        return addToPortfolioBSContentUMFactory.create(
            portfolioData = portfolioData,
            portfolioUIData = portfolioUIData,
            selectedWallet = selectedWallet,
            alreadyAddedNetworks = alreadyAddedNetworks,
        )
    }

    private fun Map<UserWallet, List<PortfolioData.CryptoCurrencyData>>.getAddButtonState(
        availableNetworks: Set<TokenMarketInfo.Network>?,
    ): AddButtonState {
        if (availableNetworks == null) return AddButtonState.Loading

        val networkIds = availableNetworks.map { it.networkId }

        val isAllAvailableNetworksAdded = this
            // User can add currencies only in multi-currency wallets
            .filterKeys(UserWallet::isMultiCurrency)
            .mapValues { entry -> entry.value.map { it.status.currency.network.backendId } }
            // Each wallets contains all available networks?
            .all { it.value.containsAll(networkIds) }

        return if (isAllAvailableNetworksAdded) AddButtonState.Unavailable else AddButtonState.Available
    }

    /** Filter map values by available networks [networks] */
    private fun Map<UserWallet, List<PortfolioData.CryptoCurrencyData>>.filterAvailableNetworks(
        networks: Set<TokenMarketInfo.Network>,
    ): Map<UserWallet, List<PortfolioData.CryptoCurrencyData>> {
        return mapValues { entry -> entry.value.filterAvailableNetworks(networks) }
    }

    /** Filter list of [CryptoCurrencyStatus] by available networks [networks] */
    private fun List<PortfolioData.CryptoCurrencyData>.filterAvailableNetworks(
        networks: Set<TokenMarketInfo.Network>,
    ): List<PortfolioData.CryptoCurrencyData> {
        val networkIds = networks.map(TokenMarketInfo.Network::networkId)

        return mapNotNull {
            it.takeIf { networkIds.contains(it.status.currency.network.backendId) }
        }
    }
}
