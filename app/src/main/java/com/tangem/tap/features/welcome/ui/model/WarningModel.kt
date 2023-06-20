package com.tangem.tap.features.welcome.ui.model

internal sealed interface WarningModel {
    data class BiometricsLockoutWarning(
        val isPermanent: Boolean,
        val onDismiss: () -> Unit,
    ) : WarningModel

    data class KeyInvalidatedWarning(
        val onDismiss: () -> Unit,
    ) : WarningModel

    data class BiometricsDisabledWarning(
        val onDismiss: () -> Unit,
    ) : WarningModel
}