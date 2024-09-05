package com.tangem.core.ui.components.rows.model

import androidx.compose.runtime.Immutable

@Immutable
data class BlockchainRowUM(
    val id: String,
    val name: String,
    val type: String,
    val iconResId: Int,
    val isMainNetwork: Boolean,
    val isSelected: Boolean,
    val isEnabled: Boolean = true,
)