package com.tangem.core.ui.components.chip.entity

import com.tangem.core.ui.extensions.TextReference

data class ChipUM(
    val id: Int,
    val text: TextReference,
    val isSelected: Boolean = false,
    val onClick: () -> Unit,
)