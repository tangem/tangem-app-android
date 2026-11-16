package com.tangem.features.polymarket.impl.placeprediction.entity

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class PredictionFeeSheetUM(val title: TextReference, val rows: ImmutableList<Row>) {

    @Immutable
    data class Row(
        val title: TextReference,
        val amount: String,
        val description: TextReference? = null,
    )

    enum class Heading(
        @StringRes val titleRes: Int,
        @StringRes val buyOrderRowTitleRes: Int,
        @StringRes val sellOrderRowTitleRes: Int,
    ) {
        Fee(
            titleRes = R.string.prediction_place_fee,
            buyOrderRowTitleRes = R.string.prediction_fee_sheet_order,
            sellOrderRowTitleRes = R.string.prediction_fee_sheet_order,
        ),
        Total(
            titleRes = R.string.prediction_fee_sheet_total,
            buyOrderRowTitleRes = R.string.prediction_fee_sheet_order,
            sellOrderRowTitleRes = R.string.prediction_close_title,
        ),
    }
}