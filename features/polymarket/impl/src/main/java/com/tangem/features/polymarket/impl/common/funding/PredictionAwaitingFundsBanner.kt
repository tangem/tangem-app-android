package com.tangem.features.polymarket.impl.common.funding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.resourceReference

@Composable
internal fun PredictionAwaitingFundsBanner(modifier: Modifier = Modifier) {
    TangemMessageBanner(
        title = resourceReference(R.string.prediction_place_notification_awaiting_funds_title),
        description = resourceReference(R.string.prediction_place_notification_awaiting_funds_description),
        variant = TangemMessageBanner.Variant.Info,
        showGlowRing = false,
        slotEnd = { TangemLoader() },
        modifier = modifier,
    )
}