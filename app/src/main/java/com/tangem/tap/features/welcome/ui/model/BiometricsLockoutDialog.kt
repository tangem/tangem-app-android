package com.tangem.tap.features.welcome.ui.model

internal data class BiometricsLockoutDialog(
    val isPermanent: Boolean,
    val onDismiss: () -> Unit,
)
