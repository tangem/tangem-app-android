package com.tangem.features.onramp.alloffers.model

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.onramp.GetOnrampAllOffersUseCase
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.OnrampProviderWithQuote
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.features.onramp.alloffers.AllOffersComponent
import com.tangem.features.onramp.alloffers.entity.AllOffersIntents
import com.tangem.features.onramp.alloffers.entity.AllOffersStateFactory
import com.tangem.features.onramp.alloffers.entity.AllOffersStateUM
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

internal class AllOffersModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val getOnrampAllOffersUseCase: GetOnrampAllOffersUseCase,
    paramsContainer: ParamsContainer,
) : Model(), AllOffersIntents {

    private var quotesJob: Job? = null

    private val stateFactory: AllOffersStateFactory by lazy(LazyThreadSafetyMode.NONE) {
        AllOffersStateFactory(
            currentStateProvider = Provider { state.value },
            allOffersIntents = this,
        )
    }

    private val params: AllOffersComponent.Params = paramsContainer.require()

    private val _state: MutableStateFlow<AllOffersStateUM> = MutableStateFlow(AllOffersStateUM.Loading)
    val state: StateFlow<AllOffersStateUM> = _state.asStateFlow()

    init {
        subscribeOnAllOffers()
    }

    fun dismiss() {
        params.onDismiss()
    }

    override fun onPaymentMethodClicked(paymentMethodId: String) {
        val contentState = state.value as? AllOffersStateUM.Content ?: return
        val method = contentState.methods.firstOrNull { it.methodConfig.method.id == paymentMethodId } ?: return
        _state.update { contentState.copy(currentMethod = method) }
    }

    override fun onBuyClick(quote: OnrampProviderWithQuote.Data) {
        analyticsEventHandler.send(
            OnrampAnalyticsEvent.OnBuyClick(
                providerName = quote.provider.info.name,
                currency = params.amountCurrencyCode,
                tokenSymbol = params.cryptoCurrency.symbol,
            ),
        )
        params.openRedirectPage(quote)
    }

    override fun onBackClicked() {
        _state.update { stateFactory.getPaymentsState() }
    }

    override fun onRefresh() {
        subscribeOnAllOffers()
    }

    private fun subscribeOnAllOffers() {
        quotesJob?.cancel()
        quotesJob = modelScope.launch(dispatchers.default) {
            getOnrampAllOffersUseCase.invoke(
                userWalletId = params.userWallet.walletId,
                cryptoCurrencyId = params.cryptoCurrency.id,
            ).collectLatest { maybeOffers ->
                maybeOffers.fold(
                    ifLeft = ::handleOnrampError,
                    ifRight = { offersGroup ->
                        _state.update { stateFactory.getLoadedPaymentsState(offersGroup) }
                    },
                )
            }
        }
    }

    private fun handleOnrampError(onrampError: OnrampError) {
        Timber.e(onrampError.toString())
        _state.update { stateFactory.getOnrampErrorState(onrampError) }
    }
}