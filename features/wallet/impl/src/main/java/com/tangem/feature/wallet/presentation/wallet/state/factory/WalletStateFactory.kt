package com.tangem.feature.wallet.presentation.wallet.state.factory

import androidx.paging.PagingData
import arrow.core.Either
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.*
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state.factory.txhistory.WalletLoadedTxHistoryConverter
import com.tangem.feature.wallet.presentation.wallet.state.factory.txhistory.WalletLoadingTxHistoryConverter
import com.tangem.feature.wallet.presentation.wallet.utils.CurrencyStatusErrorConverter
import com.tangem.feature.wallet.presentation.wallet.utils.HiddenStateConverter
import com.tangem.feature.wallet.presentation.wallet.utils.TokenListErrorConverter
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletsUpdateActionResolver
import com.tangem.utils.Provider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

/**
 * Main factory for creating [WalletState]
 *
 * @property currentStateProvider            current ui state provider
 * @property currentCardTypeResolverProvider current card type resolver
 * @property currentWalletProvider           current wallet
 * @property appCurrencyProvider             app currency provider
 * @property clickIntents                    screen click intents
 */
@Suppress("TooManyFunctions")
internal class WalletStateFactory(
    private val currentStateProvider: Provider<WalletState>,
    private val currentCardTypeResolverProvider: Provider<CardTypesResolver>,
    private val currentWalletProvider: Provider<UserWallet>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val isBalanceHiddenProvider: Provider<Boolean>,
    private val clickIntents: WalletClickIntents,
) {

    private val tokenActionsProvider by lazy { TokenActionsProvider(currentWalletProvider, clickIntents) }

    private val skeletonConverter by lazy {
        WalletSkeletonStateConverter(
            currentStateProvider = currentStateProvider,
            isBalanceHiddenProvider = isBalanceHiddenProvider,
            clickIntents = clickIntents,
        )
    }

    private val walletsUnlockStateConverter by lazy { WalletsUnlockStateConverter(currentStateProvider, clickIntents) }

    private val walletRenameStateConverter by lazy { WalletRenameStateConverter(currentStateProvider) }

    private val walletDeleteStateConverter by lazy { WalletDeleteStateConverter(currentStateProvider) }

    private val hiddenStateConverter by lazy { HiddenStateConverter(currentStateProvider) }

    private val walletUpdateCardCountConverter by lazy {
        WalletUpdateCardCountConverter(
            currentStateProvider,
            currentWalletProvider,
        )
    }

    private val tokenListErrorConverter by lazy {
        TokenListErrorConverter(currentStateProvider)
    }
    private val currencyStatusErrorConverter by lazy {
        CurrencyStatusErrorConverter(currentStateProvider)
    }
    private val loadedTokensListConverter by lazy {
        WalletLoadedTokensListConverter(
            currentStateProvider = currentStateProvider,
            tokenListErrorConverter = tokenListErrorConverter,
            appCurrencyProvider = appCurrencyProvider,
            currentWalletProvider = currentWalletProvider,
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

    private val singleCurrencyLoadedBalanceConverter by lazy {
        WalletSingleCurrencyLoadedBalanceConverter(
            currentStateProvider = currentStateProvider,
            appCurrencyProvider = appCurrencyProvider,
            currentWalletProvider = currentWalletProvider,
            currencyStatusErrorConverter = currencyStatusErrorConverter,
        )
    }

    private val lockedConverter by lazy {
        WalletLockedConverter(
            currentStateProvider = currentStateProvider,
            clickIntents = clickIntents,
        )
    }

    private val refreshStateConverter by lazy {
        WalletRefreshStateConverter(currentStateProvider)
    }

    private val cryptoCurrencyActionsConverter by lazy {
        WalletCryptoCurrencyActionsConverter(
            currentWalletProvider = currentWalletProvider,
            currentStateProvider = currentStateProvider,
            clickIntents = clickIntents,
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

    fun getStateWithUpdatedWalletName(name: String): WalletState = walletRenameStateConverter.convert(value = name)

    fun getStateWithUpdatedWalletCardCount(): WalletState = walletUpdateCardCountConverter.convert(Unit)

    fun getUnlockedState(action: WalletsUpdateActionResolver.Action.UnlockWallet): WalletState {
        return walletsUnlockStateConverter.convert(value = action)
    }

    fun getStateWithoutDeletedWallet(
        cacheState: WalletState.ContentState,
        action: WalletsUpdateActionResolver.Action.DeleteWallet,
    ): WalletState {
        return walletDeleteStateConverter.convert(
            value = WalletDeleteStateConverter.DeleteWalletModel(cacheState = cacheState, action = action),
        )
    }

    fun getStateByTokensList(maybeTokenListWithWallet: Either<TokenListError, TokenListWithWallet>): WalletState {
        return loadedTokensListConverter.convert(maybeTokenListWithWallet)
    }

    fun getStateByTokenListError(error: TokenListError): WalletState {
        return tokenListErrorConverter.convert(error)
    }

    fun getStateByNotifications(notifications: ImmutableList<WalletNotification>): WalletState {
        return when (val state = currentStateProvider()) {
            is WalletMultiCurrencyState.Content -> state.copy(notifications = notifications)
            is WalletSingleCurrencyState.Content -> state.copy(notifications = notifications)
            else -> state
        }
    }

    fun getRefreshingState(): WalletState = refreshStateConverter.convert(value = true)

    fun getRefreshedState(): WalletState = refreshStateConverter.convert(value = false)

    fun getStateWithOpenWalletBottomSheet(content: TangemBottomSheetConfigContent): WalletState {
        return when (val state = currentStateProvider() as WalletState.ContentState) {
            is WalletMultiCurrencyState.Content -> state.copy(
                bottomSheetConfig = TangemBottomSheetConfig(
                    isShow = true,
                    onDismissRequest = clickIntents::onDismissBottomSheet,
                    content = content,
                ),
            )
            is WalletMultiCurrencyState.Locked -> state.copy(
                isBottomSheetShow = true,
                onBottomSheetDismiss = clickIntents::onDismissBottomSheet,
            )
            is WalletSingleCurrencyState.Content -> state.copy(
                bottomSheetConfig = TangemBottomSheetConfig(
                    isShow = true,
                    onDismissRequest = clickIntents::onDismissBottomSheet,
                    content = content,
                ),
            )
            is WalletSingleCurrencyState.Locked -> state.copy(
                isBottomSheetShow = true,
                onBottomSheetDismiss = clickIntents::onDismissBottomSheet,
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

    fun getStateWithTokenActionBottomSheet(tokenActions: TokenActionsState): WalletState {
        return getStateWithOpenWalletBottomSheet(
            content = ActionsBottomSheetConfig(actions = tokenActionsProvider.provideActions(tokenActions)),
        )
    }

    fun getLoadingTxHistoryState(
        itemsCountEither: Either<TxHistoryStateError, Int>,
        pendingTransactions: Set<TxHistoryItem>,
    ): WalletState {
        return loadingTransactionsStateConverter.convert(
            WalletLoadingTxHistoryConverter.WalletLoadingTxHistoryModel(
                historyLoadingState = itemsCountEither,
                pendingTransactions = pendingTransactions,
            ),
        )
    }

    fun getLoadedTxHistoryState(
        txHistoryEither: Either<TxHistoryListError, Flow<PagingData<TxHistoryItem>>>,
    ): WalletState {
        return loadedTxHistoryConverter.convert(txHistoryEither)
    }

    fun getLockedState(): WalletState = lockedConverter.convert(Unit)

    fun getSingleCurrencyLoadedBalanceState(
        maybeCryptoCurrencyStatus: Either<CurrencyStatusError, CryptoCurrencyStatus>,
    ): WalletState {
        return singleCurrencyLoadedBalanceConverter.convert(maybeCryptoCurrencyStatus)
    }

    fun getSingleCurrencyManageButtonsState(actionsState: TokenActionsState): WalletState {
        return cryptoCurrencyActionsConverter.convert(value = actionsState)
    }

    fun getStateByCurrencyStatusError(error: CurrencyStatusError): WalletState {
        return currencyStatusErrorConverter.convert(error)
    }

    fun getHiddenBalanceState(isBalanceHidden: Boolean): WalletState {
        return hiddenStateConverter.convert(isBalanceHidden)
    }

    fun getStateAndTriggerEvent(
        state: WalletState,
        event: WalletEvent,
        setUiState: (WalletState) -> Unit,
    ): WalletState {
        return when (state) {
            is WalletState.ContentState -> state.copySealed(
                event = triggeredEvent(
                    data = event,
                    onConsume = {
                        val currentState = currentStateProvider()
                        if (currentState is WalletState.ContentState) {
                            setUiState(currentState.copySealed(event = consumedEvent()))
                        }
                    },
                ),
            )
            is WalletState.Initial -> state
        }
    }
}