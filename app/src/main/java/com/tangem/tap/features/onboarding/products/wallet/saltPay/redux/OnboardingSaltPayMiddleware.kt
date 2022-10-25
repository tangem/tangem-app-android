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
                val updateResult = state.saltPayManager.updateActivationStatus(state.amountToClaim).successOr {
                    onException(it.error)
                    return@launch
                }

                handleInProgress = false
                withMainContext {
                    store.dispatch(OnboardingSaltPayAction.SetStep(updateResult.step))
                }
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

                if (!attestationResponse.success) {
                    onError(SaltPayActivationError.CardNotFound(attestationResponse.error))
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

                withMainContext {
                    handleInProgress = false
                    store.dispatch(OnboardingSaltPayAction.SetStep(SaltPayActivationStep.KycIntro))
                }
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
                analyticsHandler.handleAnalyticsEvent(Onboarding.ClaimWasSuccessfully())
                dispatchOnMain(OnboardingSaltPayAction.SetStep(SaltPayActivationStep.ClaimInProgress))
                dispatchOnMain(OnboardingSaltPayAction.RefreshClaim)
            }
        }
        is OnboardingSaltPayAction.RefreshClaim -> {
            handleInProgress = true
            handleClaimRefreshInProgress = true

            val state = getState()
            scope.launch {
                val tokenAmountValue = state.saltPayManager.getTokenAmount().successOr {
                    SaltPayExceptionHandler.handle(it.error)
                    handleClaimRefreshInProgress = false
                    return@launch
                }

                if (tokenAmountValue.isPositive()) {
                    handleInProgress = false
                    handleClaimRefreshInProgress = false
                    dispatchOnMain(OnboardingSaltPayAction.SetTokenBalance(tokenAmountValue))
                    dispatchOnMain(OnboardingSaltPayAction.SetStep(SaltPayActivationStep.Finished))
                } else {
                    handleClaimRefreshInProgress = false
                }
            }
        }
        is OnboardingSaltPayAction.SetStep -> handleAnalytics(analyticsHandler, action.newStep)
        else -> {
            /* do nothing, only reduce */
        }
    }
}

fun handleAnalytics(analyticsHandler: GlobalAnalyticsEventHandler, step: SaltPayActivationStep?) {
    when (step) {
        SaltPayActivationStep.None -> {}
        SaltPayActivationStep.NoGas -> {}
        SaltPayActivationStep.NeedPin -> {}
        SaltPayActivationStep.CardRegistration -> {}
        SaltPayActivationStep.KycIntro -> {}
        SaltPayActivationStep.KycStart -> analyticsHandler.handleAnalyticsEvent(Onboarding.KYCStarted())
        SaltPayActivationStep.KycWaiting -> analyticsHandler.handleAnalyticsEvent(Onboarding.KYCInProgress())
        SaltPayActivationStep.KycReject -> analyticsHandler.handleAnalyticsEvent(Onboarding.KYCRejected())
        SaltPayActivationStep.Claim -> analyticsHandler.handleAnalyticsEvent(Onboarding.ClaimScreenOpened())
        SaltPayActivationStep.ClaimInProgress -> {}
        SaltPayActivationStep.Finished -> {}
        null -> {}
    }
}

data class UpdateResult(
    val step: SaltPayActivationStep = SaltPayActivationStep.None,
    val amountToClaim: Amount? = null,
)

suspend fun SaltPayActivationManager.updateActivationStatus(amountToClaim: Amount?): Result<UpdateResult> {
    Timber.d("updateSaltPayStatus")
    var updateResult = UpdateResult()

    val saltPayStep = checkRegistration(this).successOr {
        return Result.Failure(it.error)
    }

    checkGasIfNeeded(this, saltPayStep).successOr {
        return Result.Failure(it.error)
    }

    val fetchedAmountToClaim = getAmountToClaimIfNeeded(this, amountToClaim)
    updateResult = when {
        // if fetchedAmountToClaim = null -> then it already claimed
        saltPayStep == SaltPayActivationStep.Claim && fetchedAmountToClaim == null -> {
            updateResult.copy(
                step = SaltPayActivationStep.Finished,
                amountToClaim = fetchedAmountToClaim,
            )
        }
        else -> updateResult.copy(
            step = saltPayStep,
            amountToClaim = fetchedAmountToClaim,
        )
    }

    Timber.d("updateSaltPayStatus: success: %s", updateResult)
    return Result.Success(updateResult)
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

private suspend fun checkRegistration(
    saltPayManager: SaltPayActivationManager,
): Result<SaltPayActivationStep> {
    Timber.d("checkRegistration")
    return try {
        val registrationResponseItem = saltPayManager.checkRegistration().successOr {
            Timber.e(it.error, "checkRegistration: failure")
            return it
        }

        Timber.d("checkRegistration: success: try convert to SaltPayStep")
        val step = registrationResponseItem.toSaltPayStep()
        Timber.d("checkRegistration: step: %s", step)
        Result.Success(step)
    } catch (ex: SaltPayActivationError) {
        Timber.e(ex, "checkRegistration: step")
        Result.Failure(ex)
    }
}

@Throws(SaltPayActivationError::class)
fun RegistrationResponse.Item.toSaltPayStep(): SaltPayActivationStep {
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