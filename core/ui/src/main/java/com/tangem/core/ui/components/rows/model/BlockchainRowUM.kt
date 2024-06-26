package com.tangem.core.ui.components.rows.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
data class BlockchainRowUM(
    val name: TextReference,
    val type: TextReference,
    val icon: Icon,
    val isAccented: Boolean,
    val usePrimaryTextColor: Boolean,
) {

    @Immutable
    data class Icon(
        @DrawableRes val resId: Int,
        val isColored: Boolean,
    )
}