package com.tangem.tap.features.welcome.redux

import android.content.Intent
import com.tangem.common.core.TangemError
import kotlinx.coroutines.CoroutineScope
import org.rekotlin.StateType

data class WelcomeState(
    val scope: CoroutineScope? = null,
    val isUnlockWithBiometricsInProgress: Boolean = false,
    val isUnlockWithCardInProgress: Boolean = false,
    val intent: Intent? = null,
    val error: TangemError? = null,
) : StateType