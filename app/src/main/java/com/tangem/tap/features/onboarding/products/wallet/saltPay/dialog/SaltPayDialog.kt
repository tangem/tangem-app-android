package com.tangem.tap.features.onboarding.products.wallet.saltPay.dialog

import com.tangem.tap.common.redux.StateDialog
import com.tangem.tap.features.onboarding.products.wallet.saltPay.message.SaltPayActivationError

/**
 * Created by Anton Zhilenkov on 12.10.2022.
 */
sealed class SaltPayDialog : StateDialog {
    sealed class Activation : SaltPayDialog() {
        object NoGas : SaltPayDialog()
        object PutVisaCard : SaltPayDialog()
        data class OnError(val error: SaltPayActivationError) : SaltPayDialog()
    }
}
