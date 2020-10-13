package com.tangem.tap.features.disclaimer.redux

import org.rekotlin.Action

sealed class DisclaimerAction: Action {
    object AcceptDisclaimer : DisclaimerAction()
    object ShowAcceptedDisclaimer : DisclaimerAction()
}