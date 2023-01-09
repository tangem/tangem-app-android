package com.tangem.tap.features.welcome.ui

import androidx.compose.runtime.Immutable
import com.tangem.tap.features.details.ui.cardsettings.TextReference

@Immutable
internal data class WelcomeScreenState(
    val showUnlockWithBiometricsProgress: Boolean = false,
    val showUnlockWithCardProgress: Boolean = false,
    val error: TextReference? = null,
)
