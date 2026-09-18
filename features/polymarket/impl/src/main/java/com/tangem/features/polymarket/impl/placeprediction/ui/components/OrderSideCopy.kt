package com.tangem.features.polymarket.impl.placeprediction.ui.components

import androidx.annotation.StringRes
import com.tangem.core.ui.R
import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM

@StringRes
internal fun OrderSideUM.flowTitle(): Int = when (this) {
    is OrderSideUM.Buy -> R.string.prediction_place_title
    is OrderSideUM.Sell -> R.string.prediction_close_title
}