package com.tangem.features.onboarding.v2.visa.impl.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.visa.model.VisaActivationRemoteState
import com.tangem.domain.visa.model.VisaCardActivationStatus
import com.tangem.features.onboarding.v2.visa.api.OnboardingVisaComponent
import com.tangem.features.onboarding.v2.visa.impl.OnboardingVisaInnerNavigationState
import com.tangem.features.onboarding.v2.visa.impl.child.accesscode.OnboardingVisaAccessCodeComponent
import com.tangem.features.onboarding.v2.visa.impl.child.choosewallet.OnboardingVisaChooseWalletComponent
import com.tangem.features.onboarding.v2.visa.impl.route.OnboardingVisaRoute
import com.tangem.features.onboarding.v2.visa.impl.route.stepNum
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@Stable
@ComponentScoped
internal class OnboardingVisaModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    @Suppress("UnusedPrivateMember")
    private val params = paramsContainer.require<OnboardingVisaComponent.Params>()

    private val _currentScanResponse = MutableStateFlow(params.scanResponse)

    val initialRoute = initializeRoute()

    val stackNavigation = StackNavigation<OnboardingVisaRoute>()

    private val _innerNavigationState =
        MutableStateFlow(OnboardingVisaInnerNavigationState(stackSize = initialRoute.stepNum()))

    val currentScanResponse = _currentScanResponse.asStateFlow()

    val innerNavigationState = _innerNavigationState.asStateFlow()

    fun updateStepForNewRoute(route: OnboardingVisaRoute) {
        _innerNavigationState.value = innerNavigationState.value.copy(
            stackSize = route.stepNum(),
        )
    }

    fun navigateFromWelcome(route: OnboardingVisaRoute.Welcome) {
        if (route.isWelcomeBack) {
            val scanResponse = _currentScanResponse.value
            val activationStatus = scanResponse.visaCardActivationStatus
                as? VisaCardActivationStatus.ActivationStarted
                ?: error("Activation status is not correct for welcome back route")

            when (activationStatus.remoteState) {
                is VisaActivationRemoteState.CardWalletSignatureRequired,
                is VisaActivationRemoteState.CustomerWalletSignatureRequired,
                -> OnboardingVisaRoute.AccessCode
                VisaActivationRemoteState.WaitingPinCode -> OnboardingVisaRoute.PinCode
                VisaActivationRemoteState.WaitingForActivationFinishing -> OnboardingVisaRoute.InProgress
                VisaActivationRemoteState.PaymentAccountDeploying -> OnboardingVisaRoute.InProgress
                else -> error("Remote state is not correct for welcome back route")
            }
        } else {
            stackNavigation.push(OnboardingVisaRoute.AccessCode)
        }
    }

    fun navigateFromAccessCode(result: OnboardingVisaAccessCodeComponent.DoneEvent) {
        _currentScanResponse.value = result.newScanResponse
        stackNavigation.push(
            if (result.walletFound) {
                OnboardingVisaRoute.TangemWalletApproveOption(
                    visaDataForApprove = result.visaDataForApprove,
                    allowNavigateBack = false,
                )
            } else {
                OnboardingVisaRoute.ChooseWallet(result.visaDataForApprove)
            },
        )
    }

    fun navigateFromChooseWallet(
        route: OnboardingVisaRoute.ChooseWallet,
        event: OnboardingVisaChooseWalletComponent.Params.Event,
    ) {
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
    }

    fun onChildBack(currentRoute: OnboardingVisaRoute, lastRoute: Boolean) {
        if (lastRoute) {
            // TODO show dialog
            return
        }

        when (currentRoute) {
            OnboardingVisaRoute.AccessCode,
            is OnboardingVisaRoute.OtherWalletApproveOption,
            -> stackNavigation.pop()
            is OnboardingVisaRoute.TangemWalletApproveOption -> {
                if (currentRoute.allowNavigateBack) {
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

    private fun initializeRoute(): OnboardingVisaRoute {
        val activationStatus = params.scanResponse.visaCardActivationStatus

        return when (activationStatus) {
            is VisaCardActivationStatus.ActivationStarted -> OnboardingVisaRoute.Welcome(isWelcomeBack = true)
            is VisaCardActivationStatus.NotStartedActivation -> OnboardingVisaRoute.Welcome(isWelcomeBack = false)
            else -> error("Visa activation status is not correct for onboarding flow")
        }
    }
}