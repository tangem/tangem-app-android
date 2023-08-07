package com.tangem.feature.wallet.presentation.wallet.state.factory

import androidx.paging.PagingData
import arrow.core.Either
import com.tangem.common.Provider
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.txhistory.error.TxHistoryListError
import com.tangem.domain.txhistory.error.TxHistoryStateError
import com.tangem.domain.txhistory.model.TxHistoryItem
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.WalletBottomSheetConfig
import com.tangem.feature.wallet.presentation.wallet.state.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.feature.wallet.presentation.wallet.state.factory.WalletLoadedTokensListConverter.LoadedTokensListModel
import com.tangem.feature.wallet.presentation.wallet.state.factory.txhistory.WalletLoadedTxHistoryConverter
import com.tangem.feature.wallet.presentation.wallet.state.factory.txhistory.WalletLoadingTxHistoryConverter
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

/**
 * Main factory for creating [WalletStateHolder]
 *
 * @property currentStateProvider            current ui state provider
 * @param currentCardTypeResolverProvider current card type resolver
 * @property clickIntents                    screen click intents
 */
internal class WalletStateFactory(
    private val currentStateProvider: Provider<WalletStateHolder>,
    currentCardTypeResolverProvider: Provider<CardTypesResolver>,
    private val clickIntents: WalletClickIntents,
) {

    private val skeletonConverter by lazy { WalletSkeletonStateConverter(clickIntents = clickIntents) }

    private val loadedTokensListConverter by lazy {
        WalletLoadedTokensListConverter(
            currentStateProvider = currentStateProvider,
            cardTypeResolverProvider = currentCardTypeResolverProvider,
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

    fun getInitialState(): WalletStateHolder = WalletStateHolder.Loading(onBackClick = clickIntents::onBackClick)

    fun getSkeletonState(wallets: List<UserWallet>): WalletStateHolder = skeletonConverter.convert(wallets)

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
}
