package com.tangem.features.markets.portfolio.impl.model

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.markets.portfolio.impl.loader.PortfolioData
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
    private val onTokenItemClick: (Int, CryptoCurrency.ID) -> Unit,
    private val addToPortfolioBSContentUMFactory: AddToPortfolioBSContentUMFactory,
) {

    fun create(
        portfolioModel: PortfolioData,
        portfolioBSVisibilityModel: PortfolioBSVisibilityModel,
        availableNetworks: List<TokenMarketInfo.Network>?,
        selectedWalletId: UserWalletId?,
        walletsWithChangedNetworks: Map<UserWalletId, List<String>>,
    ): MyPortfolioUM {
        if (availableNetworks == null) return MyPortfolioUM.Loading

        if (availableNetworks.isEmpty()) return MyPortfolioUM.Unavailable

        val walletsWithStatuses = portfolioModel.walletsWithCurrencyStatuses
            .filterAvailableNetworks(networks = availableNetworks)

        val isPortfolioEmpty = walletsWithStatuses.flatMap { it.value }.isEmpty()
        if (isPortfolioEmpty) {
            val hasMultiWallets = walletsWithStatuses.filterKeys(UserWallet::isMultiCurrency).isNotEmpty()

            return if (hasMultiWallets) {
                MyPortfolioUM.AddFirstToken(
                    bsConfig = createAddToPortfolioBSConfig(
                        portfolioModel = portfolioModel,
                        selectedWalletId = selectedWalletId,
                        availableNetworks = availableNetworks,
                        walletsWithChangedNetworks = walletsWithChangedNetworks,
                        portfolioBSVisibilityModel = portfolioBSVisibilityModel,
                    ),
                    onAddClick = onAddClick,
                )
            } else {
                MyPortfolioUM.Unavailable
            }
        }

        return TokensPortfolioUMConverter(
            appCurrency = portfolioModel.appCurrency,
            isBalanceHidden = portfolioModel.isBalanceHidden,
            isAllAvailableNetworksAdded = walletsWithStatuses.isAllAvailableNetworksAdded(availableNetworks),
            bsConfig = createAddToPortfolioBSConfig(
                portfolioModel = portfolioModel,
                selectedWalletId = selectedWalletId,
                availableNetworks = availableNetworks,
                portfolioBSVisibilityModel = portfolioBSVisibilityModel,
                walletsWithChangedNetworks = walletsWithChangedNetworks,
            ),
            onAddClick = onAddClick,
            onTokenItemClick = onTokenItemClick,
        )
            .convert(walletsWithStatuses)
    }

    private fun createAddToPortfolioBSConfig(
        portfolioModel: PortfolioData,
        selectedWalletId: UserWalletId?,
        availableNetworks: List<TokenMarketInfo.Network>,
        portfolioBSVisibilityModel: PortfolioBSVisibilityModel,
        walletsWithChangedNetworks: Map<UserWalletId, List<String>>,
    ): TangemBottomSheetConfig {
        val walletId = requireNotNull(selectedWalletId) {
            "Selected wallet must be not null in state with AddToPortfolio bottom sheet"
        }

        val selectedWallet = portfolioModel.walletsWithCurrencyStatuses.keys
            .firstOrNull { it.walletId == walletId }
            ?: error("portfolioModel.walletsWithCurrencyStatuses doesn't contain selected wallet: $walletId")

        val changedNetworks = walletsWithChangedNetworks[selectedWalletId]
        val alreadyAddedNetworks = requireNotNull(
            value = portfolioModel.walletsWithCurrencyStatuses[selectedWallet],
            lazyMessage = {
                "portfolioModel.walletsWithCurrencyStatuses doesn't contain selected wallet: $walletId"
            },
        )
            .filterAvailableNetworks(availableNetworks)
            .map { it.currency.network.backendId }

        return addToPortfolioBSContentUMFactory.create(
            portfolioData = portfolioModel,
            portfolioBSVisibilityModel = portfolioBSVisibilityModel,
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

    private fun Map<UserWallet, List<CryptoCurrencyStatus>>.isAllAvailableNetworksAdded(
        availableNetworks: List<TokenMarketInfo.Network>,
    ): Boolean {
        val networkIds = availableNetworks.map { it.networkId }

        return this
            // User can add currencies only in multi-currency wallets
            .filterKeys(UserWallet::isMultiCurrency)
            .mapValues { it.value.map { it.currency.network.backendId } }
            // Each wallets contains all available networks?
            .all { it.value.containsAll(networkIds) }
    }

    /** Filter map values by available networks [networks] */
    private fun Map<UserWallet, List<CryptoCurrencyStatus>>.filterAvailableNetworks(
        networks: List<TokenMarketInfo.Network>,
    ): Map<UserWallet, List<CryptoCurrencyStatus>> {
        return mapValues { entry -> entry.value.filterAvailableNetworks(networks) }
    }

    /** Filter list of [CryptoCurrencyStatus] by available networks [networks] */
    private fun List<CryptoCurrencyStatus>.filterAvailableNetworks(
        networks: List<TokenMarketInfo.Network>,
    ): List<CryptoCurrencyStatus> {
        val networkIds = networks.map(TokenMarketInfo.Network::networkId)

        return mapNotNull {
            it.takeIf { networkIds.contains(it.currency.network.backendId) }
        }
    }
}
