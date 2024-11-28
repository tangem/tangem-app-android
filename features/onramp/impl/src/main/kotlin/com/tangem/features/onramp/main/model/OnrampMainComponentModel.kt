package com.tangem.features.onramp.main.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.onramp.*
import com.tangem.domain.onramp.model.OnrampAvailability
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.OnrampProviderWithQuote
import com.tangem.features.onramp.main.OnrampMainComponent
import com.tangem.features.onramp.main.entity.OnrampIntents
import com.tangem.features.onramp.main.entity.OnrampMainBottomSheetConfig
import com.tangem.features.onramp.main.entity.OnrampMainComponentUM
import com.tangem.features.onramp.main.entity.OnrampProviderBlockUM
import com.tangem.features.onramp.main.entity.factory.OnrampStateFactory
import com.tangem.features.onramp.main.entity.factory.amount.OnrampAmountStateFactory
import com.tangem.features.onramp.utils.InputManager
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
internal class OnrampMainComponentModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val checkOnrampAvailabilityUseCase: CheckOnrampAvailabilityUseCase,
    private val getOnrampCurrencyUseCase: GetOnrampCurrencyUseCase,
    private val clearOnrampCacheUseCase: ClearOnrampCacheUseCase,
    private val fetchQuotesUseCase: OnrampFetchQuotesUseCase,
    private val getOnrampQuotesUseCase: GetOnrampQuotesUseCase,
    private val amountInputManager: InputManager,
    paramsContainer: ParamsContainer,
) : Model(), OnrampIntents {

    private val params: OnrampMainComponent.Params = paramsContainer.require()
    private val stateFactory = OnrampStateFactory(
        currentStateProvider = Provider { _state.value },
        cryptoCurrency = params.cryptoCurrency,
        onrampIntents = this,
    )
    private val amountStateFactory = OnrampAmountStateFactory(
        currentStateProvider = Provider { _state.value },
        onrampIntents = this,
    )

    private val _state: MutableStateFlow<OnrampMainComponentUM> = MutableStateFlow(
        value = stateFactory.getInitialState(
            currency = params.cryptoCurrency.name,
            onClose = router::pop,
        ),
    )
    val state: StateFlow<OnrampMainComponentUM> get() = _state.asStateFlow()
    val bottomSheetNavigation: SlotNavigation<OnrampMainBottomSheetConfig> = SlotNavigation()

    init {
        checkResidenceCountry()
        subscribeToAmountChanges()
    }

    fun onProviderSelected(providerWithQuote: OnrampProviderWithQuote.Data) {
        _state.update { amountStateFactory.getAmountSecondaryUpdatedState(providerWithQuote) }
    }

    private fun checkResidenceCountry() {
        modelScope.launch {
            checkOnrampAvailabilityUseCase.invoke(params.cryptoCurrency)
                .onRight(::handleOnrampAvailability)
                .onLeft { Timber.e(it) }
        }
    }

    private fun handleOnrampAvailability(availability: OnrampAvailability) {
        when (availability) {
            is OnrampAvailability.Available -> handleOnrampAvailable(availability.currency)
            is OnrampAvailability.ConfirmResidency,
            is OnrampAvailability.NotSupported,
            -> bottomSheetNavigation.activate(OnrampMainBottomSheetConfig.ConfirmResidency(availability.country))
        }
    }

    private fun handleOnrampAvailable(currency: OnrampCurrency) {
        _state.update { stateFactory.getReadyState(currency) }
        subscribeToCurrencyUpdates()
        subscribeToQuotesUpdate()
    }

    private fun subscribeToCurrencyUpdates() {
        getOnrampCurrencyUseCase.invoke()
            .onEach { maybeCurrency ->
                val currency = maybeCurrency.getOrNull()
                if (currency != null) {
                    _state.update { amountStateFactory.getUpdatedCurrencyState(currency) }
                }
            }
            .launchIn(modelScope)
    }

    private fun subscribeToAmountChanges() = modelScope.launch {
        amountInputManager.query
            .filter(String::isNotEmpty)
            .collectLatest { _ ->
                val content = state.value as? OnrampMainComponentUM.Content ?: return@collectLatest
                _state.update { amountStateFactory.getAmountSecondaryLoadingState() }
                fetchQuotesUseCase.invoke(
                    amount = content.amountBlockState.amountFieldModel.fiatAmount,
                    cryptoCurrency = params.cryptoCurrency,
                )
            }
    }

    private fun subscribeToQuotesUpdate() {
        getOnrampQuotesUseCase.invoke()
            .onEach { maybeQuotes ->
                val quote = maybeQuotes.getOrNull()?.firstOrNull() ?: return@onEach
                _state.update { amountStateFactory.getAmountSecondaryUpdatedState(quote) }
            }
            .launchIn(modelScope)
    }

    override fun onAmountValueChanged(value: String) {
        _state.update { amountStateFactory.getOnAmountValueChange(value) }
        modelScope.launch { amountInputManager.update(value) }
    }

    override fun openSettings() {
        params.openSettings()
    }

    override fun onBuyClick(quote: OnrampProviderWithQuote.Data) {
        params.openRedirectPage(quote)
    }

    override fun openCurrenciesList() {
        bottomSheetNavigation.activate(OnrampMainBottomSheetConfig.CurrenciesList)
    }

    override fun openProviders() {
        val providerState = (state.value as? OnrampMainComponentUM.Content)?.providerBlockState ?: return
        val providerContentState = providerState as? OnrampProviderBlockUM.Content ?: return
        bottomSheetNavigation.activate(
            OnrampMainBottomSheetConfig.ProvidersList(
                selectedPaymentMethod = providerContentState.paymentMethod,
            ),
        )
    }

    override fun onDestroy() {
        modelScope.launch { clearOnrampCacheUseCase.invoke() }
        super.onDestroy()
    }
}