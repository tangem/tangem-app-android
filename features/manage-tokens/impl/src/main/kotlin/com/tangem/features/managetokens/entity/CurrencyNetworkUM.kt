package com.tangem.features.managetokens.entity

import androidx.compose.runtime.Immutable
import com.tangem.domain.tokens.model.Network

@Immutable
internal data class CurrencyNetworkUM(
    val id: Network.ID,
    val name: String,
    val type: String,
    val iconResId: Int,
    val isMainNetwork: Boolean,
    val isSelected: Boolean,
    val onSelectedStateChange: (Boolean) -> Unit,
)