package com.tangem.features.tangempay.entity

internal data class TangemPayEditDisplayNameUM(
    val editingValue: String,
    val isLoading: Boolean,
    val onValueChanged: (String) -> Unit,
    val onDoneClick: () -> Unit,
    val onDismiss: () -> Unit,
)