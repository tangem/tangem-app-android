package com.tangem.features.polymarket.impl.placeprediction.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_20
import com.tangem.core.ui.res.generated.icons.ic_warning_20
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.features.polymarket.impl.common.PolymarketConst
import com.tangem.features.polymarket.impl.common.formatPolymarketEnteredAmount
import com.tangem.features.polymarket.impl.common.formatPolymarketShares
import com.tangem.features.polymarket.impl.placeprediction.entity.MarketHeaderUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceResultUM
import com.tangem.features.polymarket.impl.placeprediction.entity.ViewPositionUM
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionStatusIntents
import com.tangem.features.polymarket.impl.placeprediction.ui.preview.PlacePredictionPreviewIntents
import com.tangem.features.polymarket.impl.placeprediction.ui.components.PredictionMarketBlock
import com.tangem.features.polymarket.impl.placeprediction.ui.preview.PlaceResultPreviewProvider
import com.tangem.utils.StringsSigns.DASH_SIGN
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import androidx.annotation.StringRes
import com.tangem.domain.polymarket.model.PredictionOrderSide

@Composable
internal fun PlacePredictionStatusContent(
    result: PlaceResultUM,
    viewPosition: ViewPositionUM,
    market: MarketHeaderUM,
    side: PredictionOrderSide,
    intents: PlacePredictionStatusIntents,
    modifier: Modifier = Modifier,
) {
    TangemStatusScreen(
        modifier = modifier,
        variant = result.variant(),
        title = result.title(side = side),
        description = stringReference(result.placedAt.formatPlacedAt()),
        asset = { ResultIcon(result = result) },
        details = {
            ResultBanner(result = result)
            PredictionMarketBlock(state = market)
            ResultRows(result = result, market = market, side = side)
        },
        footer = { ResultFooter(viewPosition = viewPosition, intents = intents) },
    )
}

@Composable
private fun ResultFooter(viewPosition: ViewPositionUM, intents: PlacePredictionStatusIntents) {
    val modifier = Modifier
        .fillMaxWidth()
        .padding(all = 16.dp)

    when (viewPosition) {
        ViewPositionUM.Hidden -> TangemButton(
            modifier = modifier,
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.prediction_place_result_to_main),
            onClick = intents::onToMainClick,
        )
        ViewPositionUM.Awaiting,
        ViewPositionUM.Available,
        -> TangemButtonGroup(modifier = modifier, orientation = TangemButtonGroup.Orientation.Horizontal) {
            TangemButton(
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X12,
                text = resourceReference(R.string.prediction_place_result_to_main),
                onClick = intents::onToMainClick,
            )
            TangemButton(
                size = TangemButton.Size.X12,
                text = resourceReference(R.string.prediction_place_result_view_position),
                isLoading = viewPosition == ViewPositionUM.Awaiting,
                isEnabled = viewPosition == ViewPositionUM.Available,
                onClick = intents::onViewPositionClick,
            )
        }
    }
}

@Composable
private fun ResultIcon(result: PlaceResultUM) {
    val modifier = Modifier.size(40.dp)

    when (result) {
        is PlaceResultUM.Filled -> TangemStatusIcon(kind = TangemStatusIcon.Kind.Checkmark, modifier = modifier)
        is PlaceResultUM.PartiallyFilled,
        is PlaceResultUM.Submitted,
        -> TangemStatusIcon(
            kind = TangemStatusIcon.Kind.Checkmark,
            modifier = modifier,
            tint = TangemTheme.colors3.icon.status.warning,
        )
        is PlaceResultUM.NotFilled -> TangemStatusIcon(kind = TangemStatusIcon.Kind.HeartBroken, modifier = modifier)
    }
}

@Composable
private fun ResultBanner(result: PlaceResultUM) {
    when (result) {
        is PlaceResultUM.Filled -> Unit
        is PlaceResultUM.PartiallyFilled,
        is PlaceResultUM.Submitted,
        -> TangemMessageBanner(
            title = resourceReference(R.string.prediction_place_result_partial_banner_title),
            description = resourceReference(R.string.prediction_place_result_partial_banner_description),
            variant = TangemMessageBanner.Variant.Warning,
            slotEnd = { BannerIcon(icon = Icons.ic_info_20) },
        )
        is PlaceResultUM.NotFilled -> TangemMessageBanner(
            title = resourceReference(R.string.prediction_place_result_no_fill_banner_title),
            description = resourceReference(R.string.prediction_place_result_no_fill_banner_description),
            variant = TangemMessageBanner.Variant.Error,
            slotEnd = { BannerIcon(icon = Icons.ic_warning_20) },
        )
    }
}

