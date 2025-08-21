package com.tangem.features.onboarding.v2.note.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.subscribe
import com.arkivanov.essenty.instancekeeper.getOrCreateSimple
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerNavigation
import com.tangem.core.decompose.navigation.inner.InnerNavigationState
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.onboarding.v2.done.api.OnboardingDoneComponent
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.note.api.OnboardingNoteComponent
import com.tangem.features.onboarding.v2.note.impl.child.create.OnboardingNoteCreateWalletComponent
import com.tangem.features.onboarding.v2.note.impl.model.OnboardingNoteModel
import com.tangem.features.onboarding.v2.note.impl.model.OnboardingNoteCommonState
import com.tangem.features.onboarding.v2.note.impl.route.ONBOARDING_NOTE_STEPS_COUNT
import com.tangem.features.onboarding.v2.note.impl.route.OnboardingNoteRoute
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

internal class DefaultOnboardingNoteComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted val params: OnboardingNoteComponent.Params,
    val onboardingDoneComponentFactory: OnboardingDoneComponent.Factory,
) : OnboardingNoteComponent, AppComponentContext by context {

    private val model: OnboardingNoteModel = getOrCreateModel(params)

    private val currentChildBackEventHandle = instanceKeeper.getOrCreateSimple(key = "currentChildBackEventHandle") {
        MutableSharedFlow<Unit>()
    }

    private val childParams = ChildParams(
        commonState = model.commonUiState,
        onBack = ::onChildBack,
        parentBackEvent = currentChildBackEventHandle,
    )

    private val childStack: Value<ChildStack<OnboardingNoteRoute, ComposableContentComponent>> =
        childStack(
            key = "innerStack",
            source = model.stackNavigation,
            serializer = null,
            initialConfiguration = model.initialRoute,
            handleBackButton = true,
            childFactory = { configuration, factoryContext ->
                createChild(
                    configuration,
                    childByContext(factoryContext),
                )
            },
        )

    override val innerNavigation: InnerNavigation = object : InnerNavigation {
        override val state: StateFlow<InnerNavigationState> = model.innerNavigationState

        override fun pop(onComplete: (Boolean) -> Unit) {
            model.onChildBack(
                currentRoute = childStack.value.active.configuration,
            )
        }
    }

    init {
        // sets title and stepper value
        childStack.subscribe(lifecycle) { stack ->
            val currentRoute = stack.active.configuration
            params.titleProvider.changeTitle(TextReference.Res(R.string.onboarding_title))
            model.updateStepForNewRoute(currentRoute)
        }
    }

    private fun createChild(
        route: OnboardingNoteRoute,
        factoryContext: AppComponentContext,
    ): ComposableContentComponent {
        return when (route) {
            OnboardingNoteRoute.CreateWallet -> OnboardingNoteCreateWalletComponent(
                appComponentContext = factoryContext,
                params = OnboardingNoteCreateWalletComponent.Params(
                    childParams = childParams,
                    onWalletCreated = { userWallet ->
                        model.onWalletCreated(userWallet)
                        model.stackNavigation.push(OnboardingNoteRoute.Done)
                    },
                ),
            )
            OnboardingNoteRoute.Done -> onboardingDoneComponentFactory.create(
                context = factoryContext,
                params = OnboardingDoneComponent.Params(
                    mode = OnboardingDoneComponent.Mode.WalletCreated,
                    onDone = { params.onDone() },
                ),
            )
        }
    }

    private fun onChildBack() = model.onChildBack(
        currentRoute = childStack.value.active.configuration,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val stackState by childStack.subscribeAsState()

        Children(
            stack = stackState,
            animation = stackAnimation(slide()),
        ) {
            it.instance.Content(modifier)
        }
    }

    @AssistedFactory
    interface Factory : OnboardingNoteComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: OnboardingNoteComponent.Params,
        ): DefaultOnboardingNoteComponent
    }

    data class ChildParams(
        val commonState: MutableStateFlow<OnboardingNoteCommonState>,
        val onBack: () -> Unit,
        val parentBackEvent: SharedFlow<Unit>,
    )
}

internal data class OnboardingNoteInnerNavigationState(
    override val stackSize: Int,
) : InnerNavigationState {
    override val stackMaxSize: Int = ONBOARDING_NOTE_STEPS_COUNT
}

internal const val ALL_STEPS_TOP_CONTAINER_WEIGHT = 0.48f