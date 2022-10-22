package com.tangem.tap.features.onboarding.products.wallet.saltPay.redux

import com.tangem.blockchain.common.Amount
import com.tangem.tap.features.onboarding.products.wallet.saltPay.SaltPayConfig
import com.tangem.tap.features.onboarding.products.wallet.saltPay.SaltPayRegistrationManager
import org.rekotlin.Action
import java.math.BigDecimal

/**
* [REDACTED_AUTHOR]
 */
sealed class OnboardingSaltPayAction : Action {
    sealed class Init : OnboardingSaltPayAction() {
        data class SetDependencies(
            val registrationManager: SaltPayRegistrationManager,
            val saltPayConfig: SaltPayConfig,
        ) : Init()
    }

    data class SetInProgress(val isInProgress: Boolean) : OnboardingSaltPayAction()
    data class SetClaimRefreshInProgress(val isInProgress: Boolean) : OnboardingSaltPayAction()

    data class SetAccessCode(val accessCode: String?) : OnboardingSaltPayAction()

    object Update : OnboardingSaltPayAction()
    object RegisterCard : OnboardingSaltPayAction()
    object OpenUtorgKYC : OnboardingSaltPayAction()
    object UtorgKYCRedirectSuccess : OnboardingSaltPayAction()
    object RegisterKYC : OnboardingSaltPayAction()
    object Claim : OnboardingSaltPayAction()
    object RefreshClaim : OnboardingSaltPayAction()

    data class TrySetPin(val pin: String) : OnboardingSaltPayAction()
    data class SetPin(val pin: String) : OnboardingSaltPayAction()

    data class SetStep(val newStep: SaltPayRegistrationStep? = null) : OnboardingSaltPayAction()
    data class SetAmountToClaim(val amount: Amount?) : OnboardingSaltPayAction()
    data class SetTokenBalance(val balanceValue: BigDecimal) : OnboardingSaltPayAction()
}

