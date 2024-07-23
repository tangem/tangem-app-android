package com.tangem.tap.features.disclaimer.redux

import com.tangem.common.extensions.VoidCallback

import com.tangem.tap.common.entities.ProgressState
import com.tangem.tap.features.disclaimer.Disclaimer
import com.tangem.tap.features.disclaimer.DummyDisclaimer
import org.rekotlin.StateType

data class DisclaimerState(
    val disclaimer: Disclaimer = DummyDisclaimer(),
    val showedFrom: DisclaimerSource = DisclaimerSource.Home,
    val callback: DisclaimerCallback? = null,
    val progressState: ProgressState? = null,
) : StateType

data class DisclaimerCallback(
    val onAccept: VoidCallback? = null,
    val onDismiss: VoidCallback? = null,
)