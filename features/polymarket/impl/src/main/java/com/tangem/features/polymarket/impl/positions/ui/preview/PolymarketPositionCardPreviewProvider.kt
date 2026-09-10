package com.tangem.features.polymarket.impl.positions.ui.preview

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.ds2.glowring.TangemGlowRing
import com.tangem.core.ui.ds2.messagebubble.TangemMessageBubble
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_split_top_16
import com.tangem.core.ui.res.generated.icons.ic_checkmark_16
import com.tangem.core.ui.res.generated.icons.ic_heart_broken_16
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionUM

internal data class PolymarketPositionCardPreviewCase(val name: String, val state: PolymarketPositionUM)

/** UI samples also cover states whose production mapping belongs to the history/claim integration. */
internal class PolymarketPositionCardPreviewProvider :
    CollectionPreviewParameterProvider<PolymarketPositionCardPreviewCase>(
        activeCases() + resolvedCases() + finishedCases() + resolutionCases(),
    ) {

    override fun getDisplayName(index: Int): String = values.elementAt(index).name
}

internal class PolymarketPositionCardLayoutPreviewProvider :
    CollectionPreviewParameterProvider<PolymarketPositionCardPreviewCase>(layoutCases()) {

    override fun getDisplayName(index: Int): String = values.elementAt(index).name
}

private fun activeCases(): List<PolymarketPositionCardPreviewCase> {
    val active = activePosition()
    return listOf(
        PolymarketPositionCardPreviewCase("Active - Yes - loss", active),
        PolymarketPositionCardPreviewCase(
            "Active - No - gain",
            active.copy(
                outcomeLabel = stringReference("No"),
                outcomeVariant = TangemMessageBubble.Variant.Error,
                priceChange = priceChange("$12.40 (1.10%)", TangemPriceChange.Direction.Up),
                currentValue = stringReference("$1,245.00"),
            ),
        ),
        PolymarketPositionCardPreviewCase(
            "Active - unchanged",
            active.copy(priceChange = priceChange("$0.00 (0%)", TangemPriceChange.Direction.Neutral)),
        ),
        PolymarketPositionCardPreviewCase(
            "Active - custom outcome",
            active.copy(outcomeLabel = stringReference("Draw"), outcomeVariant = TangemMessageBubble.Variant.Neutral),
        ),
    )
}

private fun resolvedCases(): List<PolymarketPositionCardPreviewCase> {
    val resolved = activePosition().copy(currentValue = null, onClick = null)
    val won = resolved.copy(
        priceChange = priceChange("$245.00 (24.50%)", TangemPriceChange.Direction.Up),
        result = badge(
            text = "Won $1,245.00",
            status = TangemBadge.Status.Success,
            variant = TangemBadge.Variant.Solid,
            icon = Icons.ic_checkmark_16,
        ),
    )
    return listOf(
        PolymarketPositionCardPreviewCase(
            "Resolved - Won - current mapping",
            won.copy(result = badge("Won $1,245.00", TangemBadge.Status.Success)),
        ),
        PolymarketPositionCardPreviewCase(
            "UI: Won - Yes - awaiting claim",
            won.copy(glowVariant = TangemGlowRing.Variant.Info),
        ),
        PolymarketPositionCardPreviewCase(
            "UI: Won - No - awaiting claim",
            won.copy(
                outcomeLabel = stringReference("No"),
                outcomeVariant = TangemMessageBubble.Variant.Error,
                glowVariant = TangemGlowRing.Variant.Error,
            ),
        ),
        PolymarketPositionCardPreviewCase(
            "Resolved - Lost",
            resolved.copy(result = badge("Lost", TangemBadge.Status.Error)),
        ),
        PolymarketPositionCardPreviewCase(
            "Resolved - Split",
            resolved.copy(result = badge("Split", TangemBadge.Status.Neutral)),
        ),
    )
}

