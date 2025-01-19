package com.tangem.features.onboarding.v2.visa.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.jetpack.stack.Children
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.jetpack.subscribeAsState
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.getOrCreateSimple
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerNavigation
import com.tangem.core.decompose.navigation.inner.InnerNavigationState
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.onboarding.v2.visa.api.OnboardingVisaComponent
import com.tangem.features.onboarding.v2.visa.impl.child.accesscode.OnboardingVisaAccessCodeComponent
import com.tangem.features.onboarding.v2.visa.impl.child.approve.OnboardingVisaApproveComponent
import com.tangem.features.onboarding.v2.visa.impl.child.choosewallet.OnboardingVisaChooseWalletComponent
import com.tangem.features.onboarding.v2.visa.impl.child.inprogress.OnboardingVisaInProgressComponent
import com.tangem.features.onboarding.v2.visa.impl.child.otherwallet.OnboardingVisaOtherWalletComponent
import com.tangem.features.onboarding.v2.visa.impl.child.pincode.OnboardingVisaPinCodeComponent
import com.tangem.features.onboarding.v2.visa.impl.child.welcome.OnboardingVisaWelcomeComponent
import com.tangem.features.onboarding.v2.visa.impl.model.OnboardingVisaModel
import com.tangem.features.onboarding.v2.visa.impl.route.OnboardingVisaRoute
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Suppress("UnusedPrivateMember")
internal class DefaultOnboardingVisaComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: OnboardingVisaComponent.Params,
) : OnboardingVisaComponent, AppComponentContext by appComponentContext {

    private val model: OnboardingVisaModel = getOrCreateModel(params)

    private val innerNavigationState = instanceKeeper.getOrCreateSimple(key = "innerNavigationState") {
        MutableStateFlow(
            OnboardingVisaInnerNavigationState(
                stackSize = 1,
                stackMaxSize = 7,
            ),
        )
    }

    private val currentChildBackEventHandle = instanceKeeper.getOrCreateSimple(key = "currentChildBackEventHandle") {
        MutableSharedFlow<Unit>()
    }

    private val stackNavigation = StackNavigation<OnboardingVisaRoute>()

    private val childStack: Value<ChildStack<OnboardingVisaRoute, ComposableContentComponent>> =
        childStack(
            key = "innerStack",
            source = stackNavigation,
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
        override val state: StateFlow<InnerNavigationState> = innerNavigationState

        override fun pop(onComplete: (Boolean) -> Unit) {
            if (childStack.value.active.configuration is OnboardingVisaRoute.AccessCode) {
                componentScope.launch { currentChildBackEventHandle.emit(Unit) }
            } else {
                stackNavigation.pop()
            }
        }
    }

    private fun createChild(
        route: OnboardingVisaRoute,
        factoryContext: AppComponentContext,
    ): ComposableContentComponent {
        return when (route) {
            is OnboardingVisaRoute.Welcome -> OnboardingVisaWelcomeComponent(
                appComponentContext = factoryContext,
                params = OnboardingVisaWelcomeComponent.Params(
                    isWelcomeBack = route.isWelcomeBack,
                    onBack = ::onChildBack,
                    onDone = { stackNavigation.push(OnboardingVisaRoute.AccessCode) },
                ),
            )
            OnboardingVisaRoute.AccessCode -> OnboardingVisaAccessCodeComponent(
                appComponentContext = factoryContext,
                params = OnboardingVisaAccessCodeComponent.Params(
                    onBack = ::onChildBack,
                    parentBackEvent = currentChildBackEventHandle,
                    onDone = { stackNavigation.push(OnboardingVisaRoute.ChooseWallet) },
                ),
            )
            OnboardingVisaRoute.ChooseWallet -> OnboardingVisaChooseWalletComponent(
                appComponentContext = factoryContext,
                params = OnboardingVisaChooseWalletComponent.Params(
                    onBack = ::onChildBack,
                    onEvent = { event ->
                        stackNavigation.push(
                            when (event) {
                                OnboardingVisaChooseWalletComponent.Params.Event.TangemWallet ->
                                    OnboardingVisaRoute.TangemWalletApproveOption
                                OnboardingVisaChooseWalletComponent.Params.Event.OtherWallet ->
                                    OnboardingVisaRoute.OtherWalletApproveOption
                            },
                        )
                    },
                ),
            )
            OnboardingVisaRoute.InProgress -> OnboardingVisaInProgressComponent(
                appComponentContext = factoryContext,
                params = OnboardingVisaInProgressComponent.Params(
                    onBack = ::onChildBack,
                    onDone = { stackNavigation.push(OnboardingVisaRoute.PinCode) },
                ),
            )
            OnboardingVisaRoute.OtherWalletApproveOption -> OnboardingVisaOtherWalletComponent(
                appComponentContext = factoryContext,
                params = OnboardingVisaOtherWalletComponent.Params(
                    onBack = ::onChildBack,
                    onDone = { stackNavigation.push(OnboardingVisaRoute.InProgress) },
                ),
            )
            OnboardingVisaRoute.PinCode -> OnboardingVisaPinCodeComponent(
                appComponentContext = factoryContext,
                params = OnboardingVisaPinCodeComponent.Params(
                    onBack = ::onChildBack,
                    onDone = { params.onDone() },
                ),
            )
            OnboardingVisaRoute.TangemWalletApproveOption -> OnboardingVisaApproveComponent(
                appComponentContext = factoryContext,
                params = OnboardingVisaApproveComponent.Params(
                    onBack = ::onChildBack,
                    onDone = { stackNavigation.push(OnboardingVisaRoute.InProgress) },
                ),
            )
        }
    }

    private fun onChildBack() {
        when (childStack.value.active.configuration) {
            OnboardingVisaRoute.AccessCode,
            OnboardingVisaRoute.TangemWalletApproveOption,
            OnboardingVisaRoute.OtherWalletApproveOption,
            -> stackNavigation.pop()
            is OnboardingVisaRoute.Welcome,
            OnboardingVisaRoute.ChooseWallet,
            OnboardingVisaRoute.InProgress,
            OnboardingVisaRoute.OtherWalletApproveOption,
            OnboardingVisaRoute.PinCode,
            -> {
            }
        }
    }

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
    interface Factory : OnboardingVisaComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: OnboardingVisaComponent.Params,
        ): DefaultOnboardingVisaComponent
    }
}

data class OnboardingVisaInnerNavigationState(
    override val stackSize: Int,
    override val stackMaxSize: Int?,
) : InnerNavigationState
