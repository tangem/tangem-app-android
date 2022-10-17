package com.tangem.tap.features.onboarding.products.wallet.saltPay.redux

import com.tangem.Message
import com.tangem.blockchain.extensions.successOr
import com.tangem.common.Filter
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.common.services.Result
import com.tangem.domain.common.extensions.successOr
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.network.api.paymentology.RegistrationResponse
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.dispatchErrorNotification
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.TapError
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.onboarding.products.wallet.redux.OnboardingWalletState
import com.tangem.tap.features.onboarding.products.wallet.saltPay.AllSymbolsTheSameFilter
import com.tangem.tap.features.onboarding.products.wallet.saltPay.SaltPayRegistrationManager
import com.tangem.tap.features.onboarding.products.wallet.saltPay.dialog.SaltPayDialog
import com.tangem.tap.features.onboarding.products.wallet.saltPay.message.SaltPayRegistrationError
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.wallet.R
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware
import timber.log.Timber

/**
* [REDACTED_AUTHOR]
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

    when (action) {
        is OnboardingSaltPayAction.RegisterCard -> {
            handleIsBusy = true
            val state = getState()

            scope.launch {
                val saltPayManager = state.saltPayManager
                checkGasIfNeeded(state.saltPayManager, state.step).successOr {
                    onException(it.error)
                    return@launch
                }

                val pinCode = state.pinCode.guard {
                    onError(SaltPayRegistrationError.NeedPin)
                    return@launch
                }

                val attestationResponse = saltPayManager.requestAttestationChallenge().successOr {
                    onException(it.error)
                    return@launch
                }

                if (!attestationResponse.success) {
                    onError(SaltPayRegistrationError.CardNotFound(attestationResponse.error))
                    return@launch
                }

                val registrationResponse = tangemSdkManager.runTaskAsync(
                    runnable = saltPayManager.makeRegistrationTask(attestationResponse.challenge!!),
                    cardId = saltPayManager.cardId,
                    initialMessage = Message(
                        header = null,
                        body = tangemSdkManager.getString(R.string.registration_task_alert_message),
                    ),
                ).successOr {
                    onException(it.error as Exception)
                    return@launch
                }

                saltPayManager.sendTransactions(registrationResponse.signedTransactions).successOr {
                    onError(SaltPayRegistrationError.FailedToSendTx)
                    return@launch
                }

                saltPayManager.registerWallet(registrationResponse.attestResponse, pinCode).successOr {
                    onException(it.error)
                    return@launch
                }

                withMainContext {
                    handleIsBusy = false
                    store.dispatch(OnboardingSaltPayAction.SetStep(SaltPayRegistrationStep.Kyc))
                }
            }
        }
        is OnboardingSaltPayAction.KYCStart -> {
            store.dispatch(OnboardingSaltPayAction.SetStep(SaltPayRegistrationStep.KycStart))
        }
        is OnboardingSaltPayAction.OnFinishKYC -> {
            scope.launch {
                registerKYCIfNeeded(getState()).successOr {
                    onException(it.error)
                }
            }
        }
        is OnboardingSaltPayAction.TrySetPin -> {
            try {
                assertPinValid(action.pin, getState().pinLength)
                store.dispatch(OnboardingSaltPayAction.SetPin(action.pin))
                store.dispatch(OnboardingSaltPayAction.SetStep(SaltPayRegistrationStep.CardRegistration))
            } catch (error: SaltPayRegistrationError) {
                handleError = error
            }
        }
        is OnboardingSaltPayAction.Update -> {
            handleIsBusy = true

            val state = getState()
            scope.launch {
                checkGasIfNeeded(state.saltPayManager, state.step).successOr {
                    onException(it.error)
                    return@launch
                }

                registerKYCIfNeeded(state).successOr {
                    onException(it.error)
                    return@launch
                }

                val newStep = checkRegistration(state).successOr {
                    onException(it.error)
                    return@launch
                }

                withMainContext {
                    handleIsBusy = false
                    store.dispatch(OnboardingSaltPayAction.SetStep(newStep))
                }
            }
        }
        else -> {
            /* do nothing, only reduce */
        }
    }
}

