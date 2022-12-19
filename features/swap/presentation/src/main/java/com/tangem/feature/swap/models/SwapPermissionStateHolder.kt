package com.tangem.feature.swap.models

data class SwapPermissionStateHolder(
    val currency: String,
    val amount: String,
    val walletAddress: String,
    val spenderAddress: String,
    val fee: String,
    val approveButton: ApprovePermissionButton,
    val cancelButton: CancelPermissionButton,
)

data class ApprovePermissionButton(
    val enabled: Boolean,
    val loading: Boolean = false,
    val onClick: () -> Unit,
)

data class CancelPermissionButton(
    val enabled: Boolean,
    val onClick: () -> Unit,
)