private fun finishedCases(): List<PolymarketPositionCardPreviewCase> {
    // The closed API supplies realised P&L without a percentage or a separate payout amount.
    val finished = activePosition().copy(
        priceChange = priceChange("$12.40", TangemPriceChange.Direction.Up),
        currentValue = null,
        onClick = null,
    )
    return listOf(
        PolymarketPositionCardPreviewCase(
            "UI: Finished - Won",
            finished.copy(result = badge("Won", TangemBadge.Status.Success, icon = Icons.ic_checkmark_16)),
        ),
        PolymarketPositionCardPreviewCase(
            "UI: Finished - Lost with positive realised P&L",
            finished.copy(result = badge("Lost", TangemBadge.Status.Error, icon = Icons.ic_heart_broken_16)),
        ),
        PolymarketPositionCardPreviewCase(
            "UI: Finished - Split",
            finished.copy(result = badge("Split", TangemBadge.Status.Neutral, icon = Icons.ic_arrow_split_top_16)),
        ),
        PolymarketPositionCardPreviewCase(
            "UI: Finished - Closed - gain",
            finished.copy(result = badge("Closed", TangemBadge.Status.Neutral)),
        ),
        PolymarketPositionCardPreviewCase(
            "UI: Finished - Closed - loss",
            finished.copy(
                priceChange = priceChange("$12.40", TangemPriceChange.Direction.Down),
                result = badge("Closed", TangemBadge.Status.Neutral),
            ),
        ),
    )
}

private fun resolutionCases(): List<PolymarketPositionCardPreviewCase> {
    val active = activePosition()
    // Pending/Proposed and Disputed currently use the active card; their banners belong to details.
    return listOf(
        PolymarketPositionCardPreviewCase("Pending / Proposed - active appearance", active),
        PolymarketPositionCardPreviewCase("Disputed - active appearance", active),
        PolymarketPositionCardPreviewCase(
            "Unknown result",
            active.copy(result = badge("Unknown", TangemBadge.Status.Neutral), onClick = null),
        ),
    )
}

private fun layoutCases(): List<PolymarketPositionCardPreviewCase> {
    val active = activePosition()
    return listOf(
        PolymarketPositionCardPreviewCase(
            "Long question",
            active.copy(
                title = stringReference(
                    "Will the national team qualify for the final and win the championship before the end of the year?",
                ),
            ),
        ),
        PolymarketPositionCardPreviewCase(
            "Long custom outcome",
            active.copy(
                outcomeLabel = stringReference("None of the listed candidates"),
                outcomeVariant = TangemMessageBubble.Variant.Neutral,
            ),
        ),
        PolymarketPositionCardPreviewCase(
            "Large amounts",
            active.copy(
                priceChange = priceChange("$1,234,567.89 (123.45%)", TangemPriceChange.Direction.Up),
                currentValue = stringReference("$12,345,678.90"),
            ),
        ),
        PolymarketPositionCardPreviewCase(
            "Large status badge replaces value",
            active.copy(
                result = badge("Won $12,345,678.90", TangemBadge.Status.Success, icon = Icons.ic_checkmark_16),
                glowVariant = TangemGlowRing.Variant.Info,
                onClick = null,
            ),
        ),
        PolymarketPositionCardPreviewCase(
            "Missing image, P&L and value",
            active.copy(title = stringReference("Event"), priceChange = null, currentValue = null, onClick = null),
        ),
    )
}

private fun activePosition() = PolymarketPositionUM(
    id = "preview",
    title = stringReference("Will France win the 2026 FIFA World Cup?"),
    iconUrl = null,
    outcomeLabel = stringReference("Yes"),
    outcomeVariant = TangemMessageBubble.Variant.Info,
    priceChange = priceChange("$0.03 (0.28%)", TangemPriceChange.Direction.Down),
    currentValue = stringReference("$9.97"),
    result = null,
    onClick = {},
)

private fun priceChange(text: String, direction: TangemPriceChange.Direction) = TangemPriceChange.State(
    value = stringReference(text),
    direction = direction,
)

private fun badge(
    text: String,
    status: TangemBadge.Status,
    variant: TangemBadge.Variant = TangemBadge.Variant.Tinted,
    icon: ImageVector? = null,
) = TangemBadgeUM(
    text = stringReference(text),
    variant = variant,
    status = status,
    size = TangemBadge.Size.X6,
    iconStart = icon?.let { TangemIconUM.Icon(imageVector = it) },
)