package com.tangem.features.hotwallet.forgetwallet.entity

internal data class ForgetWalletUM(
    val onBackClick: () -> Unit,
    val firstCheckboxChecked: Boolean,
    val secondCheckboxChecked: Boolean,
    val onFirstCheckboxClick: () -> Unit,
    val onSecondCheckboxClick: () -> Unit,
    val onForgetWalletClick: () -> Unit,
    val isForgetButtonEnabled: Boolean,
)