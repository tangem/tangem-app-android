package com.tangem.features.approval.impl.model

import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal data class SelectApprovalTypeUM(
    val subtitle: TextReference,
    val approveType: ApproveType,
    val approveItems: ImmutableList<ApproveType> = ApproveType.entries.toImmutableList(),
)