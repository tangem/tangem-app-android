package com.tangem.tap.features.disclaimer.redux

import com.tangem.common.extensions.VoidCallback
import org.rekotlin.Action

sealed class DisclaimerAction : Action {

    data class SetDisclaimerType(
        val type: DisclaimerType,
    ) : DisclaimerAction()

    data class Show(val onAcceptCallback: VoidCallback? = null) : DisclaimerAction()
    data class AcceptDisclaimer(val type: DisclaimerType) : DisclaimerAction()

    internal data class UpdateState(val type: DisclaimerType, val accepted: Boolean) : DisclaimerAction()
    internal data class SetOnAcceptCallback(val onAcceptCallback: VoidCallback? = null) : DisclaimerAction()
}