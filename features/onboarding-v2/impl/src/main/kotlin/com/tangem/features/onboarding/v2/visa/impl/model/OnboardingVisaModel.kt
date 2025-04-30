package com.tangem.features.onboarding.v2.visa.impl.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.router.stack.replaceAll
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.common.visa.VisaUtilities
import com.tangem.domain.common.visa.VisaWalletPublicKeyUtility
import com.tangem.domain.visa.model.VisaActivationRemoteState
import com.tangem.domain.visa.model.VisaCardActivationStatus
import com.tangem.domain.visa.model.VisaCardWalletDataToSignRequest
import com.tangem.domain.visa.model.VisaCustomerWalletDataToSignRequest
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.onboarding.v2.visa.api.OnboardingVisaComponent
import com.tangem.features.onboarding.v2.visa.impl.OnboardingVisaInnerNavigationState
import com.tangem.features.onboarding.v2.visa.impl.child.choosewallet.OnboardingVisaChooseWalletComponent
import com.tangem.features.onboarding.v2.visa.impl.child.inprogress.OnboardingVisaInProgressComponent
import com.tangem.features.onboarding.v2.visa.impl.common.ActivationReadyEvent
import com.tangem.features.onboarding.v2.visa.impl.common.PreparationDataForApprove
import com.tangem.features.onboarding.v2.visa.impl.route.OnboardingVisaRoute
import com.tangem.features.onboarding.v2.visa.impl.route.stepNum
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class OnboardingVisaModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getWalletsUseCase: GetWalletsUseCase,
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

    val onDone = MutableSharedFlow<Unit>()

    fun updateStepForNewRoute(route: OnboardingVisaRoute) {
        _innerNavigationState.value = innerNavigationState.value.copy(
            stackSize = route.stepNum(),
        )
    }

    fun navigateFromActivationScreen(result: ActivationReadyEvent) {
        _currentScanResponse.value = result.newScanResponse
        val foundWalletCardId = tryToFindExistingWalletCardId(result.customerWalletTargetAddress)
        val preparationDataForApprove = PreparationDataForApprove(
            customerWalletAddress = result.customerWalletTargetAddress,
            request = result.customerWalletDataToSignRequest,
        )
        stackNavigation.pushNew(
            if (foundWalletCardId != null) {
                OnboardingVisaRoute.TangemWalletApproveOption(
                    foundWalletCardId = foundWalletCardId,
                    preparationDataForApprove = preparationDataForApprove,
                    allowNavigateBack = false,
                )
            } else {
                OnboardingVisaRoute.ChooseWallet(
                    preparationDataForApprove = preparationDataForApprove,
                )
            },
        )
    }

    fun navigateFromInProgress(event: OnboardingVisaInProgressComponent.Params.DoneEvent) {
        when (event) {
            OnboardingVisaInProgressComponent.Params.DoneEvent.Activated -> {
                modelScope.launch { onDone.emit(Unit) }
            }
            is OnboardingVisaInProgressComponent.Params.DoneEvent.NavigateTo -> {
                stackNavigation.replaceAll(event.route)
            }
        }
    }

    fun navigateFromChooseWallet(
        route: OnboardingVisaRoute.ChooseWallet,
        event: OnboardingVisaChooseWalletComponent.Params.Event,
    ) {
        stackNavigation.pushNew(
            when (event) {
                OnboardingVisaChooseWalletComponent.Params.Event.TangemWallet ->
                    OnboardingVisaRoute.TangemWalletApproveOption(
                        preparationDataForApprove = route.preparationDataForApprove,
                        foundWalletCardId = null,
                        allowNavigateBack = true,
                    )
                OnboardingVisaChooseWalletComponent.Params.Event.OtherWallet ->
                    OnboardingVisaRoute.OtherWalletApproveOption(
                        preparationDataForApprove = route.preparationDataForApprove,
                    )
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
            is OnboardingVisaRoute.WelcomeBack,
            is OnboardingVisaRoute.Welcome,
            is OnboardingVisaRoute.ChooseWallet,
            is OnboardingVisaRoute.InProgress,
            is OnboardingVisaRoute.PinCode,
            -> {
            }
        }
    }

    private fun initializeRoute(): OnboardingVisaRoute {
        return when (val activationStatus = params.scanResponse.visaCardActivationStatus) {
            is VisaCardActivationStatus.ActivationStarted -> {
                when (val remoteState = activationStatus.remoteState) {
                    is VisaActivationRemoteState.CardWalletSignatureRequired -> {
                        if (activationStatus.activationInput.isAccessCodeSet) {
                            OnboardingVisaRoute.WelcomeBack(
                                activationInput = activationStatus.activationInput,
                                dataToSignByCardWalletRequest = VisaCardWalletDataToSignRequest(
                                    activationOrderInfo = remoteState.activationOrderInfo,
                                    cardWalletAddress = activationStatus.cardWalletAddress,
                                ),
                            )
                        } else {
                            OnboardingVisaRoute.AccessCode
                        }
                    }
                    is VisaActivationRemoteState.CustomerWalletSignatureRequired -> {
                        remoteState.getRoute(activationStatus)
                    }
                    VisaActivationRemoteState.PaymentAccountDeploying -> {
                        OnboardingVisaRoute.InProgress(from = OnboardingVisaRoute.InProgress.From.Approve)
                    }
                    VisaActivationRemoteState.WaitingForActivationFinishing -> {
                        OnboardingVisaRoute.InProgress(from = OnboardingVisaRoute.InProgress.From.PinCode)
                    }
                    is VisaActivationRemoteState.AwaitingPinCode -> {
                        OnboardingVisaRoute.PinCode(
                            activationOrderInfo = remoteState.activationOrderInfo,
                            pinCodeValidationError = false,
                        )
                    }
                    VisaActivationRemoteState.Activated,
                    VisaActivationRemoteState.BlockedForActivation,
                    VisaActivationRemoteState.Failed,
                    -> error("Activation status is not correct for onboarding flow")
                }
            }
            is VisaCardActivationStatus.NotStartedActivation -> OnboardingVisaRoute.Welcome
            else -> error("Visa activation status is not correct for onboarding flow")
        }
    }

    private fun VisaActivationRemoteState.CustomerWalletSignatureRequired.getRoute(
        activationStatus: VisaCardActivationStatus.ActivationStarted,
    ): OnboardingVisaRoute {
        val foundWalletCardId = tryToFindExistingWalletCardId(this.activationOrderInfo.customerWalletAddress)
        val request = VisaCustomerWalletDataToSignRequest(
            orderId = this.activationOrderInfo.orderId,
            cardWalletAddress = activationStatus.cardWalletAddress,
            customerWalletAddress = this.activationOrderInfo.customerWalletAddress,
        )
        val preparationDataForApprove = PreparationDataForApprove(
            customerWalletAddress = this.activationOrderInfo.customerWalletAddress,
            request = request,
        )

        return if (foundWalletCardId != null) {
            OnboardingVisaRoute.TangemWalletApproveOption(
                preparationDataForApprove = preparationDataForApprove,
                foundWalletCardId = foundWalletCardId,
                allowNavigateBack = false,
            )
        } else {
            OnboardingVisaRoute.ChooseWallet(
                preparationDataForApprove = preparationDataForApprove,
            )
        }
    }

    private fun tryToFindExistingWalletCardId(targetAddress: String): String? {
        val wallets = getWalletsUseCase.invokeSync().filter { it.isLocked.not() }

        return wallets.firstOrNull { wallet ->
            wallet.scanResponse.card.wallets.any {
                val derivedKey = it.derivedKeys[VisaUtilities.visaDefaultDerivationPath] ?: return@any false

                VisaWalletPublicKeyUtility.validateExtendedPublicKey(
                    targetAddress = targetAddress,
                    extendedPublicKey = derivedKey,
                ).onLeft {
                    return@any VisaWalletPublicKeyUtility.findKeyWithoutDerivation(
                        targetAddress = targetAddress,
                        card = wallet.scanResponse.card,
                    ).isRight()
                }.isRight()
            }
        }?.cardId
    }
}