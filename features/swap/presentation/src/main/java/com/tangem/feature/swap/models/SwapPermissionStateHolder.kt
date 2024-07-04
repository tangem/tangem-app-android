package com.tangem.feature.swap.models

import com.tangem.core.ui.extensions.TextReference
import com.tangem.feature.swap.domain.models.domain.SwapApproveType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

sealed class SwapPermissionState {

    object InProgress : SwapPermissionState()

    object Empty : SwapPermissionState()

    data class ReadyForRequest(
        val providerName: String,
        val currency: String,
        val amount: String,
        val walletAddress: String,
        val spenderAddress: String,
        val fee: TextReference,
        val approveType: ApproveType,
        val approveItems: ImmutableList<ApproveType> = ApproveType.values().toList().toImmutableList(),
        val approveButton: ApprovePermissionButton,
        val cancelButton: CancelPermissionButton,
        val onChangeApproveType: (ApproveType) -> Unit,
    ) : SwapPermissionState()
}

enum class ApproveType {
    LIMITED, UNLIMITED
}

fun ApproveType.toDomainApproveType(): SwapApproveType {
    return when (this) {
        ApproveType.LIMITED -> SwapApproveType.LIMITED
        ApproveType.UNLIMITED -> SwapApproveType.UNLIMITED
    }
}

data class ApprovePermissionButton(
    val enabled: Boolean,
    val loading: Boolean = false,
    val onClick: () -> Unit,
)

data class CancelPermissionButton(
    val enabled: Boolean,
)
