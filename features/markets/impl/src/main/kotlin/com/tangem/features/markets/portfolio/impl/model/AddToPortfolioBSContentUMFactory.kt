package com.tangem.features.markets.portfolio.impl.model

import com.tangem.common.ui.userwallet.converter.UserWalletItemUMConverter
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.markets.portfolio.impl.loader.PortfolioData
import com.tangem.features.markets.portfolio.impl.ui.state.AddToPortfolioBSContentUM
import com.tangem.features.markets.portfolio.impl.ui.state.WalletSelectorBSContentUM
import kotlinx.collections.immutable.toImmutableList

/**
 * Factory to create AddToPortfolio bottom sheet content [TangemBottomSheetConfig]
 *
 * @property token                            token params
 * @property onAddToPortfolioVisibilityChange callback is invoked when add to portfolio visibility is changed
 * @property onWalletSelectorVisibilityChange callback is invoked when wallet selector visibility is changed
 * @property onNetworkSwitchClick             callback is invoked when network switch is clicked
 * @property onWalletSelect                   callback is invoked when wallet is selected
 * @property onContinueClick                  callback is invoked when continue button is clicked
 *
[REDACTED_AUTHOR]
 */
internal class AddToPortfolioBSContentUMFactory(
    private val token: TokenMarketParams,
    private val onAddToPortfolioVisibilityChange: (Boolean) -> Unit,
    private val onWalletSelectorVisibilityChange: (Boolean) -> Unit,
    private val onNetworkSwitchClick: (String, Boolean) -> Unit,
    private val onWalletSelect: (UserWalletId) -> Unit,
    private val onContinueClick: (selectedWalletId: UserWalletId, addedNetworks: Set<TokenMarketInfo.Network>) -> Unit,
) {

    /**
     * Create [TangemBottomSheetConfig]
     *
     * @param portfolioData        portfolio data
     * @param portfolioUIData      portfolio bottom sheet visibility model
     * @param selectedWallet       selected wallet
     * @param alreadyAddedNetworks already added networks
     */
    fun create(
        portfolioData: PortfolioData,
        portfolioUIData: PortfolioUIData,
        selectedWallet: UserWallet,
        alreadyAddedNetworks: Set<String>,
    ): TangemBottomSheetConfig {
        return TangemBottomSheetConfig(
            isShow = portfolioUIData.portfolioBSVisibilityModel.addToPortfolioBSVisibility,
            onDismissRequest = { onAddToPortfolioVisibilityChange(false) },
            content = AddToPortfolioBSContentUM(
                selectedWallet = selectedWallet.toSelectedUserWalletItemUM(),
                selectNetworkUM = SelectNetworkUMConverter(
                    networksWithToggle = portfolioUIData.addToPortfolioData.associateWithToggle(
                        userWalletId = selectedWallet.walletId,
                        alreadyAddedNetworkIds = alreadyAddedNetworks,
                    ),
                    alreadyAddedNetworks = alreadyAddedNetworks,
                    onNetworkSwitchClick = onNetworkSwitchClick,
                ).convert(value = token),
                isScanCardNotificationVisible = portfolioUIData.hasMissedDerivations,
                continueButtonEnabled = portfolioUIData.addToPortfolioData.isUserChangedNetworks(
                    userWalletId = selectedWallet.walletId,
                ),
                onContinueButtonClick = {
                    val alreadyAddedNetworkIds = portfolioData.walletsWithCurrencies[selectedWallet].orEmpty()
                        .map { it.status.currency.network.backendId }
                        .toSet()

                    onContinueClick(
                        selectedWallet.walletId,
                        portfolioUIData.addToPortfolioData.getAddedNetworks(
                            userWalletId = selectedWallet.walletId,
                            alreadyAddedNetworkIds = alreadyAddedNetworkIds,
                        ),
                    )
                },
                walletSelectorConfig = crateWalletSelectorBSConfig(
                    isShow = portfolioUIData.portfolioBSVisibilityModel.walletSelectorBSVisibility,
                    portfolioData = portfolioData,
                    selectedWalletId = selectedWallet.walletId,
                ),
                isWalletBlockVisible = portfolioData.walletsWithCurrencies
                    .filterKeys(UserWallet::isMultiCurrency).size > 1,
            ),
        )
    }

    private fun UserWallet.toSelectedUserWalletItemUM(): UserWalletItemUM {
        return UserWalletItemUMConverter(
            onClick = { onWalletSelectorVisibilityChange(true) },
            endIcon = UserWalletItemUM.EndIcon.Arrow,
        ).convert(value = this)
    }

    private fun crateWalletSelectorBSConfig(
        isShow: Boolean,
        portfolioData: PortfolioData,
        selectedWalletId: UserWalletId,
    ): TangemBottomSheetConfig {
        return TangemBottomSheetConfig(
            isShow = isShow,
            onDismissRequest = { onWalletSelectorVisibilityChange(false) },
            content = WalletSelectorBSContentUM(
                userWallets = portfolioData.walletsWithCurrencies
                    .filterKeys(UserWallet::isMultiCurrency)
                    .map { it.key }
                    .map { userWallet ->
                        val balance = portfolioData.walletsWithBalance[userWallet.walletId]

                        UserWalletItemUMConverter(
                            onClick = onWalletSelect,
                            appCurrency = portfolioData.appCurrency,
                            balance = balance?.getOrNull(),
                            isLoading = balance?.isLoading() == true,
                            isBalanceHidden = portfolioData.isBalanceHidden,
                            endIcon = if (userWallet.walletId == selectedWalletId) {
                                UserWalletItemUM.EndIcon.Checkmark
                            } else {
                                UserWalletItemUM.EndIcon.None
                            },
                        ).convert(userWallet)
                    }
                    .toImmutableList(),
                onBack = { onWalletSelectorVisibilityChange(false) },
            ),
        )
    }
}