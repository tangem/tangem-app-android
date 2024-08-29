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
    private val onContinueClick: () -> Unit,
) {

    /**
     * Create [TangemBottomSheetConfig]
     *
     * @param portfolioData              portfolio data
     * @param portfolioBSVisibilityModel portfolio bottom sheet visibility model
     * @param selectedWallet             selected wallet
     * @param networksWithToggle         networks with toggle
     * @param isUserChangedNetworks      flag indicates if user changed networks
     */
    fun create(
        portfolioData: PortfolioData,
        portfolioBSVisibilityModel: PortfolioBSVisibilityModel,
        selectedWallet: UserWallet,
        networksWithToggle: Map<TokenMarketInfo.Network, Boolean>,
        isUserChangedNetworks: Boolean,
    ): TangemBottomSheetConfig {
        return TangemBottomSheetConfig(
            isShow = portfolioBSVisibilityModel.addToPortfolioBSVisibility,
            onDismissRequest = { onAddToPortfolioVisibilityChange(false) },
            content = AddToPortfolioBSContentUM(
                selectedWallet = selectedWallet.toSelectedUserWalletItemUM(),
                selectNetworkUM = SelectNetworkUMConverter(
                    networksWithToggle = networksWithToggle,
                    onNetworkSwitchClick = onNetworkSwitchClick,
                ).convert(value = token),
                isScanCardNotificationVisible = false, // TODO [REDACTED_JIRA]
                continueButtonEnabled = isUserChangedNetworks,
                onContinueButtonClick = onContinueClick,
                walletSelectorConfig = crateWalletSelectorBSConfig(
                    isShow = portfolioBSVisibilityModel.walletSelectorBSVisibility,
                    portfolioData = portfolioData,
                    selectedWalletId = selectedWallet.walletId,
                ),
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
                userWallets = portfolioData.walletsWithCurrencyStatuses
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