package com.tangem.tap.features.scanfails.ui

internal data class ScanFailsUM(
    val isShown: Boolean = false,
    val onHowToScan: () -> Unit = {},
    val onRequestSupport: () -> Unit = {},
    val onDismiss: () -> Unit = {},
)