package com.tangem.features.askbiometry.impl.ui.state

import com.tangem.core.ui.extensions.TextReference

internal data class AskBiometryUM(
    val bottomSheetVariant: Boolean = false,
    val showProgress: Boolean = false,
    val error: TextReference? = null,
    val onAllowClick: () -> Unit = {},
    val onDontAllowClick: () -> Unit = {},
)