package com.tangem.tap.features.welcome.ui

import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.tap.features.welcome.ui.model.WarningModel

internal data class WelcomeScreenState(
    val onPopBack: () -> Unit = {},
    val showUnlockWithBiometricsProgress: Boolean = false,
    val showUnlockWithCardProgress: Boolean = false,
    val warning: WarningModel? = null,
    val error: TextReference? = null,
    val onUnlockClick: () -> Unit = {},
    val onScanCardClick: () -> Unit = {},
    val onCloseError: () -> Unit = {},
)