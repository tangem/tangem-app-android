package com.tangem.tap.features.disclaimer.redux

import org.rekotlin.StateType

data class DisclaimerState(
        val accepted: Boolean = false
) : StateType

