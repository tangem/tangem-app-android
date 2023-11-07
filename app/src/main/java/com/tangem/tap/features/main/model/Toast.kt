package com.tangem.tap.features.main.model

import com.tangem.core.ui.extensions.TextReference

internal data class Toast(
    val message: TextReference,
    val action: ActionConfig,
)