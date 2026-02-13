package com.tangem.features.onboarding.usedcard.entry

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.ObserveLifecycleMode
import com.arkivanov.decompose.value.subscribe
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.onboarding.usedcard.UsedCardOnboardingComponent
import com.tangem.features.onboarding.usedcard.routing.UsedCardOnboardingChildFactory
import com.tangem.features.onboarding.usedcard.stepper.UsedCardOnboardingStepperStateManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

internal class DefaultUsedCardOnboardingComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: UsedCardOnboardingComponent.Params,
    private val stepperStateManager: UsedCardOnboardingStepperStateManager,
    childFactory: UsedCardOnboardingChildFactory,
) : UsedCardOnboardingComponent, AppComponentContext by appComponentContext {

    private val model: UsedCardOnboardingModel = getOrCreateModel(params)

    private val innerStack = childStack(
        key = "usedCardOnboardingInnerStack",
        source = model.stackNavigation,
        serializer = null,
        initialConfiguration = model.startRoute,
        handleBackButton = true,
        childFactory = { configuration, factoryContext ->
            childFactory.createChild(
                route = configuration,
                childContext = childByContext(factoryContext),
                model = model,
            )
        },
    )

    init {
        innerStack.subscribe(
            lifecycle = lifecycle,
            mode = ObserveLifecycleMode.CREATE_DESTROY,
        ) { stack ->
            componentScope.launch {
                model.currentRoute.emit(stack.active.configuration)
            }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val stackState by innerStack.subscribeAsState()
        val currentRoute = stackState.active.configuration

        BackHandler(onBack = model::onBackClick)

        UsedCardOnboardingContent(
            stackState = stackState,
            stepperState = stepperStateManager.getStepperState(currentRoute),
            onBackClick = model::onBackClick,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : UsedCardOnboardingComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: UsedCardOnboardingComponent.Params,
        ): DefaultUsedCardOnboardingComponent
    }
}