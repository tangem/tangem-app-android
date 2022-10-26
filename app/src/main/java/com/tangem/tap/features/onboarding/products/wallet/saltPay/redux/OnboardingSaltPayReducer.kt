package com.tangem.tap.features.onboarding.products.wallet.saltPay.redux

import com.tangem.tap.features.onboarding.products.wallet.redux.BackupStep
import com.tangem.tap.features.onboarding.products.wallet.redux.OnboardingWalletState
import com.tangem.tap.features.onboarding.products.wallet.redux.OnboardingWalletStep
import org.rekotlin.Action

/**
[REDACTED_AUTHOR]
 */
class OnboardingSaltPayReducer {
    companion object {
        fun reduce(action: Action, state: OnboardingWalletState): OnboardingWalletState {
            return internalReduce(action, state)
        }
    }
}

private fun internalReduce(anyAction: Action, onboardingWalletState: OnboardingWalletState): OnboardingWalletState {
    val action = anyAction as? OnboardingSaltPayAction ?: return onboardingWalletState

    if (onboardingWalletState.onboardingSaltPayState == null && action is OnboardingSaltPayAction.Init.SetDependencies) {
        return onboardingWalletState.copy(
            onboardingSaltPayState = OnboardingSaltPayState(
                saltPayManager = action.registrationManager,
                saltPayConfig = action.saltPayConfig,
            ),
        )
    }

    val oldState = onboardingWalletState.onboardingSaltPayState ?: return onboardingWalletState

    val newState = when (action) {
        OnboardingSaltPayAction.Init.DiscardBackupSteps -> {
            return onboardingWalletState.copy(
                step = OnboardingWalletStep.SaltPay,
                backupState = onboardingWalletState.backupState.copy(
                    backupStep = BackupStep.Finished,
                    maxBackupCards = 1,
                    canSkipBackup = false,
                ),
                onboardingSaltPayState = oldState.copy(
                    isTest = true,
                    step = SaltPayRegistrationStep.NeedPin,
                ),
            )
        }
        is OnboardingSaltPayAction.SetAccessCode -> {
            oldState.copy(
                accessCode = action.accessCode,
            )
        }
        is OnboardingSaltPayAction.SetPin -> {
            oldState.copy(pinCode = action.pin)
        }
        is OnboardingSaltPayAction.SetStep -> {
            if (action.newStep == null) {
                oldState
            } else {
                oldState.copy(
                    step = action.newStep,
                )
            }
        }
        is OnboardingSaltPayAction.SetIsBusy -> {
            oldState.copy(
                isBusy = action.isBusy,
            )
        }
        else -> oldState
        // is OnboardingSaltPayAction.Init.PrepareAndSetDependencies -> oldState
        // OnboardingSaltPayAction.OnCreate -> oldState
        // is OnboardingSaltPayAction.OnFinishKYC -> oldState
        // is OnboardingSaltPayAction.Register -> oldState
        // is OnboardingSaltPayAction.TrySetPin -> oldState
        // is OnboardingSaltPayAction.Update -> oldState
        // is OnboardingSaltPayAction.TryUpdateStep -> oldState
    }

    return when (newState) {
        oldState -> onboardingWalletState
        else -> {
            onboardingWalletState.copy(onboardingSaltPayState = newState)
        }
    }
}