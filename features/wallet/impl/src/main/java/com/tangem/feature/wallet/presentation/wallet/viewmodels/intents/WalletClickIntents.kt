package com.tangem.feature.wallet.presentation.wallet.viewmodels.intents

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.extenstions.unwrap
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
import com.tangem.feature.wallet.presentation.wallet.loaders.WalletScreenContentLoader
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetRefreshStateTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTokenListErrorTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ViewModelScoped
internal class WalletClickIntents @Inject constructor(
    private val walletCardClickIntentsImplementor: WalletCardClickIntentsImplementor,
    private val warningsClickIntentsImplementer: WalletWarningsClickIntentsImplementor,
    private val currencyActionsClickIntentsImplementor: WalletCurrencyActionsClickIntentsImplementor,
    private val contentClickIntentsImplementor: WalletContentClickIntentsImplementor,
    private val visaWalletIntentsImplementor: VisaWalletIntentsImplementor,
    private val pushPermissionClickIntentsImplementor: WalletPushPermissionClickIntentsImplementor,
    private val stateHolder: WalletStateController,
    private val walletScreenContentLoader: WalletScreenContentLoader,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val selectWalletUseCase: SelectWalletUseCase,
    private val fetchTokenListUseCase: FetchTokenListUseCase,
    private val fetchCardTokenListUseCase: FetchCardTokenListUseCase,
    private val fetchCurrencyStatusUseCase: FetchCurrencyStatusUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val neverToShowWalletsScrollPreview: NeverToShowWalletsScrollPreview,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val dispatchers: CoroutineDispatcherProvider,
) : BaseWalletClickIntents(),
    WalletCardClickIntents by walletCardClickIntentsImplementor,
    WalletWarningsClickIntents by warningsClickIntentsImplementer,
    WalletCurrencyActionsClickIntents by currencyActionsClickIntentsImplementor,
    WalletContentClickIntents by contentClickIntentsImplementor,
    VisaWalletIntents by visaWalletIntentsImplementor,
    WalletPushPermissionClickIntents by pushPermissionClickIntentsImplementor {

    override fun initialize(router: InnerWalletRouter, coroutineScope: CoroutineScope) {
        super.initialize(router, coroutineScope)

        walletCardClickIntentsImplementor.initialize(router, coroutineScope)
        warningsClickIntentsImplementer.initialize(router, coroutineScope)
        currencyActionsClickIntentsImplementor.initialize(router, coroutineScope)
        contentClickIntentsImplementor.initialize(router, coroutineScope)
        visaWalletIntentsImplementor.initialize(router, coroutineScope)
        pushPermissionClickIntentsImplementor.initialize(router, coroutineScope)
    }

    fun onWalletChange(index: Int) {
        viewModelScope.launch(dispatchers.main) {
            launch(dispatchers.main) { neverToShowWalletsScrollPreview() }

            val maybeUserWallet = selectWalletUseCase(
                userWalletId = stateHolder.value.wallets[index].walletCardState.id,
            )

            stateHolder.update { it.copy(selectedWalletIndex = index) }

            maybeUserWallet.onRight {
                walletScreenContentLoader.load(
                    userWallet = it,
                    clickIntents = this@WalletClickIntents,
                    coroutineScope = viewModelScope,
                )
            }
        }
    }

    fun onRefreshSwipe(showRefreshState: Boolean) {
        when (stateHolder.getSelectedWallet()) {
            is WalletState.MultiCurrency.Content -> {
                analyticsEventHandler.send(PortfolioEvent.Refreshed)
                refreshMultiCurrencyContent(showRefreshState)
            }
            is WalletState.SingleCurrency.Content,
            is WalletState.Visa.Content,
            -> {
                analyticsEventHandler.send(PortfolioEvent.Refreshed)
                refreshSingleCurrencyContent(showRefreshState)
            }
            is WalletState.MultiCurrency.Locked,
            is WalletState.SingleCurrency.Locked,
            is WalletState.Visa.Locked,
            -> Unit
        }
    }

    fun onReloadClick() {
        refreshSingleCurrencyContent(showRefreshState = true)
    }

    private fun refreshMultiCurrencyContent(showRefreshState: Boolean) {
        val userWallet = getSelectedWalletSyncUseCase.unwrap() ?: return

        stateHolder.update(
            SetRefreshStateTransformer(userWalletId = userWallet.walletId, isRefreshing = showRefreshState),
        )

        viewModelScope.launch(dispatchers.main) {
            val maybeFetchResult = if (userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()) {
                fetchCardTokenListUseCase(userWalletId = userWallet.walletId, refresh = true)
            } else {
                fetchTokenListUseCase(userWalletId = userWallet.walletId, refresh = true)
            }

            maybeFetchResult.onLeft {
                stateHolder.update(
                    SetTokenListErrorTransformer(
                        selectedWallet = userWallet,
                        error = it,
                        appCurrency = getSelectedAppCurrencyUseCase.unwrap(),
                    ),
                )
            }

            stateHolder.update(
                SetRefreshStateTransformer(userWalletId = userWallet.walletId, isRefreshing = false),
            )
        }
    }
// [REDACTED_TODO_COMMENT]
    //  currency. Now it not works because GetPrimaryCurrency's subscriber uses .distinctUntilChanged()
    private fun refreshSingleCurrencyContent(showRefreshState: Boolean) {
        val userWallet = getSelectedWalletSyncUseCase.unwrap() ?: return

        stateHolder.update(
            SetRefreshStateTransformer(userWalletId = userWallet.walletId, isRefreshing = showRefreshState),
        )

        viewModelScope.launch(dispatchers.main) {
            fetchCurrencyStatusUseCase(userWallet.walletId, refresh = true)

            walletScreenContentLoader.load(
                userWallet = userWallet,
                clickIntents = this@WalletClickIntents,
                isRefresh = true,
                coroutineScope = viewModelScope,
            )

            stateHolder.update(
                SetRefreshStateTransformer(userWalletId = userWallet.walletId, isRefreshing = false),
            )
        }
    }
}
