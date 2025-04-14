package com.tangem.tap.features.welcome.redux

import android.content.Intent
import com.tangem.common.core.TangemError
import org.rekotlin.StateType

data class WelcomeState(
    val isUnlockWithBiometricsInProgress: Boolean = false,
    val isUnlockWithCardInProgress: Boolean = false,
    val intent: Intent? = null,
    val error: TangemError? = null,
) : StateType
