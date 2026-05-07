package com.tangem.features.tangempay.entity

import androidx.compose.ui.text.input.TextFieldValue

internal data class TangemPayEditDisplayNameUM(
    val editingValue: TextFieldValue,
    val isLoading: Boolean,
    val onValueChanged: (TextFieldValue) -> Unit,
    val onDoneClick: () -> Unit,
    val onDismiss: () -> Unit,
)