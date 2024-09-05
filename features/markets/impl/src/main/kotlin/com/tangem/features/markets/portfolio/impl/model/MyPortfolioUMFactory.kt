package com.tangem.features.markets.portfolio.impl.model

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.markets.portfolio.impl.loader.PortfolioData
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM
import com.tangem.features.markets.portfolio.impl.ui.state.PortfolioTokenUM
import com.tangem.utils.Provider
import kotlinx.collections.immutable.ImmutableList

/**
 * Factory for creating [MyPortfolioUM]
 *
 * @property onAddClick       callback when user wants to add new token
 * @property onTokenItemClick callback when user wants to see actions with token
 *
[REDACTED_AUTHOR]
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

        if (addToPortfolioData.availableNetworks == null) return MyPortfolioUM.Loading

        if (addToPortfolioData.availableNetworks.isEmpty()) return MyPortfolioUM.Unavailable

        val walletsWithCurrencies = portfolioData.walletsWithCurrencies
            .filterAvailableNetworks(networks = addToPortfolioData.availableNetworks)

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
            isAllAvailableNetworksAdded = walletsWithCurrencies.isAllAvailableNetworksAdded(
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
        val walletId = portfolioUIData.selectedWalletId
            ?: portfolioData.walletsWithCurrencies.keys.firstOrNull { it.isMultiCurrency }?.walletId

        val selectedWallet = portfolioData.walletsWithCurrencies.keys
            .firstOrNull { it.walletId == walletId }
            ?: error("portfolioModel.walletsWithCurrencyStatuses doesn't contain selected wallet: $walletId")

        val availableNetworks = portfolioUIData.addToPortfolioData.availableNetworks.orEmpty()

        val alreadyAddedNetworks = requireNotNull(
            value = portfolioData.walletsWithCurrencies
                .filterAvailableNetworks(availableNetworks)[selectedWallet],
            lazyMessage = {
                "portfolioModel.walletsWithCurrencyStatuses doesn't contain selected wallet: $walletId"
            },
        )
            .map { it.status.currency.network.backendId }
            .toSet()

        return addToPortfolioBSContentUMFactory.create(
            portfolioData = portfolioData,
            portfolioUIData = portfolioUIData,
            selectedWallet = selectedWallet,
            networksWithToggle = portfolioUIData.addToPortfolioData.associateWithToggle(
                userWalletId = selectedWallet.walletId,
                alreadyAddedNetworkIds = alreadyAddedNetworks,
            ),
            isUserChangedNetworks = portfolioUIData.addToPortfolioData.isUserChangedNetworks(selectedWallet.walletId),
        )
    }

    private fun Map<UserWallet, List<PortfolioData.CryptoCurrencyData>>.isAllAvailableNetworksAdded(
        availableNetworks: Set<TokenMarketInfo.Network>,
    ): Boolean {
        val networkIds = availableNetworks.map { it.networkId }

        return this
            // User can add currencies only in multi-currency wallets
            .filterKeys(UserWallet::isMultiCurrency)
            .mapValues { entry -> entry.value.map { it.status.currency.network.backendId } }
            // Each wallets contains all available networks?
            .all { it.value.containsAll(networkIds) }
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