package com.tangem.feature.wallet.presentation.wallet.state.utils

import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.domain.WalletImageResolver
import com.tangem.feature.wallet.presentation.wallet.state.model.*
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Factory for creating loading state [WalletState]
 *
 * @property clickIntents click intents
 */
internal class WalletLoadingStateFactory(private val clickIntents: WalletClickIntents) {

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
            warnings = persistentListOf(),
            bottomSheetConfig = null,
            tokensListState = WalletTokensListState.ContentState.Loading,
            manageTokensButtonConfig = ManageTokensButtonConfig(clickIntents::onManageTokensClick),
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

    private fun createPullToRefreshConfig(): WalletPullToRefreshConfig {
        return WalletPullToRefreshConfig(onRefresh = clickIntents::onRefreshSwipe, isRefreshing = false)
    }

    private fun UserWallet.toLoadingWalletCardState(): WalletCardState {
        return WalletCardState.Loading(
            id = walletId,
            title = name,
            additionalInfo = if (isMultiCurrency) WalletAdditionalInfoFactory.resolve(wallet = this) else null,
            imageResId = WalletImageResolver.resolve(userWallet = this),
            onRenameClick = clickIntents::onRenameBeforeConfirmationClick,
            onDeleteClick = clickIntents::onDeleteBeforeConfirmationClick,
        )
    }

    private fun createDimmedButtons(): PersistentList<WalletManageButton> {
        return persistentListOf(
            WalletManageButton.Receive(enabled = true, dimContent = true, onClick = {}),
            WalletManageButton.Send(enabled = true, dimContent = true, onClick = {}),
            WalletManageButton.Buy(enabled = true, dimContent = true, onClick = {}),
            WalletManageButton.Sell(enabled = true, dimContent = true, onClick = {}),
        )
    }
}
