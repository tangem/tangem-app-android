package com.tangem.feature.wallet.presentation.wallet.state.factory

import com.tangem.common.Provider
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.domain.WalletImageResolver
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.*
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletsUpdateActionResolver.Action.UnlockWallet as UnlockWalletAction

/**
 * Converter that responds on wallets unlocking action. Returns [WalletState] with unlocked wallets.
 *
 * @property currentStateProvider current ui state provider
 * @property clickIntents         screen click intents
 *
[REDACTED_AUTHOR]
 */
internal class WalletsUnlockStateConverter(
    private val currentStateProvider: Provider<WalletState>,
    private val clickIntents: WalletClickIntents,
) : Converter<UnlockWalletAction, WalletState> {

    override fun convert(value: UnlockWalletAction): WalletState {
        return when (val state = currentStateProvider()) {
            is WalletMultiCurrencyState.Locked -> state.toMultiCurrencyContentState(value)
            is WalletSingleCurrencyState.Locked -> state.toSingleCurrencyContentState(value)
            is WalletState.Initial,
            is WalletMultiCurrencyState.Content,
            is WalletSingleCurrencyState.Content,
            -> state
        }
    }

    private fun WalletMultiCurrencyState.Locked.toMultiCurrencyContentState(action: UnlockWalletAction): WalletState {
        return WalletMultiCurrencyState.Content(
            onBackClick = onBackClick,
            topBarConfig = topBarConfig.updateCallback(),
            walletsListConfig = walletsListConfig.unlockWallets(action),
            pullToRefreshConfig = pullToRefreshConfig.stopRefreshing(),
            tokensListState = WalletTokensListState.Loading(),
            notifications = persistentListOf(),
            bottomSheetConfig = null,
            tokenActionsBottomSheet = null,
            onManageTokensClick = clickIntents::onManageTokensClick,
        )
    }

    private fun WalletSingleCurrencyState.Locked.toSingleCurrencyContentState(action: UnlockWalletAction): WalletState {
        return WalletSingleCurrencyState.Content(
            onBackClick = onBackClick,
            topBarConfig = topBarConfig.updateCallback(),
            walletsListConfig = walletsListConfig.unlockWallets(action),
            pullToRefreshConfig = pullToRefreshConfig.stopRefreshing(),
            notifications = persistentListOf(),
            bottomSheetConfig = null,
            buttons = buttons,
            marketPriceBlockState = MarketPriceBlockState.Loading(
                currencyName = action.selectedWallet.getPrimaryCurrencyName(),
            ),
            txHistoryState = TxHistoryState.Content(
                contentItems = MutableStateFlow(
                    value = TxHistoryState.getDefaultLoadingTransactions(clickIntents::onExploreClick),
                ),
            ),
        )
    }

    private fun WalletTopBarConfig.updateCallback(): WalletTopBarConfig {
        return copy(onMoreClick = clickIntents::onDetailsClick)
    }

    private fun WalletsListConfig.unlockWallets(action: UnlockWalletAction): WalletsListConfig {
        return this.copy(
            selectedWalletIndex = action.selectedWalletIndex,
            wallets = wallets.unlockWallets(action),
        )
    }

    private fun List<WalletCardState>.unlockWallets(action: UnlockWalletAction): ImmutableList<WalletCardState> {
        return this
            .map { prevWallet ->
                if (prevWallet is WalletCardState.LockedContent && action.isUnlockedWallet(prevWallet.id)) {
                    prevWallet.mapToLoadingWalletCardState(
                        userWallet = action.getUnlockWallet(prevWallet.id),
                    )
                } else {
                    prevWallet
                }
            }
            .toImmutableList()
    }

    private fun UnlockWalletAction.isUnlockedWallet(walletId: UserWalletId): Boolean {
        return unlockedWallets.any { it.walletId == walletId }
    }

    private fun UnlockWalletAction.getUnlockWallet(walletId: UserWalletId): UserWallet {
        return unlockedWallets.firstOrNull { it.walletId == walletId }
            ?: error("Unlocked wallet with id $walletId not found")
    }

    private fun WalletCardState.mapToLoadingWalletCardState(userWallet: UserWallet): WalletCardState {
        return WalletCardState.Loading(
            id = id,
            title = title,
            additionalInfo = userWallet.createAdditionalInfo(),
            imageResId = WalletImageResolver.resolve(cardTypesResolver = userWallet.scanResponse.cardTypesResolver),
            onRenameClick = onRenameClick,
            onDeleteClick = onDeleteClick,
        )
    }

    private fun UserWallet.createAdditionalInfo(): TextReference? {
        return if (isMultiCurrency) {
            WalletAdditionalInfoFactory.resolve(cardTypesResolver = scanResponse.cardTypesResolver, wallet = this)
        } else {
            null
        }
    }

    private fun WalletPullToRefreshConfig.stopRefreshing(): WalletPullToRefreshConfig {
        return copy(isRefreshing = false)
    }

    private fun UserWallet.getPrimaryCurrencyName(): String {
        return scanResponse.cardTypesResolver.getBlockchain().currency
    }
}