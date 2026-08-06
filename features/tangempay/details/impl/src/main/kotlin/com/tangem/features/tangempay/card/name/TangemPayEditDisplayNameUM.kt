package com.tangem.features.tangempay.card.name

import androidx.compose.ui.text.input.TextFieldValue

internal data class TangemPayEditDisplayNameUM(
    val editingValue: TextFieldValue,
    val isLoading: Boolean,
    val isDoneEnabled: Boolean,
    val onValueChanged: (TextFieldValue) -> Unit,
    val onDoneClick: () -> Unit,
    val onDismiss: () -> Unit,
)