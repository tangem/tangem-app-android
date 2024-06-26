package com.tangem.features.managetokens.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.rows.model.ChainRowUM

@Immutable
internal sealed class CurrencyItemUM {

    abstract val id: String

    data class Basic(
        override val id: String,
        val model: ChainRowUM,
        val isExpanded: Boolean,
        val onExpandClick: () -> Unit,
    ) : CurrencyItemUM()

    data class Custom(
        override val id: String,
        val model: ChainRowUM,
        val onRemoveClick: () -> Unit,
    ) : CurrencyItemUM()
}