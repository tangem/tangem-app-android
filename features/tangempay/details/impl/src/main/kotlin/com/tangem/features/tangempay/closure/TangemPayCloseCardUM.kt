package com.tangem.features.tangempay.closure

import androidx.compose.runtime.Immutable

@Immutable
internal data class TangemPayCloseCardUM(
    val isClosingInProgress: Boolean,
    val onCloseClick: () -> Unit,
    val onDismissRequest: () -> Unit,
)