@Composable
private fun BannerIcon(icon: ImageVector) {
    Icon(
        modifier = Modifier.size(20.dp),
        imageVector = icon,
        contentDescription = null,
        tint = TangemTheme.colors3.icon.primary,
    )
}

@Composable
private fun ResultRows(result: PlaceResultUM, market: MarketHeaderUM, side: PredictionOrderSide) {
    val shares = when (result) {
        is PlaceResultUM.Filled -> result.shares
        is PlaceResultUM.PartiallyFilled -> result.shares
        is PlaceResultUM.NotFilled,
        is PlaceResultUM.Submitted,
        -> return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        DetailRow(
            title = resourceReference(
                when (side) {
                    PredictionOrderSide.BUY -> R.string.prediction_place_result_tokens_bought
                    PredictionOrderSide.SELL -> R.string.prediction_close_result_tokens_sold
                },
            ),
            value = shares?.formatPolymarketShares() ?: DASH_SIGN,
        )
        DetailRow(
            title = resourceReference(R.string.prediction_details_resolution_date),
            value = market.resolutionDateMillis?.formatResolutionDate() ?: DASH_SIGN,
        )
    }
}

@Composable
private fun DetailRow(title: TextReference, value: String) {
    TangemRow(
        divider = true,
        titleSlot = { TangemRowText(text = title, role = TangemRowTextRole.Title) },
        valueSlot = {
            Text(
                text = value,
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        },
    )
}

private fun PlaceResultUM.variant(): TangemStatusScreen.Variant = when (this) {
    is PlaceResultUM.Filled -> TangemStatusScreen.Variant.Success
    is PlaceResultUM.PartiallyFilled,
    is PlaceResultUM.Submitted,
    -> TangemStatusScreen.Variant.Warning
    is PlaceResultUM.NotFilled -> TangemStatusScreen.Variant.Error
}

private fun PlaceResultUM.title(side: PredictionOrderSide): TextReference = when (this) {
    is PlaceResultUM.Filled -> resourceReference(
        when (side) {
            PredictionOrderSide.BUY -> R.string.prediction_place_result_placed_title
            PredictionOrderSide.SELL -> R.string.prediction_close_result_sold_title
        },
        wrappedList(collateralAmount.formatPolymarketEnteredAmount()),
    )
    is PlaceResultUM.PartiallyFilled -> combinedReference(
        resourceReference(side.partialTitle()),
        stringReference(value = "\n"),
        resourceReference(
            R.string.prediction_place_result_partial_amount,
            wrappedList(
                filledCollateral.formatPolymarketEnteredAmount(),
                requestedCollateral.formatPolymarketEnteredAmount(),
            ),
        ),
    )
    is PlaceResultUM.NotFilled -> resourceReference(
        when (side) {
            PredictionOrderSide.BUY -> R.string.prediction_place_result_no_fill_title
            PredictionOrderSide.SELL -> R.string.prediction_close_result_no_fill_title
        },
    )
    is PlaceResultUM.Submitted -> resourceReference(R.string.prediction_place_result_partial_title)
}

@StringRes
private fun PredictionOrderSide.partialTitle(): Int = when (this) {
    PredictionOrderSide.BUY -> R.string.prediction_place_result_partial_title
    PredictionOrderSide.SELL -> R.string.prediction_close_result_partially_sold_title
}

private fun Long.formatPlacedAt(): String = DateTimeFormatters.formatDate(
    date = DateTime(this),
    formatter = DateTimeFormatters.dateTimeMMMdYYYY,
)

private fun Long.formatResolutionDate(): String = DateTimeFormatters.dateDDMMYYYY
    .withZone(DateTimeZone.forID(PolymarketConst.EASTERN_TIME_ZONE))
    .print(this)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlacePredictionStatusContentPreview(
    @PreviewParameter(PlaceResultPreviewProvider::class) result: PlaceResultUM,
) {
    TangemThemePreviewRedesign {
        PlacePredictionStatusContent(
            result = result,
            viewPosition = when (result) {
                is PlaceResultUM.Filled -> ViewPositionUM.Available
                is PlaceResultUM.PartiallyFilled -> ViewPositionUM.Awaiting
                is PlaceResultUM.NotFilled,
                is PlaceResultUM.Submitted,
                -> ViewPositionUM.Hidden
            },
            side = PredictionOrderSide.BUY,
            market = MarketHeaderUM(
                title = "Will Uzbekistan win the 2026 FIFA World Cup?",
                imageUrl = null,
                outcomeTitle = "No",
                outcomePriceCents = 85,
                resolutionDateMillis = 1_774_000_000_000L,
            ),
            intents = PlacePredictionPreviewIntents,
        )
    }
}