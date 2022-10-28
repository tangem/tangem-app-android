package com.tangem.tap.features.onboarding.products.wallet.saltPay.redux

import com.tangem.Message
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.extensions.successOr
import com.tangem.common.Filter
import com.tangem.common.extensions.guard
import com.tangem.common.services.Result
import com.tangem.domain.common.extensions.successOr
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.network.api.paymentology.KYCStatus
import com.tangem.network.api.paymentology.RegistrationResponse
import com.tangem.tap.common.analytics.GlobalAnalyticsEventHandler
import com.tangem.tap.common.analytics.events.Onboarding
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.isPositive
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.TangemSigner
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.onboarding.products.wallet.redux.OnboardingWalletState
import com.tangem.tap.features.onboarding.products.wallet.saltPay.AllSymbolsTheSameFilter
import com.tangem.tap.features.onboarding.products.wallet.saltPay.SaltPayActivationManager
import com.tangem.tap.features.onboarding.products.wallet.saltPay.SaltPayExceptionHandler
import com.tangem.tap.features.onboarding.products.wallet.saltPay.message.SaltPayActivationError
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdk
import com.tangem.tap.tangemSdkManager
import com.tangem.wallet.R
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
class OnboardingSaltPayMiddleware {
    companion object {
        val handler = onboardingSaltPayMiddleware
    }
}

private val onboardingSaltPayMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            handleOnboardingSaltPayAction(action, state)
            next(action)
        }
    }
}

