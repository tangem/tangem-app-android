package com.tangem.feature.wallet.presentation.wallet.state.factory

import androidx.annotation.DrawableRes
import com.tangem.common.Provider
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.domain.WalletImageResolver
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.*
import com.tangem.feature.wallet.presentation.wallet.state.factory.WalletSkeletonStateConverter.SkeletonModel
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Converter from loaded list of [UserWallet] to skeleton state of screen [WalletState.ContentState]
 *
 * @property currentStateProvider current ui state provider
 * @property clickIntents         screen click intents
 *
[REDACTED_AUTHOR]
 */
internal class WalletSkeletonStateConverter(
    private val currentStateProvider: Provider<WalletState>,
    private val clickIntents: WalletClickIntents,
) : Converter<SkeletonModel, WalletState.ContentState> {

    override fun convert(value: SkeletonModel): WalletState.ContentState {
        val selectedWallet = value.wallets[value.selectedWalletIndex]

        return if (selectedWallet.isMultiCurrency) {
            createMultiCurrencyState(value = value)
        } else {
            createSingleCurrencyState(value = value, currencyName = selectedWallet.getPrimaryCurrencyName())
        }
    }

    private fun createMultiCurrencyState(value: SkeletonModel): WalletMultiCurrencyState {
        return WalletMultiCurrencyState.Content(
            onBackClick = clickIntents::onBackClick,
            topBarConfig = createTopBarConfig(),
            walletsListConfig = createWalletsListConfig(value),
            pullToRefreshConfig = createPullToRefreshConfig(),
            tokensListState = WalletTokensListState.Loading(),
            notifications = persistentListOf(),
            bottomSheetConfig = null,
            tokenActionsBottomSheet = null,
            onManageTokensClick = clickIntents::onManageTokensClick,
        )
    }

    private fun createSingleCurrencyState(value: SkeletonModel, currencyName: String): WalletSingleCurrencyState {
        return WalletSingleCurrencyState.Content(
            onBackClick = clickIntents::onBackClick,
            topBarConfig = createTopBarConfig(),
            walletsListConfig = createWalletsListConfig(value),
            pullToRefreshConfig = createPullToRefreshConfig(),
            notifications = persistentListOf(),
            bottomSheetConfig = null,
            buttons = createButtons(),
            marketPriceBlockState = MarketPriceBlockState.Loading(currencyName = currencyName),
            txHistoryState = TxHistoryState.Content(
                contentItems = MutableStateFlow(
                    value = TxHistoryState.getDefaultLoadingTransactions(clickIntents::onExploreClick),
                ),
            ),
        )
    }

    private fun UserWallet.getPrimaryCurrencyName(): String {
        return scanResponse.cardTypesResolver.getBlockchain().currency
    }

    private fun createTopBarConfig(): WalletTopBarConfig {
        return WalletTopBarConfig(
            onScanCardClick = clickIntents::onScanCardClick,
            onMoreClick = clickIntents::onDetailsClick,
        )
    }

    private fun createWalletsListConfig(value: SkeletonModel): WalletsListConfig {
        return WalletsListConfig(
            selectedWalletIndex = value.selectedWalletIndex,
            wallets = value.wallets.mapIndexed(::createWalletCardState).toImmutableList(),
            onWalletChange = clickIntents::onWalletChange,
        )
    }

    /**
     * Create wallet card state by [index] and [wallet].
     * If current wallet card state is initialized, then method returns it.
     * Otherwise, returns loading wallet card state.
     */
    private fun createWalletCardState(index: Int, wallet: UserWallet): WalletCardState {
        return currentStateProvider().getInitializedWalletCardState(index) ?: wallet.mapToWalletCardState()
    }

    private fun WalletState.getInitializedWalletCardState(index: Int): WalletCardState? {
        return (this as? WalletState.ContentState)?.walletsListConfig?.wallets?.getOrNull(index)
    }

    private fun UserWallet.mapToWalletCardState(): WalletCardState {
        return if (isLocked) mapToLockedWalletCardState() else mapToLoadingWalletCardState()
    }

    private fun UserWallet.mapToLockedWalletCardState(): WalletCardState {
        return WalletCardState.LockedContent(
            id = walletId,
            title = name,
            additionalInfo = createAdditionalInfo(),
            imageResId = createImageResId(),
            onRenameClick = clickIntents::onRenameClick,
            onDeleteClick = clickIntents::onDeleteClick,
        )
    }

    private fun UserWallet.mapToLoadingWalletCardState(): WalletCardState {
        return WalletCardState.Loading(
            id = walletId,
            title = name,
            additionalInfo = createAdditionalInfo(),
            imageResId = createImageResId(),
            onRenameClick = clickIntents::onRenameClick,
            onDeleteClick = clickIntents::onDeleteClick,
        )
    }

    private fun UserWallet.createAdditionalInfo(): TextReference? {
        return if (isMultiCurrency) {
            WalletAdditionalInfoFactory.resolve(cardTypesResolver = scanResponse.cardTypesResolver, wallet = this)
        } else {
            null
        }
    }

    @DrawableRes
    private fun UserWallet.createImageResId(): Int? {
        return WalletImageResolver.resolve(cardTypesResolver = scanResponse.cardTypesResolver)
    }

    private fun createPullToRefreshConfig(): WalletPullToRefreshConfig {
        return WalletPullToRefreshConfig(isRefreshing = false, onRefresh = clickIntents::onRefreshSwipe)
    }

    private fun createButtons(): PersistentList<WalletManageButton> {
        return persistentListOf(
            WalletManageButton.Buy(enabled = false, onClick = {}),
            WalletManageButton.Send(enabled = false, onClick = {}),
            WalletManageButton.Receive(onClick = {}),
            WalletManageButton.Sell(enabled = false, onClick = {}),
        )
    }

    data class SkeletonModel(val wallets: List<UserWallet>, val selectedWalletIndex: Int)
}