package com.tangem.features.biometry.impl.ui.state

import com.tangem.core.ui.extensions.TextReference

internal data class AskBiometryUM(
    val isBottomSheetVariant: Boolean = false,
    val shouldShowProgress: Boolean = false,
    val error: TextReference? = null,
    val onAllowClick: () -> Unit = {},
    val onDontAllowClick: () -> Unit = {},
    val onDismiss: () -> Unit = {},
)