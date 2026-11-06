package com.tangem.features.earn.api.ui

import com.tangem.core.ui.extensions.TextReference

data class EarnRowUM(
    val label: TextReference,
    val value: TextReference,
    val onInfoClick: (() -> Unit)?,
    val onValueClick: (() -> Unit)? = null,
)