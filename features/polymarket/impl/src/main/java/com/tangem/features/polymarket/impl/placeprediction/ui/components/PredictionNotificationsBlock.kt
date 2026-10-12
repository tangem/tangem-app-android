package com.tangem.features.polymarket.impl.placeprediction.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_24
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionNotificationUM
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun PredictionNotificationsBlock(
    notifications: ImmutableList<PredictionNotificationUM>,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Emitting an empty column would still take a slot in the parent's spacing, doubling the gap below the amount.
    if (notifications.isEmpty()) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
        notifications.forEach { notification ->
            TangemMessageBanner(
                title = notification.title(),
                description = notification.description(),
                variant = notification.variant(),
                showGlowRing = notification !is PredictionNotificationUM.AwaitingFunds,
                primaryButton = if (notification.isRetryable()) {
                    TangemMessageBanner.Button(
                        text = resourceReference(R.string.common_retry),
                        onClick = onRetryClick,
                    )
                } else {
                    null
                },
                slotEnd = notification.slotEnd(),
            )
        }
    }
}

private fun PredictionNotificationUM.isRetryable(): Boolean =
    this is PredictionNotificationUM.QuoteFailed || this is PredictionNotificationUM.PositionUnavailable

private fun PredictionNotificationUM.title(): TextReference = when (this) {
    PredictionNotificationUM.AwaitingFunds ->
        resourceReference(R.string.prediction_place_notification_awaiting_funds_title)
    PredictionNotificationUM.PartialFill -> resourceReference(R.string.prediction_place_notification_partial_title)
    PredictionNotificationUM.NoLiquidity ->
        resourceReference(R.string.prediction_place_notification_no_liquidity_title)
    PredictionNotificationUM.MarketClosed ->
        resourceReference(R.string.prediction_place_notification_market_closed_title)
    PredictionNotificationUM.RegionRestricted -> resourceReference(R.string.prediction_region_restrictions_title)
    PredictionNotificationUM.QuoteFailed -> resourceReference(R.string.prediction_place_notification_quote_error_title)
    PredictionNotificationUM.PriceFailed,
    PredictionNotificationUM.PositionUnavailable,
    -> resourceReference(R.string.common_something_went_wrong)
}

private fun PredictionNotificationUM.description(): TextReference? = when (this) {
    PredictionNotificationUM.AwaitingFunds ->
        resourceReference(R.string.prediction_place_notification_awaiting_funds_description)
    PredictionNotificationUM.PartialFill ->
        resourceReference(R.string.prediction_place_notification_partial_description)
    PredictionNotificationUM.NoLiquidity ->
        resourceReference(R.string.prediction_place_notification_no_liquidity_description)
    PredictionNotificationUM.MarketClosed ->
        resourceReference(R.string.prediction_place_notification_market_closed_description)
    PredictionNotificationUM.RegionRestricted -> resourceReference(R.string.prediction_region_restrictions_subtitle)
    PredictionNotificationUM.QuoteFailed ->
        resourceReference(R.string.prediction_place_notification_quote_error_description)
    PredictionNotificationUM.PriceFailed,
    PredictionNotificationUM.PositionUnavailable,
    -> null
}

private fun PredictionNotificationUM.variant(): TangemMessageBanner.Variant = when (this) {
    PredictionNotificationUM.AwaitingFunds -> TangemMessageBanner.Variant.Info
    PredictionNotificationUM.PartialFill,
    PredictionNotificationUM.NoLiquidity,
    -> TangemMessageBanner.Variant.Warning
    PredictionNotificationUM.MarketClosed,
    PredictionNotificationUM.RegionRestricted,
    PredictionNotificationUM.QuoteFailed,
    PredictionNotificationUM.PriceFailed,
    PredictionNotificationUM.PositionUnavailable,
    -> TangemMessageBanner.Variant.Error
}

private fun PredictionNotificationUM.slotEnd(): (@Composable () -> Unit)? = when (this) {
    PredictionNotificationUM.PartialFill,
    PredictionNotificationUM.NoLiquidity,
    -> {
        {
            Icon(
                imageVector = Icons.ic_info_24,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.primary,
            )
        }
    }
    PredictionNotificationUM.AwaitingFunds -> {
        { TangemLoader() }
    }
    else -> null
}