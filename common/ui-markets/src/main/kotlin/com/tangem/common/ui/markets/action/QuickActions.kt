package com.tangem.common.ui.markets.action

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

data class QuickActions(
    val actions: ImmutableList<QuickActionUM>,
    val onQuickActionClick: (QuickActionUM) -> Unit,
    val onQuickActionLongClick: (QuickActionUM) -> Unit,
    val disabledActions: ImmutableSet<QuickActionUM> = persistentSetOf(),
)