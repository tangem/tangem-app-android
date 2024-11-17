package com.tangem.common.ui.bottomsheet.permission.state

import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

sealed class GiveTxPermissionState {

    data object InProgress : GiveTxPermissionState()

    data object Empty : GiveTxPermissionState()

    data class ReadyForRequest(
        val subtitle: TextReference,
        val dialogText: TextReference,
        val footerText: TextReference,
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

    fun GiveTxPermissionState.getApproveTypeOrNull(): ApproveType? {
        return (this as? ReadyForRequest)?.approveType
    }
}

enum class ApproveType(val text: TextReference) {
    LIMITED(resourceReference(R.string.give_permission_current_transaction)),
    UNLIMITED(resourceReference(R.string.give_permission_unlimited)),
}

data class ApprovePermissionButton(
    val enabled: Boolean,
    val loading: Boolean = false,
    val onClick: () -> Unit,
)

data class CancelPermissionButton(
    val enabled: Boolean,
)