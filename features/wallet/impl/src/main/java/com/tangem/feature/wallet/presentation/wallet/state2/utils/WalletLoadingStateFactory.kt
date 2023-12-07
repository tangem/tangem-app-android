package com.tangem.feature.wallet.presentation.wallet.state2.utils

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
import com.tangem.feature.wallet.presentation.wallet.state2.WalletState.Visa.BalancesAndLimitsBlockState
import com.tangem.feature.wallet.presentation.wallet.state2.WalletState.Visa.DepositButtonState
import com.tangem.feature.wallet.presentation.wallet.state2.WalletTokensListState
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Factory for creating loading state [WalletState]
 *
 * @property clickIntents click intents
 */
internal class WalletLoadingStateFactory(private val clickIntents: WalletClickIntentsV2) {

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
            buttons = createDisabledButtons(),
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