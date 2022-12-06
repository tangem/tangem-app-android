package com.tangem.tap.features.saveWallet.ui

import androidx.compose.runtime.Immutable
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.tap.features.saveWallet.ui.models.EnrollBiometricsDialog

@Immutable
internal data class SaveWalletScreenState(
    val showProgress: Boolean = false,
    val enrollBiometricsDialog: EnrollBiometricsDialog? = null,
    val error: TextReference? = null,
)
