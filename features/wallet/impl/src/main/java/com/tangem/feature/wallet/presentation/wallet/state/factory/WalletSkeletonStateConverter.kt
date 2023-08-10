package com.tangem.feature.wallet.presentation.wallet.state.factory

import com.tangem.common.Provider
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

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
        val cardTypeResolver = value.wallets[value.selectedWalletIndex].scanResponse.cardTypesResolver

        return if (cardTypeResolver.isMultiwalletAllowed()) {
            createMultiCurrencyState(value = value)
        } else {
            createSingleCurrencyState(value = value, currencyName = cardTypeResolver.getBlockchain().currency)
        }
    }

    private fun createMultiCurrencyState(value: SkeletonModel): WalletMultiCurrencyState {
        return WalletMultiCurrencyState.Content(
            onBackClick = clickIntents::onBackClick,
            topBarConfig = createTopBarConfig(),
            walletsListConfig = createWalletsListConfig(value),
            pullToRefreshConfig = createPullToRefreshConfig(),
            tokensListState = WalletTokensListState.Loading,
            notifications = persistentListOf(),
            bottomSheetConfig = null,
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
            buttons = getButtons(),
            marketPriceBlockState = MarketPriceBlockState.Loading(currencyName = currencyName),
            txHistoryState = TxHistoryState.Loading(onExploreClick = clickIntents::onExploreClick),
        )
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
            wallets = value.wallets.map(::createWalletState).toImmutableList(),
            onWalletChange = clickIntents::onWalletChange,
        )
    }

    private fun createWalletState(wallet: UserWallet): WalletCardState {
        val state = currentStateProvider()

        // If it isn't first initialization (example, when user unlocks wallet)
        return if (state is WalletState.ContentState) {
            val initializedWallet = state.walletsListConfig.wallets.first { it.id == wallet.walletId }

            // If wallet is initialized, return it, otherwise return loading state
            if (initializedWallet !is WalletCardState.Loading) {
                initializedWallet
            } else {
                createWalletLoadingState(wallet)
            }
        } else {
            createWalletLoadingState(wallet)
        }
    }

    private fun createWalletLoadingState(wallet: UserWallet): WalletCardState {
        val cardTypeResolver = wallet.scanResponse.cardTypesResolver

        return WalletCardState.Loading(
            id = wallet.walletId,
            title = wallet.name,
            additionalInfo = WalletAdditionalInfoFactory.resolve(
                cardTypesResolver = cardTypeResolver,
                isLocked = wallet.isLocked,
            ),
            imageResId = WalletImageResolver.resolve(cardTypesResolver = cardTypeResolver),
        )
    }

    private fun createPullToRefreshConfig(): WalletPullToRefreshConfig {
        return WalletPullToRefreshConfig(isRefreshing = false, onRefresh = clickIntents::onRefreshSwipe)
    }

    // TODO: [REDACTED_JIRA]
    private fun getButtons(): ImmutableList<WalletManageButton> {
        return persistentListOf(
            WalletManageButton.Buy(),
            WalletManageButton.Send(),
            WalletManageButton.Receive(onClick = {}),
            WalletManageButton.Exchange(),
            WalletManageButton.Sell(),
            WalletManageButton.CopyAddress(onClick = {}),
        )
    }

    data class SkeletonModel(val wallets: List<UserWallet>, val selectedWalletIndex: Int)
}