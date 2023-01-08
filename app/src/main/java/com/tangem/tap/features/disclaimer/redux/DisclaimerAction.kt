package com.tangem.tap.features.disclaimer.redux

import com.tangem.common.extensions.VoidCallback
import org.rekotlin.Action

sealed class DisclaimerAction : Action {

    data class SetDisclaimerType(
        val type: DisclaimerType,
    ) : DisclaimerAction()

    data class Show(
        val callback: DisclaimerCallback? = null,
    ) : DisclaimerAction()

    data class AcceptDisclaimer(val type: DisclaimerType) : DisclaimerAction()

    internal data class UpdateState(val type: DisclaimerType, val accepted: Boolean) : DisclaimerAction()

    object OnBackPressed : DisclaimerAction()
}

data class DisclaimerCallback(
    val onAccept: VoidCallback? = null,
    val onDismiss: VoidCallback? = null,
)