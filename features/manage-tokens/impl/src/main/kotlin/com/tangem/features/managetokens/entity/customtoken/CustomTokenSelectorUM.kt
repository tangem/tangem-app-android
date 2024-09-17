package com.tangem.features.managetokens.entity.customtoken

import androidx.compose.runtime.Immutable
import com.tangem.features.managetokens.entity.item.SelectableItemUM
import kotlinx.collections.immutable.ImmutableList

internal data class CustomTokenSelectorUM(
    val header: HeaderUM,
    val items: ImmutableList<SelectableItemUM>,
) {

    @Immutable
    sealed class HeaderUM {

        data object None : HeaderUM()

        data object Description : HeaderUM()

        data class CustomDerivationButton(
            val value: String?,
            val onClick: () -> Unit,
        ) : HeaderUM()
    }
}