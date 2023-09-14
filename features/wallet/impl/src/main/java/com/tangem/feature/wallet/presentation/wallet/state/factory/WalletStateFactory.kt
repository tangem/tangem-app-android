package com.tangem.feature.wallet.presentation.wallet.state.factory

import androidx.paging.PagingData
import arrow.core.Either
import com.tangem.common.Provider
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.ActionsBottomSheetConfig
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletBottomSheetConfig
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state.factory.txhistory.WalletLoadedTxHistoryConverter
import com.tangem.feature.wallet.presentation.wallet.state.factory.txhistory.WalletLoadingTxHistoryConverter
import com.tangem.feature.wallet.presentation.wallet.utils.CurrencyStatusErrorConverter
import com.tangem.feature.wallet.presentation.wallet.utils.TokenListErrorConverter
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletsUpdateActionResolver
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

/**
 * Main factory for creating [WalletState]
 *
 * @property currentStateProvider            current ui state provider
 * @property currentCardTypeResolverProvider current card type resolver
 * @property currentWalletProvider           current wallet
 * @property clickIntents                    screen click intents
 */
internal class WalletStateFactory(
    private val currentStateProvider: Provider<WalletState>,
    private val currentCardTypeResolverProvider: Provider<CardTypesResolver>,
    private val currentWalletProvider: Provider<UserWallet>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val clickIntents: WalletClickIntents,
) {

    private val tokenActionsProvider by lazy { TokenActionsProvider(clickIntents) }

    private val skeletonConverter by lazy { WalletSkeletonStateConverter(currentStateProvider, clickIntents) }

    private val walletsUnlockStateConverter by lazy { WalletsUnlockStateConverter(currentStateProvider, clickIntents) }

    private val walletRenameStateConverter by lazy { WalletRenameStateConverter(currentStateProvider) }

    private val walletDeleteStateConverter by lazy { WalletDeleteStateConverter(currentStateProvider) }

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
            cardTypeResolverProvider = currentCardTypeResolverProvider,
            currentWalletProvider = currentWalletProvider,
            appCurrencyProvider = appCurrencyProvider,
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
            cardTypeResolverProvider = currentCardTypeResolverProvider,
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

    fun getStateByTokensList(maybeTokenList: Either<TokenListError, TokenList>): WalletState {
        return loadedTokensListConverter.convert(maybeTokenList)
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

    fun getStateWithOpenWalletBottomSheet(content: WalletBottomSheetConfig.BottomSheetContentConfig): WalletState {
        return when (val state = currentStateProvider() as WalletState.ContentState) {
            is WalletMultiCurrencyState.Content -> state.copy(
                bottomSheetConfig = WalletBottomSheetConfig(
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
                bottomSheetConfig = WalletBottomSheetConfig(
                    isShow = true,
                    onDismissRequest = clickIntents::onDismissBottomSheet,
                    content = content,
                ),
            )
            is WalletSingleCurrencyState.Locked -> state.copy(
                isBottomSheetShow = true,
                onBottomSheetDismiss = clickIntents::onDismissBottomSheet,
            )
            else -> state
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
        return when (val state = currentStateProvider() as WalletState.ContentState) {
            is WalletMultiCurrencyState.Content -> state.copy(
                tokenActionsBottomSheet = ActionsBottomSheetConfig(
                    isShow = true,
                    actions = tokenActionsProvider.provideActions(tokenActions),
                    onDismissRequest = clickIntents::onDismissActionsBottomSheet,
                ),
            )
            else -> state
        }
    }

    fun getLoadingTxHistoryState(
        itemsCountEither: Either<TxHistoryStateError, Int>,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): WalletState {
        return loadingTransactionsStateConverter.convert(
            WalletLoadingTxHistoryConverter.WalletLoadingTxHistoryModel(
                historyLoadingState = itemsCountEither,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
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
}
