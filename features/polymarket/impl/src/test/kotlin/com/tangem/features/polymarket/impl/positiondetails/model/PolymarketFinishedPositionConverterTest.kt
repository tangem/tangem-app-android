package com.tangem.features.polymarket.impl.positiondetails.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.ds2.messagebubble.TangemMessageBubble
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_split_top_16
import com.tangem.core.ui.res.generated.icons.ic_checkmark_16
import com.tangem.core.ui.res.generated.icons.ic_heart_broken_16
import com.tangem.domain.polymarket.model.PolymarketClosedPositionStatus
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketFinishedPositionUM
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketPositionDetailsUM.Summary
import com.tangem.features.polymarket.impl.positions.model.createClosedPolymarketPosition
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketFinishedPositionConverterTest {

    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.US).withZone(ZoneOffset.UTC)
    private val converter = PolymarketFinishedPositionConverter(dateFormatter)

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN closed holding WHEN converted THEN separates realized result from settlement and omits unsupported values`(
        model: FinishedModel,
    ) {
        // Arrange
        val position = createClosedPolymarketPosition(status = model.status, realizedPnl = BigDecimal(model.pnl))
            .copy(endDate = LocalDate.of(2050, 1, 1))
        val expected = PolymarketFinishedPositionUM(
            summary = Summary(
                title = stringReference("Will the event happen?"),
                iconUrl = null,
                pnl = TangemPriceChange.State(stringReference(model.amount), model.direction),
                currentValue = null,
                outcomeLabel = stringReference("Yes"),
                outcomeVariant = TangemMessageBubble.Variant.Info,
                payout = null,
                result = model.badge,
            ),
            shouldShowOutcome = model.badge != null,
            closureDate = stringReference("Sep 8, 2026"),
        )

        // Act
        val actual = converter.convert(position)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN close time across local midnight WHEN converted THEN shows local closure date without a resolution date`() {
        // Arrange
        val position = createClosedPolymarketPosition(outcome = "Draw").copy(
            closedAt = Instant.parse("2026-09-08T23:30:00Z"),
            endDate = LocalDate.of(2025, 1, 1),
        )
        val converter = PolymarketFinishedPositionConverter(dateFormatter.withZone(ZoneOffset.ofHours(3)))

        // Act
        val actual = converter.convert(position)

        // Assert
        assertThat(actual.closureDate).isEqualTo(stringReference("Sep 9, 2026"))
        assertThat(actual.resolutionDate).isNull()
        assertThat(actual.summary.outcomeLabel).isEqualTo(stringReference("Draw"))
        assertThat(actual.summary.outcomeVariant).isEqualTo(TangemMessageBubble.Variant.Neutral)
    }

    @Test
    fun `GIVEN first outcome of the market WHEN converted THEN colours it blue whatever its label`() {
        // Arrange
        val position = createClosedPolymarketPosition(outcome = "Up", outcomeIndex = 0)

        // Act
        val actual = converter.convert(position)

        // Assert
        assertThat(actual.summary.outcomeVariant).isEqualTo(TangemMessageBubble.Variant.Info)
    }

    private fun provideTestModels(): List<FinishedModel> {
        val won = TangemBadgeUM(
            text = resourceReference(R.string.prediction_positions_won),
            variant = TangemBadge.Variant.Solid,
            status = TangemBadge.Status.Success,
            size = TangemBadge.Size.X6,
            iconStart = TangemIconUM.Icon(imageVector = Icons.ic_checkmark_16),
        )
        val lost = TangemBadgeUM(
            text = resourceReference(R.string.prediction_positions_lost),
            status = TangemBadge.Status.Error,
            size = TangemBadge.Size.X6,
            iconStart = TangemIconUM.Icon(imageVector = Icons.ic_heart_broken_16),
        )
        val split = TangemBadgeUM(
            text = resourceReference(R.string.prediction_positions_split),
            size = TangemBadge.Size.X6,
            iconStart = TangemIconUM.Icon(imageVector = Icons.ic_arrow_split_top_16),
        )
        return listOf(
            FinishedModel(PolymarketClosedPositionStatus.WON, won),
            FinishedModel(PolymarketClosedPositionStatus.WON, won, "-12.40", TangemPriceChange.Direction.Down),
            FinishedModel(PolymarketClosedPositionStatus.LOST, lost),
            FinishedModel(PolymarketClosedPositionStatus.LOST, lost, "-12.40", TangemPriceChange.Direction.Down),
            FinishedModel(PolymarketClosedPositionStatus.SPLIT, split),
            FinishedModel(PolymarketClosedPositionStatus.SPLIT, split, "-12.40", TangemPriceChange.Direction.Down),
            FinishedModel(PolymarketClosedPositionStatus.CLOSED, null),
            FinishedModel(PolymarketClosedPositionStatus.CLOSED, null, "-12.40", TangemPriceChange.Direction.Down),
            FinishedModel(PolymarketClosedPositionStatus.CLOSED, null, "0", TangemPriceChange.Direction.Neutral, "$0.00"),
            FinishedModel(
                PolymarketClosedPositionStatus.UNKNOWN,
                TangemBadgeUM(resourceReference(R.string.prediction_positions_unknown), size = TangemBadge.Size.X6),
            ),
        )
    }

    data class FinishedModel(
        val status: PolymarketClosedPositionStatus,
        val badge: TangemBadgeUM?,
        val pnl: String = "12.40",
        val direction: TangemPriceChange.Direction = TangemPriceChange.Direction.Up,
        val amount: String = "$12.40",
    )
}