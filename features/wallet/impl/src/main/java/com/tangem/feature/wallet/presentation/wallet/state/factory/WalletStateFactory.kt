package com.tangem.feature.wallet.presentation.wallet.state.factory

import androidx.paging.PagingData
import arrow.core.Either
import com.tangem.common.Provider
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.tokens.error.CurrencyError
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletBottomSheetConfig
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletManageButton
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state.factory.txhistory.WalletLoadedTxHistoryConverter
import com.tangem.feature.wallet.presentation.wallet.state.factory.txhistory.WalletLoadingTxHistoryConverter
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow

/**
 * Main factory for creating [WalletState]
 *
 * @property currentStateProvider            current ui state provider
 * @property currentCardTypeResolverProvider current card type resolver
 * @property isLockedWalletProvider          current wallet is locked or not
 * @property clickIntents                    screen click intents
 */
internal class WalletStateFactory(
    private val currentStateProvider: Provider<WalletState>,
    private val currentCardTypeResolverProvider: Provider<CardTypesResolver>,
    private val isLockedWalletProvider: Provider<Boolean>,
    private val clickIntents: WalletClickIntents,
) {

    private val skeletonConverter by lazy { WalletSkeletonStateConverter(currentStateProvider, clickIntents) }

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

    private val singleCurrencyLoadedBalanceConverter by lazy {
        WalletSingleCurrencyLoadedBalanceConverter(
            currentStateProvider = currentStateProvider,
            fiatCurrencyCode = "USD", // TODO: [REDACTED_JIRA]
            fiatCurrencySymbol = "$", // TODO: [REDACTED_JIRA]
        )
    }

    fun getInitialState(): WalletState = WalletState.Initial(onBackClick = clickIntents::onBackClick)

    fun getSkeletonState(wallets: List<UserWallet>, selectedWalletIndex: Int): WalletState {
        return skeletonConverter.convert(
            value = WalletSkeletonStateConverter.SkeletonModel(
                wallets = wallets,
                selectedWalletIndex = selectedWalletIndex,
            ),
        )
    }

    fun getStateByTokensList(tokenListEither: Either<TokenListError, TokenList>, isRefreshing: Boolean): WalletState {
        return loadedTokensListConverter.convert(
            value = WalletLoadedTokensListConverter.LoadedTokensListModel(
                tokenListEither = tokenListEither,
                isRefreshing = isRefreshing,
            ),
        )
    }

    fun getStateByNotifications(notifications: ImmutableList<WalletNotification>): WalletState {
        return when (val state = currentStateProvider()) {
            is WalletMultiCurrencyState.Content -> state.copy(notifications = notifications)
            is WalletSingleCurrencyState.Content -> state.copy(notifications = notifications)
            else -> state
        }
    }

    fun getStateAfterContentRefreshing(): WalletState {
        return currentStateProvider()
    }

    fun getStateWithOpenBottomSheet(content: WalletBottomSheetConfig.BottomSheetContentConfig): WalletState {
        return when (val state = currentStateProvider() as WalletState.ContentState) {
            is WalletMultiCurrencyState.Content -> state.copy(
                bottomSheetConfig = WalletBottomSheetConfig(
                    isShow = true,
                    onDismissRequest = clickIntents::onBottomSheetDismiss,
                    content = content,
                ),
            )
            is WalletMultiCurrencyState.Locked -> state.copy(
                isBottomSheetShow = true,
                onBottomSheetDismiss = clickIntents::onBottomSheetDismiss,
            )
            is WalletSingleCurrencyState.Content -> state.copy(
                bottomSheetConfig = WalletBottomSheetConfig(
                    isShow = true,
                    onDismissRequest = clickIntents::onBottomSheetDismiss,
                    content = content,
                ),
            )
            is WalletSingleCurrencyState.Locked -> state.copy(
                isBottomSheetShow = true,
                onBottomSheetDismiss = clickIntents::onBottomSheetDismiss,
            )
        }
    }

    fun getStateWithClosedBottomSheet(): WalletState {
        return when (val state = currentStateProvider() as WalletState.ContentState) {
            is WalletMultiCurrencyState.Content -> state.copy(
                bottomSheetConfig = state.bottomSheetConfig?.copy(isShow = false),
            )
            is WalletMultiCurrencyState.Locked -> state.copy(isBottomSheetShow = false)
            is WalletSingleCurrencyState.Content -> state.copy(
                bottomSheetConfig = state.bottomSheetConfig?.copy(isShow = false),
            )
            is WalletSingleCurrencyState.Locked -> state.copy(isBottomSheetShow = false)
        }
    }

    fun getLoadingTxHistoryState(itemsCountEither: Either<TxHistoryStateError, Int>): WalletState {
        return loadingTransactionsStateConverter.convert(value = itemsCountEither)
    }

    fun getLoadedTxHistoryState(
        txHistoryEither: Either<TxHistoryListError, Flow<PagingData<TxHistoryItem>>>,
    ): WalletState {
        return loadedTxHistoryConverter.convert(txHistoryEither)
    }

    fun getLockedState(): WalletState {
        val cardTypeResolver = currentCardTypeResolverProvider()
        val state = requireNotNull(currentStateProvider() as? WalletState.ContentState)
        return if (cardTypeResolver.isMultiwalletAllowed()) {
            WalletMultiCurrencyState.Locked(
                onBackClick = state.onBackClick,
                topBarConfig = state.topBarConfig.copy(
                    onMoreClick = clickIntents::onUnlockWalletNotificationClick,
                ),
                walletsListConfig = state.walletsListConfig,
                pullToRefreshConfig = state.pullToRefreshConfig,
                onUnlockWalletsNotificationClick = clickIntents::onUnlockWalletNotificationClick,
                onUnlockClick = clickIntents::onUnlockWalletClick,
                onScanClick = clickIntents::onScanCardClick,
            )
        } else {
            WalletSingleCurrencyState.Locked(
                onBackClick = state.onBackClick,
                topBarConfig = state.topBarConfig.copy(
                    onMoreClick = clickIntents::onUnlockWalletNotificationClick,
                ),
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

    fun getSingleCurrencyLoadedBalanceState(
        cryptoCurrencyEither: Either<CurrencyError, CryptoCurrencyStatus>,
    ): WalletState {
        return singleCurrencyLoadedBalanceConverter.convert(cryptoCurrencyEither)
    }
}