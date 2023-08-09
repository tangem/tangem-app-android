package com.tangem.feature.wallet.presentation.wallet.state.factory

import androidx.paging.PagingData
import arrow.core.Either
import com.tangem.common.Provider
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.WalletLoading
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletBottomSheetConfig
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletManageButton
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state.factory.WalletLoadedTokensListConverter.LoadedTokensListModel
import com.tangem.feature.wallet.presentation.wallet.state.factory.txhistory.WalletLoadedTxHistoryConverter
import com.tangem.feature.wallet.presentation.wallet.state.factory.txhistory.WalletLoadingTxHistoryConverter
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow

/**
 * Main factory for creating [WalletStateHolder]
 *
 * @property currentStateProvider            current ui state provider
 * @property currentCardTypeResolverProvider current card type resolver
 * @property isLockedWalletProvider          current wallet is locked or not
 * @property clickIntents                    screen click intents
 */
internal class WalletStateFactory(
    private val currentStateProvider: Provider<WalletStateHolder>,
    private val currentCardTypeResolverProvider: Provider<CardTypesResolver>,
    private val isLockedWalletProvider: Provider<Boolean>,
    private val clickIntents: WalletClickIntents,
) {

    private val skeletonConverter by lazy { WalletSkeletonStateConverter(clickIntents = clickIntents) }

    private val loadedTokensListConverter by lazy {
        WalletLoadedTokensListConverter(
            currentStateProvider = currentStateProvider,
            cardTypeResolverProvider = currentCardTypeResolverProvider,
            isLockedWalletProvider = isLockedWalletProvider,
            clickIntents = clickIntents,
        )
    }

    private val loadingTransactionsStateConverter by lazy {
        WalletLoadingTxHistoryConverter(
            currentStateProvider = currentStateProvider,
            currentCardTypeResolverProvider = currentCardTypeResolverProvider,
            clickIntents = clickIntents,
        )
    }

    private val loadedTxHistoryConverter by lazy {
        WalletLoadedTxHistoryConverter(
            currentStateProvider = currentStateProvider,
            currentCardTypeResolverProvider = currentCardTypeResolverProvider,
            clickIntents = clickIntents,
        )
    }

    fun getInitialState(): WalletStateHolder = WalletLoading(onBackClick = clickIntents::onBackClick)

    fun getSkeletonState(wallets: List<UserWallet>, index: Int): WalletStateHolder {
        return skeletonConverter.convert(
            value = WalletSkeletonStateConverter.SkeletonModel(wallets = wallets, selectedWalletIndex = index),
        )
    }

    fun getStateByTokensList(
        tokenListEither: Either<TokenListError, TokenList>,
        isRefreshing: Boolean,
    ): WalletStateHolder {
        return loadedTokensListConverter.convert(
            value = LoadedTokensListModel(tokenListEither = tokenListEither, isRefreshing = isRefreshing),
        )
    }

    fun getStateByNotifications(notifications: ImmutableList<WalletNotification>): WalletStateHolder {
        return currentStateProvider().copySealed(notifications = notifications)
    }

    fun getStateAfterWalletChanging(index: Int): WalletStateHolder {
        return currentStateProvider().let { stateHolder ->
            stateHolder.copySealed(walletsListConfig = stateHolder.walletsListConfig.copy(selectedWalletIndex = index))
        }
    }

    fun getStateAfterContentRefreshing(): WalletStateHolder {
        return currentStateProvider().let { state ->
            state.copySealed(pullToRefreshConfig = state.pullToRefreshConfig.copy(isRefreshing = true))
        }
    }

    fun getStateWithOpenBottomSheet(content: WalletBottomSheetConfig.BottomSheetContentConfig): WalletStateHolder {
        return currentStateProvider().let { state ->
            state.copySealed(
                bottomSheet = WalletBottomSheetConfig(
                    isShow = true,
                    onDismissRequest = {
                        state.copySealed(bottomSheet = state.bottomSheetConfig?.copy(isShow = false))
                    },
                    content = content,
                ),
            )
        }
    }

    fun getLoadingTxHistoryState(itemsCountEither: Either<TxHistoryStateError, Int>): WalletStateHolder {
        return loadingTransactionsStateConverter.convert(value = itemsCountEither)
    }

    fun getLoadedTxHistoryState(
        txHistoryEither: Either<TxHistoryListError, Flow<PagingData<TxHistoryItem>>>,
    ): WalletStateHolder {
        return loadedTxHistoryConverter.convert(txHistoryEither)
    }

    fun getLockedState(): WalletStateHolder {
        val cardTypeResolver = currentCardTypeResolverProvider()
        val state = currentStateProvider()
        return if (cardTypeResolver.isMultiwalletAllowed()) {
            WalletMultiCurrencyState.Locked(
                onBackClick = state.onBackClick,
                topBarConfig = state.topBarConfig,
                walletsListConfig = state.walletsListConfig,
                pullToRefreshConfig = state.pullToRefreshConfig,
                onUnlockWalletsNotificationClick = clickIntents::onUnlockWalletNotificationClick,
                onUnlockClick = clickIntents::onUnlockWalletClick,
                onScanClick = clickIntents::onScanCardClick,
            )
        } else {
            WalletSingleCurrencyState.Locked(
                onBackClick = state.onBackClick,
                topBarConfig = state.topBarConfig,
                walletsListConfig = state.walletsListConfig,
                pullToRefreshConfig = state.pullToRefreshConfig,
                buttons = getButtons(),
                onUnlockWalletsNotificationClick = clickIntents::onUnlockWalletNotificationClick,
                onUnlockClick = clickIntents::onUnlockWalletClick,
                onScanClick = clickIntents::onScanCardClick,
                onExploreClick = clickIntents::onExploreClick,
            )
        }
    }

    // TODO: [REDACTED_JIRA]
    private fun getButtons(): ImmutableList<ActionButtonConfig> {
        return persistentListOf(
            WalletManageButton.Buy(onClick = {}),
            WalletManageButton.Send(onClick = {}),
            WalletManageButton.Receive(onClick = {}),
            WalletManageButton.Exchange(onClick = {}),
            WalletManageButton.CopyAddress(onClick = {}),
        )
            .map(WalletManageButton::config)
            .toImmutableList()
    }
}