package com.tangem.tap.features.main.model

import com.tangem.core.ui.extensions.TextReference

internal data class ActionConfig(
    val text: TextReference,
    val onClick: () -> Unit,
)