private fun handleOnboardingSaltPayAction(anyAction: Action, appState: () -> AppState?) {
    if (DemoHelper.tryHandle(appState, anyAction)) return
    val action = anyAction as? OnboardingSaltPayAction ?: return
    appState() ?: return

    fun getAppState(): AppState = appState()!!
    fun getOnboardingWalletState(): OnboardingWalletState = getAppState().onboardingWalletState
    fun getState(): OnboardingSaltPayState = getOnboardingWalletState().onboardingSaltPayState!!
    val analyticsHandler = getAppState().globalState.analyticsHandler

    when (action) {
        is OnboardingSaltPayAction.Update -> {
            handleInProgress = true

            val state = getState()
            scope.launch {
                val updateStep = state.saltPayManager.update(state.step, state.amountToClaim).successOr {
                    onException(it.error)
                    return@launch
                }

                handleInProgress = false
                dispatchOnMain(OnboardingSaltPayAction.SetStep(updateStep))
            }
        }
        is OnboardingSaltPayAction.RegisterCard -> {
            analyticsHandler.handleAnalyticsEvent(Onboarding.ButtonConnect())

            handleInProgress = true
            val state = getState()

            scope.launch {
                val saltPayManager = state.saltPayManager
                checkGasIfNeeded(state.saltPayManager, state.step).successOr {
                    onException(it.error)
                    return@launch
                }

                val pinCode = state.pinCode.guard {
                    onError(SaltPayActivationError.NeedPin)
                    return@launch
                }

                val attestationResponse = saltPayManager.requestAttestationChallenge().successOr {
                    onException(it.error)
                    return@launch
                }

                val registrationResponse = tangemSdkManager.runTaskAsync(
                    runnable = saltPayManager.makeRegistrationTask(attestationResponse.challenge!!),
                    cardId = saltPayManager.cardId,
                    initialMessage = Message(
                        header = null,
                        body = tangemSdkManager.getString(R.string.registration_task_alert_message),
                    ),
                    accessCode = state.accessCode,
                ).successOr {
                    onException(it.error as Exception)
                    return@launch
                }

                saltPayManager.sendTransactions(registrationResponse.signedTransactions).successOr {
                    onError(SaltPayActivationError.FailedToSendTx)
                    return@launch
                }

                saltPayManager.registerWallet(registrationResponse.attestResponse, pinCode).successOr {
                    onException(it.error)
                    return@launch
                }

                handleInProgress = false
                dispatchOnMain(OnboardingSaltPayAction.SetStep(SaltPayActivationStep.KycIntro))
            }
        }
        is OnboardingSaltPayAction.OpenUtorgKYC -> {
            store.dispatch(OnboardingSaltPayAction.SetStep(SaltPayActivationStep.KycStart))
        }
        is OnboardingSaltPayAction.UtorgKYCRedirectSuccess -> {
            store.dispatch(OnboardingSaltPayAction.RegisterKYC)
        }
        is OnboardingSaltPayAction.RegisterKYC -> {
            val state = getState()
            store.dispatch(OnboardingSaltPayAction.SetStep(SaltPayActivationStep.KycWaiting))
            handleInProgress = true

            scope.launch {
                Timber.d("saltPayManager.registerKYC()")
                state.saltPayManager.registerKYC().successOr {
                    onException(it.error)
                    return@launch
                }

                handleInProgress = false
                dispatchOnMain(OnboardingSaltPayAction.Update)
            }
        }
        is OnboardingSaltPayAction.TrySetPin -> {
            try {
                assertPinValid(action.pin, getState().pinLength)
                analyticsHandler.handleAnalyticsEvent(Onboarding.PinCodeSet())
                store.dispatch(OnboardingSaltPayAction.SetPin(action.pin))
                store.dispatch(OnboardingSaltPayAction.SetStep(SaltPayActivationStep.CardRegistration))
            } catch (error: SaltPayActivationError) {
                SaltPayExceptionHandler.handle(error)
            }
        }
        is OnboardingSaltPayAction.Claim -> {
            analyticsHandler.handleAnalyticsEvent(Onboarding.ButtonClaim())
            val state = getState()
            handleInProgress = true

            scope.launch {
                val amountToClaim = getAmountToClaimIfNeeded(state.saltPayManager, state.amountToClaim).guard {
                    onException(SaltPayActivationError.NoFundsToClaim)
                    return@launch
                }

                val scanResponse = store.state.globalState.scanResponse
                    ?: store.state.globalState.onboardingState.onboardingManager?.scanResponse
                val signer = TangemSigner(
                    card = scanResponse!!.card,
                    tangemSdk = tangemSdk,
                    initialMessage = Message(),
                    accessCode = state.accessCode,
                ) { signResponse ->
                    store.dispatch(
                        GlobalAction.UpdateWalletSignedHashes(
                            walletSignedHashes = signResponse.totalSignedHashes,
                            walletPublicKey = state.saltPayManager.walletPublicKey,
                            remainingSignatures = signResponse.remainingSignatures,
                        ),
                    )
                }
                state.saltPayManager.claim(amountToClaim.value!!, signer).successOr {
                    dispatchOnMain(OnboardingSaltPayAction.SetStep(SaltPayActivationStep.Claim))
                    onException(it.error)
                    return@launch
                }

                handleInProgress = false
                analyticsHandler.handleAnalyticsEvent(Onboarding.ClaimWasSuccessfully())
                dispatchOnMain(OnboardingSaltPayAction.SetStep(SaltPayActivationStep.ClaimInProgress))
                dispatchOnMain(OnboardingSaltPayAction.RefreshClaim)
            }
        }
        is OnboardingSaltPayAction.RefreshClaim -> {
            handleClaimRefreshInProgress = true

            val state = getState()
            scope.launch {
                when (val amountToClaimResult = state.saltPayManager.getAmountToClaim()) {
                    is Result.Success -> {
                        // need to re claim
                        handleClaimRefreshInProgress = false
                        dispatchOnMain(OnboardingSaltPayAction.SetAmountToClaim(amountToClaimResult.data))
                        dispatchOnMain(OnboardingSaltPayAction.SetStep(SaltPayActivationStep.Claim))
                    }
                    is Result.Failure -> {
                        when (amountToClaimResult.error) {
                            is SaltPayActivationError.NoFundsToClaim -> {
                                val tokenAmountValue = state.saltPayManager.getTokenAmount().successOr {
                                    SaltPayExceptionHandler.handle(it.error)
                                    handleClaimRefreshInProgress = false
                                    return@launch
                                }

                                handleClaimRefreshInProgress = false
                                if (tokenAmountValue.isPositive()) {
                                    dispatchOnMain(OnboardingSaltPayAction.SetTokenBalance(tokenAmountValue))
                                    dispatchOnMain(OnboardingSaltPayAction.SetStep(SaltPayActivationStep.ClaimSuccess))
                                }
                            }
                            else -> {
                                handleClaimRefreshInProgress = false
                                SaltPayExceptionHandler.handle(amountToClaimResult.error)
                            }
                        }
                    }
                }
            }
        }
        is OnboardingSaltPayAction.SetStep -> handleAnalytics(analyticsHandler, action.newStep)
        else -> {
            /* do nothing, only reduce */
        }
    }
}

