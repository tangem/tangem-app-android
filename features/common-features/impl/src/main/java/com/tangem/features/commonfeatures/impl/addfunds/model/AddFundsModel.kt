package com.tangem.features.commonfeatures.impl.addfunds.model

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.markets.action.CryptoCurrencyData
import com.tangem.common.ui.markets.action.TokenActionsBSContentUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.account.status.usecase.GetCryptoCurrencyActionsUseCaseV2
import com.tangem.domain.models.account.AccountStatus
import com.tangem.features.commonfeatures.api.addfunds.AddFundsComponent
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenAnalyticsPayload
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridge
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenResult
import com.tangem.features.commonfeatures.impl.addfunds.analytics.AddFundsAnalyticsEvent
import com.tangem.features.commonfeatures.impl.addtoportfolio.TokenActionsComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class AddFundsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    chooseTokenBridgeFactory: ChooseTokenBridge.Factory,
    private val getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCaseV2,
    private val appRouter: AppRouter,
    private val analyticsEventHandler: AnalyticsEventHandler,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model(), TokenActionsComponent.Callbacks {

    private val params = paramsContainer.require<AddFundsComponent.Params>()

    val chooseTokenBridge: ChooseTokenBridge = chooseTokenBridgeFactory.create(
        modelScope = modelScope,
        settings = ChooseTokenBridge.Settings.AddFunds,
        analyticsPayload = setOf(
            ChooseTokenAnalyticsPayload.ScreensSources(SCREEN_SOURCE),
        ),
    )

    private val selectedToken = MutableStateFlow<ChooseTokenResult?>(null)

    val isTokenActionsShown: StateFlow<Boolean> = selectedToken
        .map { it != null }
        .stateIn(modelScope, SharingStarted.Eagerly, initialValue = false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val tokenActionsData: Flow<CryptoCurrencyData> = selectedToken
        .filterNotNull()
        .flatMapLatest { result ->
            val cryptoPortfolio = result.account as? AccountStatus.CryptoPortfolio
                ?: return@flatMapLatest emptyFlow()
            getCryptoCurrencyActionsUseCase(
                accountId = cryptoPortfolio.account.accountId,
                currency = result.currency.currency,
            ).map { actionsState ->
                CryptoCurrencyData(
                    userWallet = result.wallet,
                    status = actionsState.cryptoCurrencyStatus,
                    actions = actionsState.states,
                    isAccountMode = false,
                    account = cryptoPortfolio,
                )
            }
        }

    init {
        chooseTokenBridge.selectWalletTab(params.userWalletId)
        analyticsEventHandler.send(
            AddFundsAnalyticsEvent.MethodScreenOpened(source = AddFundsAnalyticsEvent.SOURCE_MAIN_SCREEN),
        )
        observeBridge()
    }

    override fun onBottomActionClick() {
        val result = selectedToken.value ?: return
        selectedToken.value = null
        analyticsEventHandler.send(AddFundsAnalyticsEvent.ButtonGoToToken())
        appRouter.replaceCurrent(
            AppRoute.CurrencyDetails(
                userWalletId = result.wallet.walletId,
                currency = result.currency.currency,
            ),
        )
    }

    override fun onQuickActionClick(action: TokenActionsBSContentUM.Action) {
        val event = when (action) {
            TokenActionsBSContentUM.Action.Buy -> AddFundsAnalyticsEvent.ButtonBuy()
            TokenActionsBSContentUM.Action.Exchange -> AddFundsAnalyticsEvent.ButtonSwap()
            TokenActionsBSContentUM.Action.Receive -> AddFundsAnalyticsEvent.ButtonReceive()
            else -> return
        }
        analyticsEventHandler.send(event)
    }

    fun onTokenActionsDismiss() {
        selectedToken.value = null
    }

    private fun observeBridge() {
        modelScope.launch {
            chooseTokenBridge.onCurrencyChosen.receiveAsFlow().collect { result ->
                selectedToken.value = result
            }
        }
        modelScope.launch {
            chooseTokenBridge.onClose.receiveAsFlow().collect {
                appRouter.pop()
            }
        }
    }

    private companion object {
        const val SCREEN_SOURCE = "AddFunds"
    }
}