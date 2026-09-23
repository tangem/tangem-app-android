package com.tangem.features.polymarket.impl.positiondetails.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.polymarket.model.PolymarketPositionResult
import com.tangem.domain.polymarket.model.PolymarketResolutionStatus
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketPositionDetailsUM
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketPositionDetailsUM.Notice
import com.tangem.features.polymarket.impl.positions.model.createPolymarketPosition
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketPositionDetailsConverterTest {
    private val converter = PolymarketPositionDetailsConverter()

    @Test
    fun `GIVEN remaining loss and realized profit WHEN converted THEN keeps both results and share precision`() {
        // Arrange
        val position = createPolymarketPosition()

        // Act
        val actual = converter.convert(PolymarketPositionDetailsConverter.Input(position = position))

        // Assert
        assertThat(actual.summary.pnl).isEqualTo(
            TangemPriceChange.State(
                resourceReference(R.string.common_crypto_fiat_format, wrappedList("0.28%", "$0.03")),
                TangemPriceChange.Direction.Down,
            ),
        )
        assertThat(actual.metrics).isEqualTo(
            PolymarketPositionDetailsUM.Metrics(
                quantity = stringReference("28.09"),
                priceChanges = stringReference("35.59¢ → 35.5¢"),
                unrealizedPnl = stringReference("-$0.03"),
                realizedPnl = stringReference("+$12.00"),
            ),
        )
        assertThat(actual.information).isEqualTo(
            PolymarketPositionDetailsUM.Information(
                invested = stringReference("$10.00"),
                status = resourceReference(R.string.prediction_positions_open),
                endDate = stringReference("Apr 30, 2027"),
            ),
        )
        assertThat(actual.summary.payout).isEqualTo(stringReference("$28.09"))
        assertThat(actual.summary.currentValue).isEqualTo(stringReference("$9.97"))
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN resolution state and past end date WHEN converted THEN uses backend state`(case: StatusCase) {
        // Arrange
        val position = createPolymarketPosition(resolutionStatus = case.status).copy(endDate = LocalDate.of(2000, 1, 1))

        // Act
        val actual = converter.convert(PolymarketPositionDetailsConverter.Input(position = position))

        // Assert
        assertThat(actual.notice).isEqualTo(case.notice)
        assertThat(actual.information.status).isEqualTo(resourceReference(case.label))
        assertThat(position.canOpenPositionDetails).isEqualTo(case.canOpen)
    }

    @ParameterizedTest
    @EnumSource(PolymarketPositionResult::class)
    fun `GIVEN settlement result WHEN checking details THEN no action or payout estimate`(result: PolymarketPositionResult) {
        // Arrange
        val position = createPolymarketPosition(result = result)

        // Act
        val actual = converter.convert(PolymarketPositionDetailsConverter.Input(position = position))

        // Assert
        assertThat(position.canOpenPositionDetails).isFalse()
        assertThat(actual.summary.payout).isNull()
    }

    @Test
    fun `GIVEN redeemable position WHEN checking details THEN claim remains outside details`() {
        // Arrange
        val position = createPolymarketPosition().copy(isRedeemable = true)

        // Act
        val actual = position.canOpenPositionDetails

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN missing metadata and tiny pnl WHEN converted THEN omits metadata without negative zero`() {
        // Arrange
        val position = createPolymarketPosition(cashPnl = BigDecimal("-0.001"), realizedPnl = BigDecimal("0.001"))
            .copy(eventId = null, eventTitle = null, endDate = null)

        // Act
        val actual = converter.convert(PolymarketPositionDetailsConverter.Input(position = position))

        // Assert
        assertThat(actual.event).isNull()
        assertThat(actual.information.endDate).isNull()
        assertThat(actual.metrics.unrealizedPnl).isEqualTo(stringReference("$0.00"))
        assertThat(actual.metrics.realizedPnl).isEqualTo(stringReference("<$0.01"))
    }

    private fun provideTestModels() = listOf(
        StatusCase(PolymarketResolutionStatus.OPEN, null, R.string.prediction_positions_open),
        StatusCase(
            PolymarketResolutionStatus.PENDING_RESOLUTION,
            Notice.Pending,
            R.string.prediction_positions_pending,
        ),
        StatusCase(PolymarketResolutionStatus.PROPOSED, Notice.Pending, R.string.prediction_positions_pending),
        StatusCase(PolymarketResolutionStatus.DISPUTED, Notice.Disputed, R.string.prediction_positions_disputed),
        StatusCase(PolymarketResolutionStatus.UNKNOWN, null, R.string.prediction_positions_unknown),
        StatusCase(PolymarketResolutionStatus.RESOLVED, null, R.string.prediction_positions_unknown, canOpen = false),
    )

    data class StatusCase(
        val status: PolymarketResolutionStatus,
        val notice: Notice?,
        val label: Int,
        val canOpen: Boolean = true,
    )
}