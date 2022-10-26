package com.tangem.tap.features.onboarding.products.wallet.saltPay.redux

import com.tangem.tap.features.onboarding.products.wallet.saltPay.SaltPayConfig
import com.tangem.tap.features.onboarding.products.wallet.saltPay.SaltPayRegistrationManager
import org.rekotlin.Action

/**
[REDACTED_AUTHOR]
 */
sealed class OnboardingSaltPayAction : Action {
    sealed class Init : OnboardingSaltPayAction() {
        data class SetDependencies(
            val registrationManager: SaltPayRegistrationManager,
            val saltPayConfig: SaltPayConfig,
        ) : Init()

        // only for tests
        object DiscardBackupSteps : Init()
    }

    data class SetIsBusy(val isBusy: Boolean) : OnboardingSaltPayAction()
    data class SetAccessCode(val accessCode: String?) : OnboardingSaltPayAction()

    object Update : OnboardingSaltPayAction()
    object RegisterCard : OnboardingSaltPayAction()
    object KYCStart : OnboardingSaltPayAction()
    object OnFinishKYC : OnboardingSaltPayAction()

    data class TrySetPin(val pin: String) : OnboardingSaltPayAction()
    data class SetPin(val pin: String) : OnboardingSaltPayAction()

    data class SetStep(val newStep: SaltPayRegistrationStep? = null) : OnboardingSaltPayAction()
}
