package com.tangem.feature.wallet.presentation.wallet.state.factory

import arrow.core.Either
import com.tangem.common.Provider
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.WalletBottomSheetConfig
import com.tangem.feature.wallet.presentation.wallet.state.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.feature.wallet.presentation.wallet.state.factory.WalletLoadedTokensListConverter.LoadedTokensListModel
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickCallbacks
import kotlinx.collections.immutable.ImmutableList

/**
 * Main factory for creating [WalletStateHolder]
 *
 * @property currentStateProvider current ui state provider
 * @property clickCallbacks       screen click callbacks
 */
internal class WalletStateFactory(
    private val currentStateProvider: Provider<WalletStateHolder>,
    private val clickCallbacks: WalletClickCallbacks,
) {

    private val skeletonConverter = WalletSkeletonStateConverter(clickCallbacks = clickCallbacks)
    private val loadedTokensListConverter = WalletLoadedTokensListConverter(currentStateProvider = currentStateProvider)

    fun getInitialState(): WalletStateHolder = WalletStateHolder.Loading(onBackClick = clickCallbacks::onBackClick)

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
                    onDismissRequest = { state.copySealed(bottomSheet = state.bottomSheet?.copy(isShow = false)) },
                    content = content,
                ),
            )
        }
    }
}
