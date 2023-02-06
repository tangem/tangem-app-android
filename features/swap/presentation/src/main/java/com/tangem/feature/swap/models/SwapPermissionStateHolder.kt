package com.tangem.feature.swap.models

sealed class SwapPermissionState {

    object InProgress : SwapPermissionState()

    object Empty : SwapPermissionState()

    data class ReadyForRequest(
        val currency: String,
        val amount: String,
        val walletAddress: String,
        val spenderAddress: String,
        val fee: String,
        val approveButton: ApprovePermissionButton,
        val cancelButton: CancelPermissionButton,
    ) : SwapPermissionState()
}

data class ApprovePermissionButton(
    val enabled: Boolean,
    val loading: Boolean = false,
    val onClick: () -> Unit,
)

data class CancelPermissionButton(
    val enabled: Boolean,
    val onClick: () -> Unit,
)
