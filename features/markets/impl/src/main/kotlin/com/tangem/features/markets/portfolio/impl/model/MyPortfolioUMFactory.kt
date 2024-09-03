package com.tangem.features.markets.portfolio.impl.model

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.markets.portfolio.impl.loader.PortfolioData
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM

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
    private val onTokenItemClick: (Int, CryptoCurrency.ID) -> Unit,
    private val addToPortfolioBSContentUMFactory: AddToPortfolioBSContentUMFactory,
    private val tokenActionsHandler: TokenActionsHandler,
) {

    fun create(
        portfolioData: PortfolioData,
        portfolioUIData: PortfolioUIData,
        availableNetworks: List<TokenMarketInfo.Network>?,
    ): MyPortfolioUM {
        if (availableNetworks == null) return MyPortfolioUM.Loading

        if (availableNetworks.isEmpty()) return MyPortfolioUM.Unavailable

        val walletsWithCurrencies = portfolioData.walletsWithCurrencies
            .filterAvailableNetworks(networks = availableNetworks)

        val isPortfolioEmpty = walletsWithCurrencies.flatMap { it.value }.isEmpty()
        if (isPortfolioEmpty) {
            val hasMultiWallets = walletsWithCurrencies.filterKeys(UserWallet::isMultiCurrency).isNotEmpty()

            return if (hasMultiWallets) {
                MyPortfolioUM.AddFirstToken(
                    addToPortfolioBSConfig = createAddToPortfolioBSConfig(
                        portfolioData = portfolioData,
                        portfolioUIData = portfolioUIData,
                        availableNetworks = availableNetworks,
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
            isAllAvailableNetworksAdded = walletsWithCurrencies.isAllAvailableNetworksAdded(availableNetworks),
            bsConfig = createAddToPortfolioBSConfig(
                portfolioData = portfolioData,
                portfolioUIData = portfolioUIData,
                availableNetworks = availableNetworks,
            ),
            onAddClick = onAddClick,
            onTokenItemClick = onTokenItemClick,
            quickActionsIntents = tokenActionsHandler,
        )
            .convert(walletsWithCurrencies)
    }

    private fun createAddToPortfolioBSConfig(
        portfolioData: PortfolioData,
        portfolioUIData: PortfolioUIData,
        availableNetworks: List<TokenMarketInfo.Network>,
    ): TangemBottomSheetConfig {
        val walletId = portfolioUIData.selectedWalletId
            ?: portfolioData.walletsWithCurrencies.keys.firstOrNull { it.isMultiCurrency }?.walletId

        val selectedWallet = portfolioData.walletsWithCurrencies.keys
            .firstOrNull { it.walletId == walletId }
            ?: error("portfolioModel.walletsWithCurrencyStatuses doesn't contain selected wallet: $walletId")

        val changedNetworks = portfolioUIData.walletsWithChangedNetworks[portfolioUIData.selectedWalletId]
        val alreadyAddedNetworks = requireNotNull(
            value = portfolioData.walletsWithCurrencies[selectedWallet],
            lazyMessage = {
                "portfolioModel.walletsWithCurrencyStatuses doesn't contain selected wallet: $walletId"
            },
        )
            .filterAvailableNetworks(availableNetworks)
            .map { it.status.currency.network.backendId }

        return addToPortfolioBSContentUMFactory.create(
            portfolioData = portfolioData,
            portfolioUIData = portfolioUIData,
            selectedWallet = selectedWallet,
            networksWithToggle = availableNetworks.associateWithToggle(
                changedNetworks = changedNetworks,
                alreadyAddedNetworks = alreadyAddedNetworks,
            ),
            isUserChangedNetworks = changedNetworks != null && alreadyAddedNetworks != changedNetworks,
        )
    }

    private fun List<TokenMarketInfo.Network>.associateWithToggle(
        changedNetworks: List<String>?,
        alreadyAddedNetworks: List<String>,
    ): Map<TokenMarketInfo.Network, Boolean> {
        // Use user choice or check already added networks
        return associateWith { network ->
            val isSelectedByUser = changedNetworks?.contains(network.networkId)

            if (isSelectedByUser != null) return@associateWith isSelectedByUser

            val isAlreadyAdded = alreadyAddedNetworks.any { it == network.networkId }

            isAlreadyAdded
        }
    }

    private fun Map<UserWallet, List<PortfolioData.CryptoCurrencyData>>.isAllAvailableNetworksAdded(
        availableNetworks: List<TokenMarketInfo.Network>,
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
        networks: List<TokenMarketInfo.Network>,
    ): Map<UserWallet, List<PortfolioData.CryptoCurrencyData>> {
        return mapValues { entry -> entry.value.filterAvailableNetworks(networks) }
    }

    /** Filter list of [CryptoCurrencyStatus] by available networks [networks] */
    private fun List<PortfolioData.CryptoCurrencyData>.filterAvailableNetworks(
        networks: List<TokenMarketInfo.Network>,
    ): List<PortfolioData.CryptoCurrencyData> {
        val networkIds = networks.map(TokenMarketInfo.Network::networkId)

        return mapNotNull {
            it.takeIf { networkIds.contains(it.status.currency.network.backendId) }
        }
    }
}