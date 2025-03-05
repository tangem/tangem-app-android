package com.tangem.features.onramp.settings.model

import arrow.core.Either
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.onramp.GetOnrampCountryUseCase
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.features.onramp.settings.OnrampSettingsComponent
import com.tangem.features.onramp.settings.entity.OnrampSettingsConfig
import com.tangem.features.onramp.settings.entity.OnrampSettingsItemUM
import com.tangem.features.onramp.settings.entity.OnrampSettingsUM
import com.tangem.features.onramp.utils.sendOnrampErrorEvent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@ModelScoped
internal class OnrampSettingsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val getOnrampCountryUseCase: GetOnrampCountryUseCase,
    paramsContainer: ParamsContainer,
) : Model() {

    val state: StateFlow<OnrampSettingsUM> get() = _state
    val bottomSheetNavigation: SlotNavigation<OnrampSettingsConfig> = SlotNavigation()

    private val params: OnrampSettingsComponent.Params = paramsContainer.require()
    private val _state: MutableStateFlow<OnrampSettingsUM> = MutableStateFlow(getInitialState())

    init {
        analyticsEventHandler.send(OnrampAnalyticsEvent.SettingsOpened)
        subscribeOnUpdateState()
    }

    private fun subscribeOnUpdateState() {
        getOnrampCountryUseCase.invoke()
            .onEach(::updateResidenceState)
            .launchIn(modelScope)
    }

    private fun updateResidenceState(maybeCountry: Either<OnrampError, OnrampCountry?>) {
        maybeCountry.fold(
            ifLeft = {
                analyticsEventHandler.sendOnrampErrorEvent(it, params.cryptoCurrency.symbol)
            },
            ifRight = { country ->
                _state.update { state ->
                    state.copy(
                        items = listOf(
                            OnrampSettingsItemUM.Residence(
                                countryName = country?.name.orEmpty(),
                                flagUrl = country?.image.orEmpty(),
                                onClick = ::openSelectCountryBottomSheet,
                            ),
                        ).toImmutableList(),
                    )
                }
            },
        )
    }

    private fun openSelectCountryBottomSheet() {
        bottomSheetNavigation.activate(OnrampSettingsConfig.SelectCountry)
    }

    private fun getInitialState(): OnrampSettingsUM {
        return OnrampSettingsUM(
            onBack = params.onBack,
            items = listOf(OnrampSettingsItemUM.Residence(onClick = ::openSelectCountryBottomSheet)).toImmutableList(),
        )
    }
}