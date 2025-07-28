package com.tangem.features.hotwallet.stepper.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.hotwallet.stepper.api.HotWalletStepperComponent
import com.tangem.features.hotwallet.stepper.impl.ui.HotWalletStepper
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultHotWalletStepperComponent @AssistedInject constructor(
    @Assisted val context: AppComponentContext,
    @Assisted val params: HotWalletStepperComponent.Params,
) : HotWalletStepperComponent, AppComponentContext by context {

    private val model: HotWalletStepperModel = getOrCreateModel(params)

    override val state = model.uiState

    fun updateState(newState: HotWalletStepperComponent.StepperUM) {
        model.updateState(newState)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val uiState by state.collectAsStateWithLifecycle()

        HotWalletStepper(
            state = uiState,
            modifier = modifier,
            onBackClick = model::onBackClick,
            onSkipClick = model::onSkipClick,
            onFeedbackClick = model::onFeedbackClick,
        )
    }

    @AssistedFactory
    interface Factory : HotWalletStepperComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: HotWalletStepperComponent.Params,
        ): DefaultHotWalletStepperComponent
    }
}