package com.tangem.features.hotwallet.forgetwallet.entity

internal data class ForgetWalletUM(
    val onBackClick: () -> Unit,
    val isCheckboxChecked: Boolean,
    val onCheckboxClick: () -> Unit,
    val onForgetWalletClick: () -> Unit,
    val isForgetButtonEnabled: Boolean,
)