package com.tangem.features.hotwallet.createmobilewallet.entity

internal data class CreateMobileWalletUM(
    val isCreateButtonLoading: Boolean,
    val onBackClick: () -> Unit,
    val onImportClick: () -> Unit,
    val onCreateClick: () -> Unit,
    val onTermsClick: () -> Unit,
)