package com.tangem.tap.features.onboarding.products.wallet.saltPay.dialog

import com.tangem.tap.common.redux.StateDialog
import com.tangem.tap.features.onboarding.products.wallet.saltPay.message.SaltPayRegistrationError

/**
[REDACTED_AUTHOR]
 */
sealed class SaltPayDialog : StateDialog {
    object NoFundsForActivation : SaltPayDialog()
    data class RegistrationError(val error: SaltPayRegistrationError) : SaltPayDialog()
}