package com.tangem.features.polymarket.impl.positiondetails.ui.preview

import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.ds2.messagebubble.TangemMessageBubble
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_split_top_16
import com.tangem.core.ui.res.generated.icons.ic_checkmark_16
import com.tangem.core.ui.res.generated.icons.ic_heart_broken_16
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketFinishedPositionUM
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketPositionDetailsUM.Summary

internal class PolymarketFinishedPositionPreviewProvider :
    CollectionPreviewParameterProvider<PolymarketFinishedPositionUM>(finishedCases())

private fun finishedCases(): List<PolymarketFinishedPositionUM> {
    val summary = Summary(
        title = stringReference("Will Uzbekistan win the 2026 FIFA World Cup?"),
        iconUrl = null,
        pnl = TangemPriceChange.State(stringReference("$40.50"), TangemPriceChange.Direction.Up),
        currentValue = null,
        outcomeLabel = stringReference("Yes"),
        outcomeVariant = TangemMessageBubble.Variant.Info,
        payout = null,
    )
    val won = TangemBadgeUM(
        text = stringReference("Won"),
        status = TangemBadge.Status.Success,
        variant = TangemBadge.Variant.Solid,
        size = TangemBadge.Size.X6,
        iconStart = TangemIconUM.Icon(imageVector = Icons.ic_checkmark_16),
    )
    val settled = PolymarketFinishedPositionUM(
        summary = summary.copy(result = won),
        shouldShowOutcome = true,
        closureDate = stringReference("Dec 31, 2026"),
    )
    val loss = TangemPriceChange.State(stringReference("$40.50"), TangemPriceChange.Direction.Down)
    return listOf(
        settled,
        settled.copy(
            summary = summary.copy(
                pnl = loss,
                result = won.copy(
                    text = stringReference("Lost"),
                    status = TangemBadge.Status.Error,
                    variant = TangemBadge.Variant.Tinted,
                    iconStart = TangemIconUM.Icon(imageVector = Icons.ic_heart_broken_16),
                ),
            ),
        ),
        settled.copy(
            summary = summary.copy(
                result = won.copy(
                    text = stringReference("Split"),
                    status = TangemBadge.Status.Neutral,
                    variant = TangemBadge.Variant.Tinted,
                    iconStart = TangemIconUM.Icon(imageVector = Icons.ic_arrow_split_top_16),
                ),
            ),
        ),
        settled.copy(summary = summary, shouldShowOutcome = false),
        settled.copy(summary = summary.copy(pnl = loss), shouldShowOutcome = false),
        settled.copy(
            summary = summary.copy(
                pnl = TangemPriceChange.State(stringReference("$0.00"), TangemPriceChange.Direction.Neutral),
                title = stringReference(
                    "Will the national team qualify for the final and win the championship before the end of the year?",
                ),
                outcomeLabel = stringReference("None of the listed candidates"),
                outcomeVariant = TangemMessageBubble.Variant.Neutral,
            ),
            closureDate = null,
        ),
        settled.copy(resolutionDate = stringReference("Dec 30, 2026"), closureDate = null),
    )
}