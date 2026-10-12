package com.tangem.features.polymarket.impl.placeprediction.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class PredictionFeeSheetUM(val rows: ImmutableList<Row>) {

    @Immutable
    data class Row(
        val title: TextReference,
        val amount: String,
        val description: TextReference? = null,
    )
}