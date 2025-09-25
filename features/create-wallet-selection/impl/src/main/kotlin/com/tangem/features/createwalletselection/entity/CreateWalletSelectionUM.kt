package com.tangem.features.createwalletselection.entity

internal data class CreateWalletSelectionUM(
    val isScanInProgress: Boolean = false,
    val hardwareWalletPrice: String = "$54.90",
    val showAlreadyHaveWallet: Boolean = false,
    val onBackClick: () -> Unit,
    val onMobileWalletClick: () -> Unit,
    val onHardwareWalletClick: () -> Unit,
    val onScanClick: () -> Unit,
)