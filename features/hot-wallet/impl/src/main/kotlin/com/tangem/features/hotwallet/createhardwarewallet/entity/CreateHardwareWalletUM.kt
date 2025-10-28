package com.tangem.features.hotwallet.createhardwarewallet.entity

internal data class CreateHardwareWalletUM(
    val onBackClick: () -> Unit,
    val onBuyTangemWalletClick: () -> Unit,
    val onScanDeviceClick: () -> Unit,
    val isScanInProgress: Boolean = false,
)