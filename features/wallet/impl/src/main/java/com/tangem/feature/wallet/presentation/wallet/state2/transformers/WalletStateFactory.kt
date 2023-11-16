package com.tangem.feature.wallet.presentation.wallet.state2.transformers

import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.domain.WalletImageResolver
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletManageButton
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletPullToRefreshConfig
import com.tangem.feature.wallet.presentation.wallet.state2.ManageTokensButtonConfig
import com.tangem.feature.wallet.presentation.wallet.state2.WalletState
import com.tangem.feature.wallet.presentation.wallet.state2.WalletTokensListState
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow

internal class WalletStateFactory(private val clickIntents: WalletClickIntentsV2) {

    fun createLoadingState(userWallet: UserWallet): WalletState {
        return createStateByWalletType(
            userWallet = userWallet,
            multiCurrencyCreator = { createLoadingMultiCurrencyContent(userWallet) },
            singleCurrencyCreator = { createLoadingSingleCurrencyContent(userWallet) },
        )
    }

    inline fun createStateByWalletType(
        userWallet: UserWallet,
        multiCurrencyCreator: () -> WalletState.MultiCurrency,
        singleCurrencyCreator: () -> WalletState.SingleCurrency,
    ): WalletState {
        return if (userWallet.isMultiCurrencyWallet()) multiCurrencyCreator() else singleCurrencyCreator()
    }

    private fun UserWallet.isMultiCurrencyWallet(): Boolean {
        return isMultiCurrency || scanResponse.cardTypesResolver.isSingleWalletWithToken()
    }

    fun createLoadingMultiCurrencyContent(userWallet: UserWallet): WalletState.MultiCurrency.Content {
        return WalletState.MultiCurrency.Content(
            pullToRefreshConfig = createPullToRefreshConfig(),
            walletCardState = userWallet.toLoadingWalletCardState(),
            warnings = persistentListOf(),
            bottomSheetConfig = null,
            tokensListState = WalletTokensListState.ContentState.Loading,
            manageTokensButtonConfig = ManageTokensButtonConfig(clickIntents::onManageTokensClick),
        )
    }

    fun createLoadingSingleCurrencyContent(userWallet: UserWallet): WalletState.SingleCurrency.Content {
        val currencySymbol = userWallet.scanResponse.cardTypesResolver.getBlockchain().currency
        return WalletState.SingleCurrency.Content(
            pullToRefreshConfig = createPullToRefreshConfig(),
            walletCardState = userWallet.toLoadingWalletCardState(),
            warnings = persistentListOf(),
            bottomSheetConfig = null,
            buttons = createDisabledButtons(),
            marketPriceBlockState = MarketPriceBlockState.Loading(currencySymbol = currencySymbol),
            txHistoryState = TxHistoryState.Content(
                contentItems = MutableStateFlow(
                    value = TxHistoryState.getDefaultLoadingTransactions(clickIntents::onExploreClick),
                ),
            ),
        )
    }

    private fun createPullToRefreshConfig(): WalletPullToRefreshConfig {
        return WalletPullToRefreshConfig(onRefresh = clickIntents::onRefreshSwipe, isRefreshing = false)
    }

    private fun UserWallet.toLoadingWalletCardState(): WalletCardState {
        return WalletCardState.Loading(
            id = walletId,
            title = name,
            additionalInfo = if (isMultiCurrency) WalletAdditionalInfoFactory.resolve(wallet = this) else null,
            imageResId = WalletImageResolver.resolve(userWallet = this),
            onRenameClick = clickIntents::onRenameClick,
            onDeleteClick = clickIntents::onDeleteBeforeConfirmationClick,
        )
    }

    private fun createDisabledButtons(): PersistentList<WalletManageButton> {
        return persistentListOf(
            WalletManageButton.Buy(enabled = false, onClick = {}),
            WalletManageButton.Send(enabled = false, onClick = {}),
            WalletManageButton.Receive(enabled = false, onClick = {}),
            WalletManageButton.Sell(enabled = false, onClick = {}),
        )
    }
}