private var handleIsBusy: Boolean = false
    set(value) {
        field = value
        store.dispatchOnMain(OnboardingSaltPayAction.SetIsBusy(value))
    }

private suspend fun onError(error: SaltPayRegistrationError) {
    withMainContext {
        handleIsBusy = false
        handleError = error
    }
}

private suspend fun onException(error: Throwable) {
    withMainContext {
        handleIsBusy = false
        handleException = error
    }
}

private var handleException: Throwable = Throwable("Init")
    set(value) {
        field = value
        when (value) {
            is SaltPayRegistrationError -> handleError = value
            else -> {
                Timber.e(value)
                when (value) {
                    is TangemSdkError -> {
                        // store.dispatchErrorNotification(TapError.CustomError(value.customMessage))
                    }
                    else -> {
                        store.dispatchErrorNotification(
                            TapError.CustomError(value.message ?: "Unknown SaltPay exception"),
                        )
                    }
                }
            }
        }
    }

private var handleError: SaltPayRegistrationError = SaltPayRegistrationError.Unknown
    set(value) {
        field = value
        when (value) {
            SaltPayRegistrationError.NoGas -> {
                store.dispatch(OnboardingSaltPayAction.SetStep(SaltPayRegistrationStep.NoGas))
            }
            else -> {
                store.dispatchDialogShow(SaltPayDialog.RegistrationError(value))
            }
        }
    }
// [REDACTED_TODO_COMMENT]
private suspend fun checkGasIfNeeded(
    saltPayManager: SaltPayRegistrationManager,
    step: SaltPayRegistrationStep,
): Result<Unit> {
    if (step == SaltPayRegistrationStep.KycStart ||
        step == SaltPayRegistrationStep.KycWaiting ||
        step == SaltPayRegistrationStep.Finished
    ) {
        return Result.Success(Unit)
    }

    return saltPayManager.checkHasGas()
}

private suspend fun registerKYCIfNeeded(state: OnboardingSaltPayState): Result<Unit> {
    if (state.step != SaltPayRegistrationStep.KycStart) {
        return Result.Success(Unit)
    }

    withMainContext {
        store.dispatch(OnboardingSaltPayAction.SetStep(SaltPayRegistrationStep.KycWaiting))
    }
    return state.saltPayManager.registerKYC()
}

private suspend fun checkRegistration(
    state: OnboardingSaltPayState,
): Result<SaltPayRegistrationStep> {
    return try {
        val registrationResponseItem = state.saltPayManager.checkRegistration().successOr { return it }
        val step = registrationResponseItem.toSaltPayStep()
        Result.Success(step)
    } catch (ex: SaltPayRegistrationError) {
        Result.Failure(ex)
    }
}

@Throws(SaltPayRegistrationError::class)
fun RegistrationResponse.Item.toSaltPayStep(): SaltPayRegistrationStep {
    return when {
        passed != true -> throw SaltPayRegistrationError.CardNotPassed(this.error)
        disabledByAdmin == true -> throw SaltPayRegistrationError.CardDisabled(this.error)

        // go to success screen
        active == true -> SaltPayRegistrationStep.Finished

        // pinSet is false, go toPin screen
        pinSet == false -> SaltPayRegistrationStep.NeedPin

        // kycDate is set, go to kyc waiting screen
        kycDate != null -> SaltPayRegistrationStep.KycWaiting

        else -> SaltPayRegistrationStep.KycStart
    }
}

@Throws(SaltPayRegistrationError::class)
private fun assertPinValid(pin: String, pinLength: Int) {
    val array = pin.toCharArray()

    if (array.size < pinLength) throw SaltPayRegistrationError.WeakPin

    val filters: List<Filter<String>> = listOf(
        AllSymbolsTheSameFilter(),
    )

    if (filters.any { it.filter(pin) }) {
        throw SaltPayRegistrationError.WeakPin
    }
}
