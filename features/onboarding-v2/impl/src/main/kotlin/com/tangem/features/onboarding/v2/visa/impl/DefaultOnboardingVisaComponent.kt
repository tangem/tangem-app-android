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
import com.arkivanov.decompose.value.observe
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
import com.tangem.features.onboarding.v2.visa.impl.route.ONBOARDING_VISA_STEPS_COUNT
import com.tangem.features.onboarding.v2.visa.impl.route.OnboardingVisaRoute
import com.tangem.features.onboarding.v2.visa.impl.route.screenTitle
import com.tangem.features.onboarding.v2.visa.impl.route.stepNum
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Suppress("UnusedPrivateMember")
internal class DefaultOnboardingVisaComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: OnboardingVisaComponent.Params,
) : OnboardingVisaComponent, AppComponentContext by appComponentContext {

    private val model: OnboardingVisaModel = getOrCreateModel(params)

    private val innerNavigationState = instanceKeeper.getOrCreateSimple(key = "innerNavigationState") {
        MutableStateFlow(OnboardingVisaInnerNavigationState(stackSize = model.initialStepNum))
    }

    private val currentChildBackEventHandle = instanceKeeper.getOrCreateSimple(key = "currentChildBackEventHandle") {
        MutableSharedFlow<Unit>()
    }

    private val childParams = ChildParams(
        onBack = ::onChildBack,
        parentBackEvent = currentChildBackEventHandle,
    )

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
                onChildBack()
            }
        }
    }

    init {
        // sets title and stepper value
        childStack.observe(lifecycle) { stack ->
            val currentRoute = stack.active.configuration
            params.titleProvider.changeTitle(currentRoute.screenTitle())
            innerNavigationState.update { it.copy(stackSize = currentRoute.stepNum()) }
        }
    }

    @Suppress("LongMethod")
    private fun createChild(
        route: OnboardingVisaRoute,
        factoryContext: AppComponentContext,
    ): ComposableContentComponent {
        return when (route) {
            is OnboardingVisaRoute.Welcome -> OnboardingVisaWelcomeComponent(
                appComponentContext = factoryContext,
                params = OnboardingVisaWelcomeComponent.Params(
                    isWelcomeBack = route.isWelcomeBack,
                    childParams = childParams,
                    onDone = { stackNavigation.push(OnboardingVisaRoute.AccessCode) },
                ),
            )
            OnboardingVisaRoute.AccessCode -> OnboardingVisaAccessCodeComponent(
                appComponentContext = factoryContext,
                params = OnboardingVisaAccessCodeComponent.Params(
                    childParams = childParams,
                    onDone = {
                        stackNavigation.push(
                            if (it.walletFound) {
                                OnboardingVisaRoute.TangemWalletApproveOption(
                                    visaDataForApprove = it.visaDataForApprove,
                                    allowNavigateBack = false,
                                )
                            } else {
                                OnboardingVisaRoute.ChooseWallet(it.visaDataForApprove)
                            },
                        )
                    },
                ),
                config = OnboardingVisaAccessCodeComponent.Config(
                    scanResponse = params.scanResponse,
                ),
            )
            is OnboardingVisaRoute.ChooseWallet -> OnboardingVisaChooseWalletComponent(
                appComponentContext = factoryContext,
                params = OnboardingVisaChooseWalletComponent.Params(
                    childParams = childParams,
                    onEvent = { event ->
                        stackNavigation.push(
                            when (event) {
                                OnboardingVisaChooseWalletComponent.Params.Event.TangemWallet ->
                                    OnboardingVisaRoute.TangemWalletApproveOption(
                                        visaDataForApprove = route.visaDataForApprove,
                                        allowNavigateBack = true,
                                    )
                                OnboardingVisaChooseWalletComponent.Params.Event.OtherWallet ->
                                    OnboardingVisaRoute.OtherWalletApproveOption(route.visaDataForApprove)
                            },
                        )
                    },
                ),
            )
            OnboardingVisaRoute.InProgress -> OnboardingVisaInProgressComponent(
                appComponentContext = factoryContext,
                config = OnboardingVisaInProgressComponent.Config(
                    scanResponse = params.scanResponse,
                ),
                params = OnboardingVisaInProgressComponent.Params(
                    childParams = childParams,
                    onDone = { stackNavigation.push(OnboardingVisaRoute.PinCode) },
                ),
            )
            is OnboardingVisaRoute.OtherWalletApproveOption -> OnboardingVisaOtherWalletComponent(
                appComponentContext = factoryContext,
                config = OnboardingVisaOtherWalletComponent.Config(
                    scanResponse = params.scanResponse,
                    visaDataForApprove = route.visaDataForApprove,
                ),
                params = OnboardingVisaOtherWalletComponent.Params(
                    childParams = childParams,
                    onDone = { stackNavigation.push(OnboardingVisaRoute.PinCode) },
                ),
            )
            OnboardingVisaRoute.PinCode -> OnboardingVisaPinCodeComponent(
                appComponentContext = factoryContext,
                config = OnboardingVisaPinCodeComponent.Config(
                    scanResponse = params.scanResponse,
                ),
                params = OnboardingVisaPinCodeComponent.Params(
                    childParams = childParams,
                    onDone = { params.onDone() },
                ),
            )
            is OnboardingVisaRoute.TangemWalletApproveOption -> OnboardingVisaApproveComponent(
                appComponentContext = factoryContext,
                config = OnboardingVisaApproveComponent.Config(
                    visaDataForApprove = route.visaDataForApprove,
                ),
                params = OnboardingVisaApproveComponent.Params(
                    childParams = childParams,
                    onDone = { stackNavigation.push(OnboardingVisaRoute.InProgress) },
                ),
            )
        }
    }

    private fun onChildBack() {
        if (childStack.value.backStack.size == 1) {
            // TODO show dialog
            return
        }

        when (val route = childStack.value.active.configuration) {
            OnboardingVisaRoute.AccessCode,
            is OnboardingVisaRoute.OtherWalletApproveOption,
            -> stackNavigation.pop()
            is OnboardingVisaRoute.TangemWalletApproveOption -> {
                if (route.allowNavigateBack) {
                    stackNavigation.pop()
                }
            }

            is OnboardingVisaRoute.Welcome,
            is OnboardingVisaRoute.ChooseWallet,
            OnboardingVisaRoute.InProgress,
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

    data class ChildParams(
        val onBack: () -> Unit,
        val parentBackEvent: SharedFlow<Unit>,
    )

    @AssistedFactory
    interface Factory : OnboardingVisaComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: OnboardingVisaComponent.Params,
        ): DefaultOnboardingVisaComponent
    }
}

internal data class OnboardingVisaInnerNavigationState(
    override val stackSize: Int,
) : InnerNavigationState {
    override val stackMaxSize: Int = ONBOARDING_VISA_STEPS_COUNT
}