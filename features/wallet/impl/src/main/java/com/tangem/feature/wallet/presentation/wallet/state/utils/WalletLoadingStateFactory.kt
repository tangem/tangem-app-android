package com.tangem.feature.wallet.presentation.wallet.state.utils

import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.pullToRefresh.PullToRefreshConfig
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.domain.WalletImageResolver
import com.tangem.feature.wallet.presentation.wallet.state.model.*
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import com.tangem.features.wallet.featuretoggles.WalletFeatureToggles
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Factory for creating loading state [WalletState]
 *
 * @property clickIntents click intents
 */
internal class WalletLoadingStateFactory(
    private val clickIntents: WalletClickIntents,
    private val walletImageResolver: WalletImageResolver,
    private val walletFeatureToggles: WalletFeatureToggles,
) {

    fun create(userWallet: UserWallet): WalletState {
        return userWallet.createStateByWalletType(
            multiCurrencyCreator = { createLoadingMultiCurrencyContent(userWallet) },
            singleCurrencyCreator = { createLoadingSingleCurrencyContent(userWallet) },
            visaWalletCreator = { createLoadingVisaWalletContent(userWallet) },
        )
    }

    private fun createLoadingMultiCurrencyContent(userWallet: UserWallet): WalletState.MultiCurrency.Content {
        return WalletState.MultiCurrency.Content(
            pullToRefreshConfig = createPullToRefreshConfig(),
            walletCardState = userWallet.toLoadingWalletCardState(),
            buttons = createMultiWalletActions(userWallet),
            warnings = persistentListOf(),
            bottomSheetConfig = null,
            tokensListState = WalletTokensListState.ContentState.Loading,
        )
    }

    private fun createLoadingSingleCurrencyContent(userWallet: UserWallet): WalletState.SingleCurrency.Content {
        val currencySymbol = userWallet.scanResponse.cardTypesResolver.getBlockchain().currency
        return WalletState.SingleCurrency.Content(
            pullToRefreshConfig = createPullToRefreshConfig(),
            walletCardState = userWallet.toLoadingWalletCardState(),
            warnings = persistentListOf(),
            bottomSheetConfig = null,
            buttons = createDimmedButtons(),
            marketPriceBlockState = MarketPriceBlockState.Loading(currencySymbol = currencySymbol),
            txHistoryState = TxHistoryState.Content(
                contentItems = MutableStateFlow(
                    value = TxHistoryState.getDefaultLoadingTransactions(clickIntents::onExploreClick),
                ),
            ),
        )
    }

    private fun createLoadingVisaWalletContent(userWallet: UserWallet): WalletState.Visa.Content {
        return WalletState.Visa.Content(
            pullToRefreshConfig = createPullToRefreshConfig(),
            walletCardState = userWallet.toLoadingWalletCardState(),
            buttons = createMultiWalletActions(userWallet),
            warnings = persistentListOf(),
            bottomSheetConfig = null,
            balancesAndLimitBlockState = BalancesAndLimitsBlockState.Loading,
            txHistoryState = TxHistoryState.Content(
                contentItems = MutableStateFlow(
                    value = TxHistoryState.getDefaultLoadingTransactions(clickIntents::onExploreClick),
                ),
            ),
            depositButtonState = DepositButtonState(isEnabled = false, clickIntents::onDepositClick),
        )
    }

    private fun createPullToRefreshConfig(): PullToRefreshConfig {
        return PullToRefreshConfig(onRefresh = { clickIntents.onRefreshSwipe(it.value) }, isRefreshing = false)
    }

    private fun UserWallet.toLoadingWalletCardState(): WalletCardState {
        return WalletCardState.Loading(
            id = walletId,
            title = name,
            additionalInfo = if (isMultiCurrency) WalletAdditionalInfoFactory.resolve(wallet = this) else null,
            imageResId = walletImageResolver.resolve(userWallet = this),
            onRenameClick = clickIntents::onRenameBeforeConfirmationClick,
            onDeleteClick = clickIntents::onDeleteBeforeConfirmationClick,
        )
    }

    private fun createMultiWalletActions(userWallet: UserWallet): PersistentList<WalletManageButton> {
        val isSingleWalletWithToken = userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()
        if (!walletFeatureToggles.isMainActionButtonsEnabled || isSingleWalletWithToken) return persistentListOf()

        return persistentListOf(
            WalletManageButton.Buy(
                enabled = true,
                dimContent = false,
                onClick = { clickIntents.onMultiWalletBuyClick(userWalletId = userWallet.walletId) },
            ),
            WalletManageButton.Swap(
                enabled = true,
                dimContent = false,
                onClick = { clickIntents.onMultiWalletSwapClick(userWalletId = userWallet.walletId) },
            ),
            WalletManageButton.Sell(
                enabled = true,
                dimContent = false,
                onClick = { clickIntents.onMultiWalletSellClick(userWalletId = userWallet.walletId) },
            ),
        )
    }

    private fun createDimmedButtons(): PersistentList<WalletManageButton> {
        return persistentListOf(
            WalletManageButton.Receive(enabled = true, dimContent = true, onClick = {}, onLongClick = null),
            WalletManageButton.Send(enabled = true, dimContent = true, onClick = {}),
            WalletManageButton.Buy(enabled = true, dimContent = true, onClick = {}),
            WalletManageButton.Sell(enabled = true, dimContent = true, onClick = {}),
        )
    }
}
