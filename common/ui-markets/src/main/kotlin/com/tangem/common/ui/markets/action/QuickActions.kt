package com.tangem.common.ui.markets.action

import kotlinx.collections.immutable.ImmutableList

data class QuickActions(
    val actions: ImmutableList<QuickActionUM>,
    val onQuickActionClick: (QuickActionUM) -> Unit,
    val onQuickActionLongClick: (QuickActionUM) -> Unit,
)