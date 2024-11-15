package com.tangem.features.onramp.main.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.onramp.CheckOnrampAvailabilityUseCase
import com.tangem.domain.onramp.GetOnrampCurrencyUseCase
import com.tangem.domain.onramp.model.OnrampAvailability
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.features.onramp.main.OnrampMainComponent
import com.tangem.features.onramp.main.entity.OnrampIntents
import com.tangem.features.onramp.main.entity.OnrampMainBottomSheetConfig
import com.tangem.features.onramp.main.entity.OnrampMainComponentUM
import com.tangem.features.onramp.main.entity.factory.OnrampStateFactory
import com.tangem.features.onramp.main.entity.factory.amount.OnrampAmountStateFactory
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

internal class OnrampMainComponentModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val checkOnrampAvailabilityUseCase: CheckOnrampAvailabilityUseCase,
    private val getOnrampCurrencyUseCase: GetOnrampCurrencyUseCase,
    paramsContainer: ParamsContainer,
) : Model(), OnrampIntents {

    private val params: OnrampMainComponent.Params = paramsContainer.require()
    private val stateFactory = OnrampStateFactory(
        currentStateProvider = Provider { _state.value },
        cryptoCurrency = params.cryptoCurrency,
        onrampIntents = this,
    )
    private val amountStateFactory = OnrampAmountStateFactory(currentStateProvider = Provider { _state.value })

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
    }

    private fun checkResidenceCountry() {
        modelScope.launch {
            checkOnrampAvailabilityUseCase.invoke()
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

    override fun onAmountValueChanged(value: String) {
        _state.update { amountStateFactory.getOnAmountValueChange(value) }
    }

    override fun openSettings() {
        params.openSettings()
    }

    override fun onBuyClick() {
        TODO("Not yet implemented")
    }

    override fun openCurrenciesList() {
        bottomSheetNavigation.activate(OnrampMainBottomSheetConfig.CurrenciesList)
    }
}