private fun handleAnalytics(analyticsHandler: GlobalAnalyticsEventHandler, step: SaltPayActivationStep) {
    when (step) {
        SaltPayActivationStep.KycStart -> analyticsHandler.handleAnalyticsEvent(Onboarding.KYCStarted())
        SaltPayActivationStep.KycWaiting -> analyticsHandler.handleAnalyticsEvent(Onboarding.KYCInProgress())
        SaltPayActivationStep.KycReject -> analyticsHandler.handleAnalyticsEvent(Onboarding.KYCRejected())
        SaltPayActivationStep.Claim -> analyticsHandler.handleAnalyticsEvent(Onboarding.ClaimScreenOpened())
        else -> {}
    }
}

suspend fun SaltPayActivationManager.update(
    currentStep: SaltPayActivationStep,
    currentAmountToClaim: Amount?,
): Result<SaltPayActivationStep> {
    Timber.d("updateSaltPayStatus")

    val registrationResponse = checkRegistration().successOr { return it }
    val amountToClaim = getAmountToClaimIfNeeded(this, currentAmountToClaim)

    val newStep = try {
        determineStep(currentStep, amountToClaim, registrationResponse)
    } catch (ex: Exception) {
        return Result.Failure(ex)
    }

    checkGasIfNeeded(this, newStep).successOr { return it }

    Timber.d("update: success: %s", newStep)
    return Result.Success(newStep)
}

private fun determineStep(
    currentStep: SaltPayActivationStep,
    amountToClaim: Amount?,
    response: RegistrationResponse.Item,
): SaltPayActivationStep {
    fun getStepBaseOnAmountToClaim(amount: Amount?): SaltPayActivationStep = when (amount) {
        null -> SaltPayActivationStep.Success
        else -> SaltPayActivationStep.Claim
    }

    return when {
        response.passed != true -> throw SaltPayActivationError.CardNotPassed(response.error)
        response.disabledByAdmin == true -> throw SaltPayActivationError.CardDisabled(response.error)

        // go to claim screen
        response.active == true -> getStepBaseOnAmountToClaim(amountToClaim)

        // pinSet is false, go toPin screen
        response.pinSet == false -> SaltPayActivationStep.NeedPin

        response.kycStatus != null -> {
            when (currentStep) {
                SaltPayActivationStep.KycWaiting -> when (response.kycStatus) {
                    KYCStatus.NOT_STARTED, KYCStatus.STARTED -> SaltPayActivationStep.KycIntro
                    KYCStatus.WAITING_FOR_APPROVAL -> SaltPayActivationStep.KycWaiting
                    KYCStatus.CORRECTION_REQUESTED, KYCStatus.REJECTED -> SaltPayActivationStep.KycReject
                    KYCStatus.APPROVED -> getStepBaseOnAmountToClaim(amountToClaim)
                    else -> SaltPayActivationStep.KycIntro
                }
                SaltPayActivationStep.KycReject -> when (response.kycStatus) {
                    KYCStatus.NOT_STARTED, KYCStatus.STARTED -> SaltPayActivationStep.KycStart
                    KYCStatus.WAITING_FOR_APPROVAL -> SaltPayActivationStep.KycWaiting
                    KYCStatus.CORRECTION_REQUESTED -> SaltPayActivationStep.KycStart
                    KYCStatus.REJECTED -> SaltPayActivationStep.KycStart
                    KYCStatus.APPROVED -> getStepBaseOnAmountToClaim(amountToClaim)
                    else -> SaltPayActivationStep.KycIntro
                }
                else -> when (response.kycStatus) {
                    KYCStatus.NOT_STARTED, KYCStatus.STARTED -> SaltPayActivationStep.KycIntro
                    KYCStatus.WAITING_FOR_APPROVAL -> SaltPayActivationStep.KycWaiting
                    KYCStatus.CORRECTION_REQUESTED, KYCStatus.REJECTED -> SaltPayActivationStep.KycReject
                    KYCStatus.APPROVED -> getStepBaseOnAmountToClaim(amountToClaim)
                    else -> SaltPayActivationStep.KycIntro
                }
            }
        }

        // kycDate is set, go to kyc waiting screen
        response.kycDate != null -> SaltPayActivationStep.KycWaiting

        else -> SaltPayActivationStep.KycIntro
    }
}

