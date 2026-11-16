package com.tangem.features.survey.impl.nativesurvey.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.essenty.backhandler.BackCallback
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.survey.NativeSurveyComponent
import com.tangem.features.survey.impl.nativesurvey.screen.model.NativeSurveyModel
import com.tangem.features.survey.impl.nativesurvey.screen.ui.NativeSurveyScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultNativeSurveyComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: NativeSurveyComponent.Params,
) : NativeSurveyComponent, AppComponentContext by appComponentContext {

    private val model: NativeSurveyModel = getOrCreateModel(params = params)

    init {
        backHandler.register(BackCallback(onBack = model::onSystemBack))
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        NativeSurveyScreen(state = state, modifier = modifier)
    }

    @AssistedFactory
    interface Factory : NativeSurveyComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: NativeSurveyComponent.Params,
        ): DefaultNativeSurveyComponent
    }
}