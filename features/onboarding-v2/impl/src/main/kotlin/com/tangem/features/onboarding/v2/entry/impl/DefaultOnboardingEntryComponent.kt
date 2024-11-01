package com.tangem.features.onboarding.v2.entry.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.jetpack.subscribeAsState
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerNavigationHolder
import com.tangem.features.onboarding.v2.entry.OnboardingEntryComponent
import com.tangem.features.onboarding.v2.entry.impl.model.OnboardingEntryModel
import com.tangem.features.onboarding.v2.entry.impl.routing.OnboardingChildFactory
import com.tangem.features.onboarding.v2.entry.impl.routing.OnboardingRoute
import com.tangem.features.onboarding.v2.entry.impl.ui.OnboardingEntry
import com.tangem.features.onboarding.v2.stepper.api.OnboardingStepperComponent
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class DefaultOnboardingEntryComponent @AssistedInject constructor(
    @Assisted val context: AppComponentContext,
    @Assisted val params: OnboardingEntryComponent.Params,
    stepperFactory: OnboardingStepperComponent.Factory,
    onboardingChildFactory: OnboardingChildFactory,
) : OnboardingEntryComponent, AppComponentContext by context {

    private val innerNavigationLinkJobHolder = JobHolder()
    private val model: OnboardingEntryModel = getOrCreateModel(params)

    private val stepperComponent = stepperFactory.create(
        context = child("stepper"),
        params = OnboardingStepperComponent.Params(
            scanResponse = params.scanResponse,
            initState = OnboardingStepperComponent.StepperState(
                currentStep = 0,
                steps = 0,
                title = model.titleProvider.currentTitle.value,
                showProgress = false,
            ),
            popBack = {
                popInternal { success ->
                    if (success.not()) {
                        stackNavigation.pop()
                    }
                }
            },
        ),
    )

    private val stackNavigation = StackNavigation<OnboardingRoute>()

    private val innerStack: Value<ChildStack<OnboardingRoute, Any>> = childStack(
        key = "innerStack",
        source = stackNavigation,
        serializer = null,
        initialConfiguration = model.state.value.currentRoute,
        handleBackButton = true,
        childFactory = { configuration, factoryContext ->
            onboardingChildFactory.createChild(
                route = configuration,
                childContext = childByContext(factoryContext),
            )
        },
    )

    init {
        linkToInnerNavigation()
    }

    private fun popInternal(onComplete: (Boolean) -> Unit) {
        val activeChild = innerStack.value.active.instance
        if (activeChild is InnerNavigationHolder) {
            activeChild.innerNavigation.pop(onComplete)
        } else {
            onComplete(false)
        }
    }

    private fun linkToInnerNavigation() {
        innerStack.observe { stack ->
            val activeChild = stack.active.instance
            if (activeChild is InnerNavigationHolder) {
                componentScope.launch {
                    activeChild.innerNavigation.state.collect { state ->
                        stepperComponent.state.update {
                            it.copy(
                                currentStep = state.stackSize,
                                steps = state.stackMaxSize ?: 0,
                                title = model.titleProvider.currentTitle.value,
                                showProgress = state.stackMaxSize != null,
                            )
                        }
                    }
                }.saveIn(innerNavigationLinkJobHolder)
            }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val innerStackState by innerStack.subscribeAsState()

        OnboardingEntry(
            modifier = modifier,
            stepperContent = { modifierParam ->
                stepperComponent.Content(modifierParam)
            },
            childStack = innerStackState,
        )
    }

    @AssistedFactory
    interface Factory : OnboardingEntryComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: OnboardingEntryComponent.Params,
        ): DefaultOnboardingEntryComponent
    }
}
