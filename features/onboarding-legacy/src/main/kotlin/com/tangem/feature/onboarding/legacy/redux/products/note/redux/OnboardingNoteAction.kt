package com.tangem.feature.onboarding.legacy.redux.products.note.redux

import com.tangem.blockchain.common.WalletManager
import com.tangem.feature.onboarding.legacy.redux.products.OnboardingWalletBalance
import com.tangem.sdk.api.TapError
import org.rekotlin.Action

internal sealed class OnboardingNoteAction : Action {
    // from user, ui
    data object Init : OnboardingNoteAction()
    data object LoadCardArtwork : OnboardingNoteAction()
    data object CreateWallet : OnboardingNoteAction()
    data object TopUp : OnboardingNoteAction()
    data object ShowAddressInfoDialog : OnboardingNoteAction()
    data object OnBackPressed : OnboardingNoteAction()

    // from redux
    class SetArtworkUrl(val artworkUrl: String) : OnboardingNoteAction()
    data class SetWalletManager(val walletManager: WalletManager) : OnboardingNoteAction()
    data object DetermineStepOfScreen : OnboardingNoteAction()
    data object Done : OnboardingNoteAction()

    data class SetStepOfScreen(val step: OnboardingNoteStep) : OnboardingNoteAction()

    sealed class Balance {
        data object Update : OnboardingNoteAction()
        data class Set(val balance: OnboardingWalletBalance) : OnboardingNoteAction()
        data class SetCriticalError(val error: TapError?) : OnboardingNoteAction()
        data class SetNonCriticalError(val error: TapError?) : OnboardingNoteAction()
    }

    sealed class Confetti {
        data object Show : OnboardingNoteAction()
        data object Hide : OnboardingNoteAction()
    }
}