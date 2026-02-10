package com.tangem.feature.wallet.presentation.wallet.state.utils

import com.tangem.core.analytics.models.event.MainScreenAnalyticsEvent.Companion.WALLET_TYPE
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.domain.WalletImageResolver
import com.tangem.feature.wallet.presentation.wallet.state.model.*
import kotlinx.collections.immutable.ImmutableList
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
) {

    fun create(userWallet: UserWallet): WalletState {
        return when (userWallet) {
            is UserWallet.Cold -> {
                userWallet.createStateByWalletType(
                    multiCurrencyCreator = { createLoadingMultiCurrencyContent(userWallet) },
                    singleCurrencyCreator = { createLoadingSingleCurrencyContent(userWallet) },
                )
            }
            is UserWallet.Hot -> {
                createLoadingHotWalletContent(userWallet)
            }
        }
    }

    private fun createLoadingHotWalletContent(userWallet: UserWallet.Hot): WalletState.MultiCurrency.Content {
        return WalletState.MultiCurrency.Content(
            pullToRefreshConfig = createPullToRefreshConfig(),
            walletCardState = createLoadingWalletCardState(userWallet),
            buttons = createMultiWalletActions(userWallet),
            warnings = persistentListOf(),
            bottomSheetConfig = null,
            tokensListState = WalletTokensListState.ContentState.Loading,
            nftState = WalletNFTItemUM.Hidden,
            type = WalletState.MultiCurrency.WalletType.Hot,
            tangemPayState = TangemPayState.Empty,
        )
    }

    private fun createLoadingMultiCurrencyContent(userWallet: UserWallet.Cold): WalletState.MultiCurrency.Content {
        return WalletState.MultiCurrency.Content(
            pullToRefreshConfig = createPullToRefreshConfig(),
            walletCardState = createLoadingWalletCardState(userWallet),
            buttons = createMultiWalletActions(userWallet),
            warnings = persistentListOf(),
            bottomSheetConfig = null,
            tokensListState = WalletTokensListState.ContentState.Loading,
            nftState = WalletNFTItemUM.Hidden,
            type = WalletState.MultiCurrency.WalletType.Cold,
            tangemPayState = TangemPayState.Empty,
        )
    }

    private fun createLoadingSingleCurrencyContent(userWallet: UserWallet.Cold): WalletState.SingleCurrency.Content {
        val currencySymbol = userWallet.scanResponse.cardTypesResolver.getBlockchain().currency
        return WalletState.SingleCurrency.Content(
            pullToRefreshConfig = createPullToRefreshConfig(),
            walletCardState = createLoadingWalletCardState(userWallet),
            warnings = persistentListOf(),
            bottomSheetConfig = null,
            buttons = createDimmedButtons(),
            marketPriceBlockState = MarketPriceBlockState.Loading(currencySymbol = currencySymbol),
            txHistoryState = TxHistoryState.Content(
                contentItems = MutableStateFlow(
                    value = TxHistoryState.getDefaultLoadingTransactions(clickIntents::onExploreClick),
                ),
            ),
            expressTxsToDisplay = persistentListOf(),
            expressTxs = persistentListOf(),
        )
    }

    private fun createPullToRefreshConfig(): PullToRefreshConfig {
        return PullToRefreshConfig(
            onRefresh = { clickIntents.onRefreshSwipe(it.value) },
            isRefreshing = false,
        )
    }

    private fun createLoadingWalletCardState(userWallet: UserWallet): WalletCardState {
        return WalletCardState.Loading(
            id = userWallet.walletId,
            title = userWallet.name,
            additionalInfo = if (!userWallet.isMultiCurrency) {
                null
            } else {
                WalletAdditionalInfoFactory.resolve(wallet = userWallet)
            },
            imageResId = if (userWallet is UserWallet.Cold) {
                walletImageResolver.resolve(userWallet)
            } else {
                null
            },
            dropDownItems = createDropDownItems(userWalletId = userWallet.walletId),
        )
    }

    private fun createMultiWalletActions(userWallet: UserWallet): PersistentList<WalletManageButton> {
        val isSingleWalletWithToken =
            userWallet is UserWallet.Cold && userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()
        if (isSingleWalletWithToken) return persistentListOf()

        return persistentListOf(
            WalletManageButton.Buy(
                enabled = true,
                dimContent = false,
                onClick = {
                    clickIntents.onMultiWalletBuyClick(
                        userWalletId = userWallet.walletId,
                        WALLET_TYPE,
                    )
                },
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
            WalletManageButton.Receive(
                enabled = true,
                dimContent = true,
                onClick = {},
                onLongClick = null,
            ),
            WalletManageButton.Send(enabled = true, dimContent = true, onClick = {}),
            WalletManageButton.Buy(enabled = true, dimContent = true, onClick = {}),
            WalletManageButton.Sell(enabled = true, dimContent = true, onClick = {}),
        )
    }

    private fun createDropDownItems(userWalletId: UserWalletId): ImmutableList<WalletDropDownItems> {
        return persistentListOf(
            WalletDropDownItems(
                text = resourceReference(id = R.string.common_rename),
                icon = R.drawable.ic_edit_24,
                onClick = { clickIntents.onRenameBeforeConfirmationClick(userWalletId) },
            ),
        )
    }
}