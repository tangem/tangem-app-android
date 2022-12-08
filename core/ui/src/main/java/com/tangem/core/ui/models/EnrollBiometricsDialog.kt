package com.tangem.core.ui.models

import androidx.compose.runtime.Immutable

@Immutable
data class EnrollBiometricsDialog(
    val onEnroll: () -> Unit,
    val onCancel: () -> Unit,
)