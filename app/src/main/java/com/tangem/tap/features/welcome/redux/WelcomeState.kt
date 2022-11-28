package com.tangem.tap.features.welcome.redux

import com.tangem.common.core.TangemError
import org.rekotlin.StateType

data class WelcomeState(
    val isUnlockWithBiometricsInProgress: Boolean = false,
    val isUnlockWithCardInProgress: Boolean = false,
    val error: TangemError? = null,
) : StateType