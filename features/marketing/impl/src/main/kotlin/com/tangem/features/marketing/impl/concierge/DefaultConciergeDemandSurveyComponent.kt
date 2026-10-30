package com.tangem.features.marketing.impl.concierge

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.childStack
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.res.TangemAnimations
import com.tangem.features.marketing.api.concierge.ConciergeDemandSurveyComponent
import com.tangem.features.marketing.impl.concierge.model.ConciergeDemandSurveyModel
import com.tangem.features.marketing.impl.concierge.model.ConciergeDemandSurveyRoute
import com.tangem.features.marketing.impl.concierge.ui.ConciergeDemandSurveyContent
import com.tangem.features.marketing.impl.concierge.ui.ConciergeDemandSurveyModal
import com.tangem.features.marketing.impl.concierge.ui.ConciergeDemandSurveySuccessContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultConciergeDemandSurveyComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: ConciergeDemandSurveyComponent.Params,
) : ConciergeDemandSurveyComponent, AppComponentContext by appComponentContext {

    private val model: ConciergeDemandSurveyModel = getOrCreateModel(params = params)

    private val innerStack = childStack(
        key = "conciergeDemandSurveyStack",
        source = model.stackNavigation,
        serializer = ConciergeDemandSurveyRoute.serializer(),
        initialConfiguration = ConciergeDemandSurveyRoute.Survey,
        handleBackButton = false,
        childFactory = { route, _ -> createStep(route) },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val stack by innerStack.subscribeAsState()
        val state by model.uiState.collectAsStateWithLifecycle()

        ConciergeDemandSurveyModal(
            state = state,
            isSuccessStep = stack.active.configuration == ConciergeDemandSurveyRoute.Success,
        ) {
            AnimatedContent(
                targetState = stack.active,
                contentKey = { child -> child.configuration },
                modifier = Modifier.weight(weight = 1f, fill = false),
                label = "ConciergeDemandSurveyStep",
                transitionSpec = TangemAnimations.AnimatedContent.slide { from, to ->
                    from.configuration.ordinal < to.configuration.ordinal
                },
            ) { child ->
                child.instance.Content(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    private fun createStep(route: ConciergeDemandSurveyRoute): ComposableContentComponent = when (route) {
        ConciergeDemandSurveyRoute.Survey -> SurveyStep(model)
        ConciergeDemandSurveyRoute.Success -> SuccessStep(model)
    }

    private class SurveyStep(private val model: ConciergeDemandSurveyModel) : ComposableContentComponent {

        @Composable
        override fun Content(modifier: Modifier) {
            val state by model.uiState.collectAsStateWithLifecycle()
            ConciergeDemandSurveyContent(state = state, modifier = modifier)
        }
    }

    private class SuccessStep(private val model: ConciergeDemandSurveyModel) : ComposableContentComponent {

        @Composable
        override fun Content(modifier: Modifier) {
            val state by model.uiState.collectAsStateWithLifecycle()
            ConciergeDemandSurveySuccessContent(onDoneClick = state.onDismiss, modifier = modifier)
        }
    }

    @AssistedFactory
    interface Factory : ConciergeDemandSurveyComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: ConciergeDemandSurveyComponent.Params,
        ): DefaultConciergeDemandSurveyComponent
    }
}