package com.tangem.common.ui.bottomsheet.permission.state

import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

sealed class GiveTxPermissionState {

    data object InProgress : GiveTxPermissionState()

    data object Empty : GiveTxPermissionState()

    data class ReadyForRequest(
        val subtitle: TextReference,
        val dialogText: TextReference,
        val currency: String,
        val amount: String,
        val walletAddress: String,
        val spenderAddress: String,
        val fee: TextReference,
        val approveType: ApproveType,
        val approveItems: ImmutableList<ApproveType> = ApproveType.entries.toImmutableList(),
        val approveButton: ApprovePermissionButton,
        val cancelButton: CancelPermissionButton,
        val onChangeApproveType: ((ApproveType) -> Unit)? = null,
    ) : GiveTxPermissionState()
}

enum class ApproveType {
    LIMITED, UNLIMITED
}

data class ApprovePermissionButton(
    val enabled: Boolean,
    val loading: Boolean = false,
    val onClick: () -> Unit,
)

data class CancelPermissionButton(
    val enabled: Boolean,
)
