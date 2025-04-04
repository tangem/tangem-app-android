package com.tangem.features.onboarding.v2.visa.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.subscribe
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

    private val currentChildBackEventHandle = instanceKeeper.getOrCreateSimple(key = "currentChildBackEventHandle") {
        MutableSharedFlow<Unit>()
    }

    private val childParams = ChildParams(
        onBack = ::onChildBack,
        parentBackEvent = currentChildBackEventHandle,
    )

    private val childStack: Value<ChildStack<OnboardingVisaRoute, ComposableContentComponent>> =
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
            if (childStack.value.active.configuration is OnboardingVisaRoute.AccessCode) {
                componentScope.launch { currentChildBackEventHandle.emit(Unit) }
            } else {
                onChildBack()
            }
        }
    }

    init {
        componentScope.launch {
            model.onDone.collect { params.onDone() }
        }

        // sets title and stepper value
        childStack.subscribe(lifecycle) { stack ->
            val currentRoute = stack.active.configuration
            params.titleProvider.changeTitle(currentRoute.screenTitle())
            model.updateStepForNewRoute(currentRoute)
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
                config = OnboardingVisaWelcomeComponent.Config.Welcome,
                params = OnboardingVisaWelcomeComponent.Params(
                    childParams = childParams,
                    onDone = { model.stackNavigation.pushNew(OnboardingVisaRoute.AccessCode) },
                ),
            )
            is OnboardingVisaRoute.WelcomeBack -> OnboardingVisaWelcomeComponent(
                appComponentContext = factoryContext,
                config = OnboardingVisaWelcomeComponent.Config.WelcomeBack(
                    activationInput = route.activationInput,
                    dataToSignRequest = route.dataToSignByCardWalletRequest,
                    scanResponse = model.currentScanResponse.value,
                ),
                params = OnboardingVisaWelcomeComponent.Params(
                    childParams = childParams,
                    onDone = {
                        val activationReadyEvent =
                            (it as? OnboardingVisaWelcomeComponent.DoneEvent.WelcomeBackDone)?.activationReadyEvent

                        if (activationReadyEvent != null) {
                            model.navigateFromActivationScreen(activationReadyEvent)
                        }
                    },
                ),
            )
            OnboardingVisaRoute.AccessCode -> OnboardingVisaAccessCodeComponent(
                appComponentContext = factoryContext,
                params = OnboardingVisaAccessCodeComponent.Params(
                    childParams = childParams,
                    onDone = { model.navigateFromActivationScreen(it) },
                ),
                config = OnboardingVisaAccessCodeComponent.Config(
                    scanResponse = model.currentScanResponse.value,
                ),
            )
            is OnboardingVisaRoute.ChooseWallet -> OnboardingVisaChooseWalletComponent(
                appComponentContext = factoryContext,
                params = OnboardingVisaChooseWalletComponent.Params(
                    childParams = childParams,
                    onEvent = { event -> model.navigateFromChooseWallet(route, event) },
                ),
            )
            is OnboardingVisaRoute.InProgress -> OnboardingVisaInProgressComponent(
                appComponentContext = factoryContext,
                config = OnboardingVisaInProgressComponent.Config(
                    scanResponse = model.currentScanResponse.value,
                    type = when (route.from) {
                        OnboardingVisaRoute.InProgress.From.Approve ->
                            OnboardingVisaInProgressComponent.Config.Type.AfterApprove
                        OnboardingVisaRoute.InProgress.From.PinCode ->
                            OnboardingVisaInProgressComponent.Config.Type.AfterPinCode
                    },
                ),
                params = OnboardingVisaInProgressComponent.Params(
                    childParams = childParams,
                    onDone = { model.navigateFromInProgress(it) },
                ),
            )
            is OnboardingVisaRoute.OtherWalletApproveOption -> OnboardingVisaOtherWalletComponent(
                appComponentContext = factoryContext,
                config = OnboardingVisaOtherWalletComponent.Config(
                    scanResponse = model.currentScanResponse.value,
                    preparationDataForApprove = route.preparationDataForApprove,
                ),
                params = OnboardingVisaOtherWalletComponent.Params(
                    childParams = childParams,
                    onDone = {
                        model.stackNavigation.pushNew(OnboardingVisaRoute.PinCode(activationOrderInfo = it))
                    },
                ),
            )
            is OnboardingVisaRoute.PinCode -> OnboardingVisaPinCodeComponent(
                appComponentContext = factoryContext,
                config = OnboardingVisaPinCodeComponent.Config(
                    scanResponse = model.currentScanResponse.value,
                    activationOrderInfo = route.activationOrderInfo,
                ),
                params = OnboardingVisaPinCodeComponent.Params(
                    childParams = childParams,
                    onDone = {
                        model.stackNavigation.pushNew(
                            OnboardingVisaRoute.InProgress(from = OnboardingVisaRoute.InProgress.From.PinCode),
                        )
                    },
                ),
            )
            is OnboardingVisaRoute.TangemWalletApproveOption -> OnboardingVisaApproveComponent(
                appComponentContext = factoryContext,
                config = OnboardingVisaApproveComponent.Config(
                    customerWalletCardId = route.foundWalletCardId,
                    preparationDataForApprove = route.preparationDataForApprove,
                    scanResponse = model.currentScanResponse.value,
                ),
                params = OnboardingVisaApproveComponent.Params(
                    childParams = childParams,
                    onDone = {
                        model.stackNavigation.pushNew(
                            OnboardingVisaRoute.InProgress(from = OnboardingVisaRoute.InProgress.From.Approve),
                        )
                    },
                ),
            )
        }
    }

    private fun onChildBack() = model.onChildBack(
        currentRoute = childStack.value.active.configuration,
        lastRoute = childStack.value.backStack.size == 1,
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