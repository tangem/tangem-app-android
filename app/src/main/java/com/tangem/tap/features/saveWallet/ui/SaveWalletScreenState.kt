package com.tangem.tap.features.saveWallet.ui

import androidx.compose.runtime.Immutable
import com.tangem.common.core.TangemError
import com.tangem.core.ui.models.EnrollBiometricsDialog

@Immutable
internal data class SaveWalletScreenState(
    val showProgress: Boolean = false,
    val enrollBiometricsDialog: EnrollBiometricsDialog? = null,
    val error: TangemError? = null,
)