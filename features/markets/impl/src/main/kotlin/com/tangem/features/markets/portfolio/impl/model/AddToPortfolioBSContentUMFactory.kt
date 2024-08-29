package com.tangem.features.markets.portfolio.impl.model

import com.tangem.common.ui.userwallet.converter.UserWalletItemUMConverter
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.markets.portfolio.impl.domain.PortfolioData
import com.tangem.features.markets.portfolio.impl.ui.state.AddToPortfolioBSContentUM
import com.tangem.features.markets.portfolio.impl.ui.state.WalletSelectorBSContentUM
import kotlinx.collections.immutable.toImmutableList

/**
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

    fun create(
        portfolioModel: PortfolioData,
        portfolioBSVisibilityModel: PortfolioBSVisibilityModel,
        selectedWallet: UserWallet,
        networksWithToggle: Map<TokenMarketInfo.Network, Boolean>,
        isUserChangedNetworks: Boolean,
    ): TangemBottomSheetConfig {
        val multiWalletsWithStatuses = portfolioModel.walletsWithCurrencyStatuses
            .filterKeys(UserWallet::isMultiCurrency)

        return TangemBottomSheetConfig(
            isShow = portfolioBSVisibilityModel.addToPortfolioBSVisibility,
            onDismissRequest = { onAddToPortfolioVisibilityChange(false) },
            content = AddToPortfolioBSContentUM(
                selectedWallet = selectedWallet.toSelectedUserWalletItemUM(),
                selectNetworkUM = SelectNetworkUMConverter(
                    networksWithToggle = networksWithToggle,
                    onNetworkSwitchClick = onNetworkSwitchClick,
                ).convert(value = token),
                isScanCardNotificationVisible = false, // TODO add use case
                continueButtonEnabled = isUserChangedNetworks,
                onContinueButtonClick = onContinueClick,
                walletSelectorConfig = TangemBottomSheetConfig(
                    isShow = portfolioBSVisibilityModel.walletSelectorBSVisibility,
                    onDismissRequest = { onWalletSelectorVisibilityChange(false) },
                    content = WalletSelectorBSContentUM(
                        userWallets = multiWalletsWithStatuses
                            .map {
                                val balance = portfolioModel.walletsWithBalance[it.key.walletId]

                                UserWalletItemUMConverter(
                                    onClick = onWalletSelect,
                                    appCurrency = portfolioModel.appCurrency,
                                    balance = balance?.getOrNull(),
                                    isLoading = balance?.isLoading() == true,
                                    isBalanceHidden = portfolioModel.isBalanceHidden,
                                    endIcon = if (it.key.walletId == selectedWallet.walletId) {
                                        UserWalletItemUM.EndIcon.Checkmark
                                    } else {
                                        UserWalletItemUM.EndIcon.None
                                    },
                                ).convert(it.key)
                            }
                            .toImmutableList(),
                        onBack = { onWalletSelectorVisibilityChange(false) },
                    ),
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
}