package com.tangem.features.hotwallet.createmobilewallet.entity

internal data class CreateMobileWalletUM(
    val createButtonLoading: Boolean,
    val onBackClick: () -> Unit,
    val onCreateClick: () -> Unit,
)