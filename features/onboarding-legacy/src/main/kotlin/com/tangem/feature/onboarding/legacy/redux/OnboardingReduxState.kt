package com.tangem.feature.onboarding.legacy.redux

import com.tangem.feature.onboarding.legacy.redux.products.OnboardingManager
import com.tangem.feature.onboarding.legacy.redux.products.note.redux.OnboardingNoteMiddleware
import com.tangem.feature.onboarding.legacy.redux.products.note.redux.OnboardingNoteState
import com.tangem.feature.onboarding.legacy.redux.products.wallet.redux.OnboardingWalletState
import org.rekotlin.Middleware
import org.rekotlin.StateType

internal data class OnboardingReduxState(
    val onboardingState: OnboardingState = OnboardingState(),
    val daggerGraphState: DaggerGraphState = DaggerGraphState(),
    val onboardingNoteState: OnboardingNoteState = OnboardingNoteState(),
    val onboardingWalletState: OnboardingWalletState = OnboardingWalletState(),
    // val onboardingOtherCardsState: OnboardingOtherCardsState = OnboardingOtherCardsState(),
) : StateType {

    companion object {
        fun getMiddleware(): List<Middleware<OnboardingReduxState>> {
            return listOf(
                OnboardingNoteMiddleware.handler,
            )
        }
    }
}

internal data class OnboardingState(
    val onboardingStarted: Boolean = false,
    val onboardingManager: OnboardingManager? = null,
    val shouldResetOnCreate: Boolean = false,
)
