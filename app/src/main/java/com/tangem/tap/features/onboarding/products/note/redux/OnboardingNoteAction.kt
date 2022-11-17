package com.tangem.tap.features.onboarding.products.note.redux

import com.tangem.blockchain.common.WalletManager
import com.tangem.tap.domain.TapError
import com.tangem.tap.features.onboarding.OnboardingWalletBalance
import org.rekotlin.Action

/**
 * Created by Anton Zhilenkov on 23/09/2021.
 */
sealed class OnboardingNoteAction : Action {
    // from user, ui
    object Init : OnboardingNoteAction()
    object LoadCardArtwork : OnboardingNoteAction()
    object CreateWallet : OnboardingNoteAction()
    object TopUp : OnboardingNoteAction()
    object ShowAddressInfoDialog : OnboardingNoteAction()

    // from redux
    class SetArtworkUrl(val artworkUrl: String) : OnboardingNoteAction()
    data class SetWalletManager(val walletManager: WalletManager) : OnboardingNoteAction()
    object DetermineStepOfScreen : OnboardingNoteAction()
    object Done : OnboardingNoteAction()

    data class SetStepOfScreen(val step: OnboardingNoteStep) : OnboardingNoteAction()

    sealed class Balance {
        object Update : OnboardingNoteAction()
        data class Set(val balance: OnboardingWalletBalance) : OnboardingNoteAction()
        data class SetCriticalError(val error: TapError?) : OnboardingNoteAction()
        data class SetNonCriticalError(val error: TapError?) : OnboardingNoteAction()
    }

    sealed class Confetti {
        object Show : OnboardingNoteAction()
        object Hide : OnboardingNoteAction()
    }
}
