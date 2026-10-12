package com.tangem.features.hotwallet.forgetwallet.entity

import com.tangem.core.ui.extensions.TextReference

internal data class ForgetWalletUM(
    val onBackClick: () -> Unit,
    val description: TextReference,
    val buttonText: TextReference,
    val isCheckboxChecked: Boolean,
    val onCheckboxClick: () -> Unit,
    val onForgetWalletClick: () -> Unit,
    val isForgetButtonEnabled: Boolean,
    val isInProgress: Boolean = false,
)