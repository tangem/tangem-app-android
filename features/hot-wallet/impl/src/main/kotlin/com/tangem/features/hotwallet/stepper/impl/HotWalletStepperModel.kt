package com.tangem.features.hotwallet.stepper.impl

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.hotwallet.stepper.api.HotWalletStepperComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Stable
@ModelScoped
internal class HotWalletStepperModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    val params = paramsContainer.require<HotWalletStepperComponent.Params>()

    val uiState: StateFlow<HotWalletStepperComponent.StepperUM>
    field = MutableStateFlow(params.initState)

    fun updateState(newState: HotWalletStepperComponent.StepperUM) {
        uiState.value = newState
    }

    fun onBackClick() {
        params.callback.onBackClick()
    }

    fun onSkipClick() {
        // TODO send analytics
        params.callback.onSkipClick()
    }

    fun onFeedbackClick() {
        // TODO send analytics
        // openFeedback()
    }
}