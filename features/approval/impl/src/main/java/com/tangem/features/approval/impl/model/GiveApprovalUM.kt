package com.tangem.features.approval.impl.model

import androidx.annotation.DrawableRes
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal data class GiveApprovalUM(
    val approveType: ApproveType,
    val approveItems: ImmutableList<ApproveType> = ApproveType.entries.toImmutableList(),
    @DrawableRes val walletInteractionIcon: Int?,
    val isApproveButtonEnabled: Boolean,
    val isApproveLoading: Boolean,
    val isHoldToConfirm: Boolean = false,
    val isResetApproval: Boolean = false,
)