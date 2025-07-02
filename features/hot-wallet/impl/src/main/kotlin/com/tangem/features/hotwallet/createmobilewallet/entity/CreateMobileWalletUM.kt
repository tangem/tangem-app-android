package com.tangem.features.hotwallet.createmobilewallet.entity

internal data class CreateMobileWalletUM(
    val onBackClick: () -> Unit,
    val onCreateClick: () -> Unit,
)