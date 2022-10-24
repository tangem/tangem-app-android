package com.tangem.tap.features.onboarding.products.wallet.saltPay.dialog

import com.tangem.common.extensions.VoidCallback
import com.tangem.tap.common.redux.StateDialog
import com.tangem.tap.features.onboarding.products.wallet.saltPay.message.SaltPayRegistrationError

/**
[REDACTED_AUTHOR]
 */
sealed class SaltPayDialog : StateDialog {
    sealed class Activation : SaltPayDialog() {
        object NoFunds : SaltPayDialog()
        data class OnError(val error: SaltPayRegistrationError) : SaltPayDialog()
        data class TryToInterrupt(val onOk: VoidCallback, val onCancel: VoidCallback) : SaltPayDialog()
    }
}