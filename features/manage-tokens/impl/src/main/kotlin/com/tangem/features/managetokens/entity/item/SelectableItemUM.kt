package com.tangem.features.managetokens.entity.item

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface SelectableItemUM {

    val id: String
    val isSelected: Boolean
    val onSelectedStateChange: (Boolean) -> Unit
}