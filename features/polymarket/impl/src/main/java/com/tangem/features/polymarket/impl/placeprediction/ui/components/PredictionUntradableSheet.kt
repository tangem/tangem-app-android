package com.tangem.features.polymarket.impl.placeprediction.ui.components

import androidx.compose.runtime.Composable
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.polymarket.impl.common.ui.PolymarketMarketClosedSheet
import com.tangem.features.polymarket.impl.common.ui.PolymarketNoticeSheet
import com.tangem.features.polymarket.impl.placeprediction.entity.PriceErrorUM

@Composable
internal fun PredictionUntradableSheet(priceError: PriceErrorUM?, onDismiss: () -> Unit) {
    PolymarketMarketClosedSheet(isShown = priceError == PriceErrorUM.MarketClosed, onDismiss = onDismiss)
    PolymarketNoticeSheet(
        isShown = priceError == PriceErrorUM.Unknown,
        title = resourceReference(R.string.common_something_went_wrong),
        description = resourceReference(R.string.prediction_place_notification_quote_error_description),
        onDismiss = onDismiss,
    )
}