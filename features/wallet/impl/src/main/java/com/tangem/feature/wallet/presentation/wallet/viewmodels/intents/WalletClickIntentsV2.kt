package com.tangem.feature.wallet.presentation.wallet.viewmodels.intents

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.settings.NeverToShowWalletsScrollPreview
import com.tangem.domain.tokens.FetchCardTokenListUseCase
import com.tangem.domain.tokens.FetchCurrencyStatusUseCase
import com.tangem.domain.tokens.FetchTokenListUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.wallets.usecase.SelectWalletUseCase
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import com.tangem.feature.wallet.presentation.wallet.analytics.PortfolioEvent
import com.tangem.feature.wallet.presentation.wallet.domain.unwrap
import com.tangem.feature.wallet.presentation.wallet.state2.WalletState
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateHolderV2
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.SetRefreshStateTransformer
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.SetTokenListErrorTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
* [REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
internal class WalletClickIntentsV2 @Inject constructor(
    private val walletCardClickIntentsImplementor: WalletCardClickIntentsImplementor,
    private val warningsClickIntentsImplementer: WalletWarningsClickIntentsImplementer,
    private val currencyActionsClickIntentsImplementor: WalletCurrencyActionsClickIntentsImplementor,
    private val contentClickIntentsImplementor: WalletContentClickIntentsImplementor,
    private val stateHolder: WalletStateHolderV2,
// [REDACTED_TODO_COMMENT]
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val selectWalletUseCase: SelectWalletUseCase,
// [REDACTED_TODO_COMMENT]
    private val fetchTokenListUseCase: FetchTokenListUseCase,
    private val fetchCardTokenListUseCase: FetchCardTokenListUseCase,
    private val fetchCurrencyStatusUseCase: FetchCurrencyStatusUseCase,
    private val neverToShowWalletsScrollPreview: NeverToShowWalletsScrollPreview,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val dispatchers: CoroutineDispatcherProvider,
) : BaseWalletClickIntents(),
    WalletCardClickIntents by walletCardClickIntentsImplementor,
    WalletWarningsClickIntents by warningsClickIntentsImplementer,
    WalletCurrencyActionsClickIntents by currencyActionsClickIntentsImplementor,
    WalletContentClickIntents by contentClickIntentsImplementor {

    override fun initialize(router: InnerWalletRouter, coroutineScope: CoroutineScope) {
        super.initialize(router, coroutineScope)

        walletCardClickIntentsImplementor.initialize(router, coroutineScope)
        warningsClickIntentsImplementer.initialize(router, coroutineScope)
        currencyActionsClickIntentsImplementor.initialize(router, coroutineScope)
        contentClickIntentsImplementor.initialize(router, coroutineScope)
    }

    fun onWalletChange(index: Int) {
        viewModelScope.launch(dispatchers.main) {
            launch(dispatchers.main) { neverToShowWalletsScrollPreview() }

            val maybeUserWallet = selectWalletUseCase(
                userWalletId = stateHolder.value.wallets[index].walletCardState.id,
            )

            stateHolder.update { it.copy(selectedWalletIndex = index) }

            maybeUserWallet.onRight {
// [REDACTED_TODO_COMMENT]
                //  walletScreenContentLoader.load(
                //     userWallet = it,
                //     appCurrency = getSelectedAppCurrencyUseCase.unwrap(),
                //     clickIntents = this@WalletClickIntentsV2,
                //     coroutineScope = viewModelScope,
                // )
            }
        }
    }

    fun onRefreshSwipe() {
        when (stateHolder.getSelectedWallet()) {
            is WalletState.MultiCurrency.Content -> {
                analyticsEventHandler.send(PortfolioEvent.Refreshed)
                refreshMultiCurrencyContent()
            }
            is WalletState.SingleCurrency.Content -> {
                analyticsEventHandler.send(PortfolioEvent.Refreshed)
                refreshSingleCurrencyContent()
            }
            is WalletState.MultiCurrency.Locked,
            is WalletState.SingleCurrency.Locked,
            -> Unit
        }
    }

    private fun refreshMultiCurrencyContent() {
        val userWallet = getSelectedWalletSyncUseCase.unwrap() ?: return

        stateHolder.update(
            SetRefreshStateTransformer(userWalletId = userWallet.walletId, isRefreshing = true),
        )

        viewModelScope.launch(dispatchers.main) {
            val maybeFetchResult = if (userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()) {
                fetchCardTokenListUseCase(userWalletId = userWallet.walletId, refresh = true)
            } else {
                fetchTokenListUseCase(userWalletId = userWallet.walletId, refresh = true)
            }

            maybeFetchResult.onLeft {
                stateHolder.update(SetTokenListErrorTransformer(userWalletId = userWallet.walletId, error = it))
            }

            stateHolder.update(
                SetRefreshStateTransformer(userWalletId = userWallet.walletId, isRefreshing = false),
            )
        }
    }

    fun onReloadClick() {
        refreshSingleCurrencyContent()
    }
// [REDACTED_TODO_COMMENT]
    //  currency. Now it not works because GetPrimaryCurrency's subscriber uses .distinctUntilChanged()
    private fun refreshSingleCurrencyContent() {
        val userWallet = getSelectedWalletSyncUseCase.unwrap() ?: return

        stateHolder.update(
            SetRefreshStateTransformer(userWalletId = userWallet.walletId, isRefreshing = true),
        )

        viewModelScope.launch(dispatchers.main) {
            fetchCurrencyStatusUseCase(userWallet.walletId, refresh = true)
// [REDACTED_TODO_COMMENT]
            //  walletScreenContentLoader.load(
            //     userWallet = userWallet,
            //     appCurrency = getSelectedAppCurrencyUseCase.unwrap(),
            //     clickIntents = this@WalletClickIntentsV2,
            //     coroutineScope = viewModelScope,
            //     isRefresh = true,
            // )

            stateHolder.update(
                SetRefreshStateTransformer(userWalletId = userWallet.walletId, isRefreshing = false),
            )
        }
    }
}
