package com.tangem.features.hotwallet.upgradewallet.entity

internal data class UpgradeWalletUM(
    val onBackClick: () -> Unit,
    val onBuyTangemWalletClick: () -> Unit,
    val onScanDeviceClick: () -> Unit,
)