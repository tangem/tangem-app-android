package com.tangem.core.ui.components.rows.model

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.currency.icon.CurrencyIconState

@Immutable
data class ChainRowUM(
    val name: String,
    val type: String,
    val icon: CurrencyIconState,
    val showCustom: Boolean,
)