private suspend fun getAmountToClaimIfNeeded(
    saltPayManager: SaltPayActivationManager,
    amountToClaim: Amount?,
): Amount? {
    Timber.d("getAmountToClaimIfNeeded: for amount: %s", amountToClaim)
    if (amountToClaim != null) {
        Timber.d("getAmountToClaimIfNeeded: checks no need, amount already persist")
        return amountToClaim
    }

    return when (val result = saltPayManager.getAmountToClaim()) {
        is Result.Success -> {
            Timber.d("getAmountToClaimIfNeeded: success: %s", result.data)
            return result.data
        }
        is Result.Failure -> {
            Timber.e(result.error, "getAmountToClaimIfNeeded: failure")
            null
        }
    }
}

private var handleClaimRefreshInProgress: Boolean = false
    set(value) {
        field = value
        store.dispatchOnMain(OnboardingSaltPayAction.SetClaimRefreshInProgress(value))
    }

private var handleInProgress: Boolean = false
    set(value) {
        field = value
        store.dispatchOnMain(OnboardingSaltPayAction.SetInProgress(value))
    }

private suspend fun onError(error: SaltPayActivationError) {
    withMainContext {
        handleInProgress = false
        SaltPayExceptionHandler.handle(error)
    }
}

private suspend fun onException(error: Throwable) {
    withMainContext {
        handleInProgress = false
        SaltPayExceptionHandler.handle(error)
    }
}

private suspend fun checkGasIfNeeded(
    saltPayManager: SaltPayActivationManager,
    step: SaltPayActivationStep,
): Result<Unit> {
    Timber.d("checkGasIfNeeded: for step: %s", step)
    if (step.ordinal >= SaltPayActivationStep.KycIntro.ordinal) {
        Timber.d("checkGasIfNeeded: no need to check for step: %s", step)
        return Result.Success(Unit)
    }

    val result = saltPayManager.checkHasGas()
    when (result) {
        is Result.Success -> Timber.d("checkGasIfNeeded: has GAS")
        is Result.Failure -> Timber.d("checkGasIfNeeded: has NO GAS")
    }
    return result
}

@Throws(SaltPayActivationError::class)
fun RegistrationResponse.Item.toSaltPayStep(currentStep: SaltPayActivationStep): SaltPayActivationStep {
    return when {
        passed != true -> throw SaltPayActivationError.CardNotPassed(this.error)
        disabledByAdmin == true -> throw SaltPayActivationError.CardDisabled(this.error)

        // go to claim screen
        active == true -> SaltPayActivationStep.Claim

        // pinSet is false, go toPin screen
        pinSet == false -> SaltPayActivationStep.NeedPin

        kycStatus != null -> {
            when (kycStatus) {
                KYCStatus.NOT_STARTED, KYCStatus.STARTED -> SaltPayActivationStep.KycIntro
                KYCStatus.WAITING_FOR_APPROVAL -> SaltPayActivationStep.KycWaiting
                KYCStatus.CORRECTION_REQUESTED, KYCStatus.REJECTED -> SaltPayActivationStep.KycReject
                KYCStatus.APPROVED -> SaltPayActivationStep.Claim
                null -> throw UnsupportedOperationException()
            }
        }
        // kycDate is set, go to kyc waiting screen
        kycDate != null -> SaltPayActivationStep.KycWaiting

        else -> SaltPayActivationStep.KycIntro
    }
}

@Throws(SaltPayActivationError::class)
private fun assertPinValid(pin: String, pinLength: Int) {
    val array = pin.toCharArray()

    if (array.size < pinLength) throw SaltPayActivationError.WeakPin

    val filters: List<Filter<String>> = listOf(
        AllSymbolsTheSameFilter(),
    )

    if (filters.any { it.filter(pin) }) {
        throw SaltPayActivationError.WeakPin
    }
}