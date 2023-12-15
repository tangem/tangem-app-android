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
import com.tangem.feature.wallet.presentation.wallet.loaders.WalletScreenContentLoader
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state2.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.SetRefreshStateTransformer
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.SetTokenListErrorTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ViewModelScoped
internal class WalletClickIntentsV2 @Inject constructor(
    private val walletCardClickIntentsImplementor: WalletCardClickIntentsImplementor,
    private val warningsClickIntentsImplementer: WalletWarningsClickIntentsImplementer,
    private val currencyActionsClickIntentsImplementor: WalletCurrencyActionsClickIntentsImplementor,
    private val contentClickIntentsImplementor: WalletContentClickIntentsImplementor,
    private val visaWalletIntentsImplementor: VisaWalletIntentsImplementor,
    private val stateHolder: WalletStateController,
    private val walletScreenContentLoader: WalletScreenContentLoader,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val selectWalletUseCase: SelectWalletUseCase,
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
    WalletContentClickIntents by contentClickIntentsImplementor,
    VisaWalletIntents by visaWalletIntentsImplementor {

    override fun initialize(router: InnerWalletRouter, coroutineScope: CoroutineScope) {
        super.initialize(router, coroutineScope)

        walletCardClickIntentsImplementor.initialize(router, coroutineScope)
        warningsClickIntentsImplementer.initialize(router, coroutineScope)
        currencyActionsClickIntentsImplementor.initialize(router, coroutineScope)
        contentClickIntentsImplementor.initialize(router, coroutineScope)
        visaWalletIntentsImplementor.initialize(router, coroutineScope)
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
                    clickIntents = this@WalletClickIntentsV2,
                    coroutineScope = viewModelScope,
                )
            }
        }
    }

    fun onRefreshSwipe() {
        when (stateHolder.getSelectedWallet()) {
            is WalletState.MultiCurrency.Content -> {
                analyticsEventHandler.send(PortfolioEvent.Refreshed)
                refreshMultiCurrencyContent()
            }
            is WalletState.SingleCurrency.Content,
            is WalletState.Visa.Content,
            -> {
                analyticsEventHandler.send(PortfolioEvent.Refreshed)
                refreshSingleCurrencyContent()
            }
            is WalletState.MultiCurrency.Locked,
            is WalletState.SingleCurrency.Locked,
            is WalletState.Visa.Locked,
            -> Unit
        }
    }

    fun onReloadClick() {
        refreshSingleCurrencyContent()
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
// [REDACTED_TODO_COMMENT]
    //  currency. Now it not works because GetPrimaryCurrency's subscriber uses .distinctUntilChanged()
    private fun refreshSingleCurrencyContent() {
        val userWallet = getSelectedWalletSyncUseCase.unwrap() ?: return

        stateHolder.update(
            SetRefreshStateTransformer(userWalletId = userWallet.walletId, isRefreshing = true),
        )

        viewModelScope.launch(dispatchers.main) {
            fetchCurrencyStatusUseCase(userWallet.walletId, refresh = true)

            walletScreenContentLoader.load(
                userWallet = userWallet,
                clickIntents = this@WalletClickIntentsV2,
                isRefresh = true,
                coroutineScope = viewModelScope,
            )

            stateHolder.update(
                SetRefreshStateTransformer(userWalletId = userWallet.walletId, isRefreshing = false),
            )
        }
    }
}
