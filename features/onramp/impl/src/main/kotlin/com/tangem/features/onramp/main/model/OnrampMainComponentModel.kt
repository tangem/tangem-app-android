package com.tangem.features.onramp.main.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.onramp.CheckOnrampAvailabilityUseCase
import com.tangem.features.onramp.main.OnrampMainComponent
import com.tangem.features.onramp.main.entity.OnrampMainBottomSheetConfig
import com.tangem.features.onramp.main.entity.OnrampMainComponentUM
import com.tangem.features.onramp.main.entity.transformer.OnrampAvailabilityTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.transformer.Transformer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class OnrampMainComponentModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val checkOnrampAvailabilityUseCase: CheckOnrampAvailabilityUseCase,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: OnrampMainComponent.Params = paramsContainer.require()
    private val _state: MutableStateFlow<OnrampMainComponentUM> = MutableStateFlow(
        value = OnrampMainComponentUM.InitialLoading(
            currency = params.currency,
            onClose = router::pop,
            openSettings = params.openSettings,
            onBuyClick = ::onBuyClick,
        ),
    )
    val state: StateFlow<OnrampMainComponentUM> get() = _state.asStateFlow()
    val bottomSheetNavigation: SlotNavigation<OnrampMainBottomSheetConfig> = SlotNavigation()

    init {
        checkResidenceCountry()
    }

    private fun checkResidenceCountry() {
        modelScope.launch {
            OnrampAvailabilityTransformer(
                maybeAvailabilityStatus = checkOnrampAvailabilityUseCase.invoke(),
                openBottomSheet = ::openBottomSheet,
            ).let(::updateState)
        }
    }

    @Suppress("EmptyFunctionBlock")
    private fun onBuyClick() {
    }

    private fun openBottomSheet(config: OnrampMainBottomSheetConfig) {
        bottomSheetNavigation.activate(config)
    }

    private fun updateState(transformer: Transformer<OnrampMainComponentUM>) {
        _state.update(transformer::transform)
    }
}
