package com.tangem.tap.features.onboarding.products.wallet.saltPay.redux

import com.tangem.tap.features.onboarding.products.wallet.redux.OnboardingWalletState
import org.rekotlin.Action

/**
 * Created by Anton Zhilenkov on 09.10.2022.
 */
object OnboardingSaltPayReducer {
    fun reduce(action: Action, state: OnboardingWalletState): OnboardingWalletState {
        return internalReduce(action, state)
    }
}

@Suppress("ComplexMethod")
private fun internalReduce(anyAction: Action, onboardingWalletState: OnboardingWalletState): OnboardingWalletState {
    val action = anyAction as? OnboardingSaltPayAction ?: return onboardingWalletState

    if (onboardingWalletState.onboardingSaltPayState == null && action is OnboardingSaltPayAction.SetDependencies) {
        return onboardingWalletState.copy(
            onboardingSaltPayState = OnboardingSaltPayState(
                saltPayManager = action.registrationManager,
                saltPayConfig = action.saltPayConfig,
            ),
        )
    }

    val oldState = onboardingWalletState.onboardingSaltPayState ?: return onboardingWalletState

    val newState = when (action) {
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
        is OnboardingSaltPayAction.SetAmountToClaim -> {
            oldState.copy(
                amountToClaim = action.amount,
            )
        }
        is OnboardingSaltPayAction.SetTokenBalance -> {
            oldState.copy(
                tokenAmount = oldState.tokenAmount.copy(value = action.balanceValue),
            )
        }
        is OnboardingSaltPayAction.SetInProgress -> {
            oldState.copy(
                inProgress = action.isInProgress,
            )
        }
        is OnboardingSaltPayAction.SetClaimRefreshInProgress -> {
            oldState.copy(
                claimInProgress = action.isInProgress,
            )
        }
        else -> oldState
    }

    return when (newState) {
        oldState -> onboardingWalletState
        else -> {
            onboardingWalletState.copy(onboardingSaltPayState = newState)
        }
    }
}
