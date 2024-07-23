package com.tangem.features.managetokens.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.rows.model.BlockchainRowUM

@Immutable
internal data class CurrencyNetworkUM(
    val id: String,
    val model: BlockchainRowUM,
    val isSelected: Boolean,
    val onSelectedStateChange: (Boolean) -> Unit,
)