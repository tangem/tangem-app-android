package com.tangem.features.polymarket.impl.placeprediction.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionNotificationUM
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun PredictionNotificationsBlock(
    notifications: ImmutableList<PredictionNotificationUM>,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
        notifications.forEach { notification ->
            TangemMessageBanner(
                title = notification.title(),
                description = notification.description(),
                variant = if (notification.isBlocking) {
                    TangemMessageBanner.Variant.Error
                } else {
                    TangemMessageBanner.Variant.Warning
                },
                primaryButton = if (notification is PredictionNotificationUM.QuoteFailed) {
                    TangemMessageBanner.Button(
                        text = resourceReference(R.string.common_retry),
                        onClick = onRetryClick,
                    )
                } else {
                    null
                },
            )
        }
    }
}

private fun PredictionNotificationUM.title(): TextReference = when (this) {
    PredictionNotificationUM.PartialFill -> resourceReference(R.string.prediction_place_notification_partial_title)
    PredictionNotificationUM.NoLiquidity ->
        resourceReference(R.string.prediction_place_notification_no_liquidity_title)
    PredictionNotificationUM.BelowMinOrderSize ->
        resourceReference(R.string.prediction_place_notification_min_order_title)
    PredictionNotificationUM.InsufficientBalance -> resourceReference(R.string.common_insufficient_balance)
    PredictionNotificationUM.MarketClosed ->
        resourceReference(R.string.prediction_place_notification_market_closed_title)
    PredictionNotificationUM.RegionRestricted -> resourceReference(R.string.prediction_region_restrictions_title)
    PredictionNotificationUM.QuoteFailed -> resourceReference(R.string.prediction_place_notification_quote_error_title)
}

private fun PredictionNotificationUM.description(): TextReference? = when (this) {
    PredictionNotificationUM.PartialFill ->
        resourceReference(R.string.prediction_place_notification_partial_description)
    PredictionNotificationUM.NoLiquidity ->
        resourceReference(R.string.prediction_place_notification_no_liquidity_description)
    PredictionNotificationUM.BelowMinOrderSize ->
        resourceReference(R.string.prediction_place_notification_min_order_description)
    PredictionNotificationUM.InsufficientBalance -> null
    PredictionNotificationUM.MarketClosed ->
        resourceReference(R.string.prediction_place_notification_market_closed_description)
    PredictionNotificationUM.RegionRestricted -> resourceReference(R.string.prediction_region_restrictions_subtitle)
    PredictionNotificationUM.QuoteFailed ->
        resourceReference(R.string.prediction_place_notification_quote_error_description)
}