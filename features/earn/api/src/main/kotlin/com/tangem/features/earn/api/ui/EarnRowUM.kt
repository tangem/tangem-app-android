package com.tangem.features.earn.api.ui

import com.tangem.core.ui.extensions.TextReference

/** @property onValueClick not `null` for a link: the value is clickable and has an arrow after it */
data class EarnRowUM(
    val label: TextReference,
    val value: TextReference,
    val onInfoClick: (() -> Unit)?,
    val onValueClick: (() -> Unit)? = null,
)