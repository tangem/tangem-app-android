package com.tangem.domain.polymarket.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketClosedPositionStatus
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionResult
import com.tangem.domain.polymarket.model.PolymarketPositionStatus
import com.tangem.domain.polymarket.model.PolymarketResolutionStatus
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class PolymarketPositionStatusConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN position outcome and profit WHEN classified THEN uses its status independently of profit`(
        input: TestModel,
    ) {
        // Arrange
        val position: PolymarketPosition = if (input.closedStatus != null) {
            mockk<PolymarketPosition.Closed> {
                every { status } returns input.closedStatus
                every { realizedPnl } returns input.pnl
            }
        } else {
            mockk<PolymarketPosition.Open> {
                every { result } returns input.result
                every { resolutionStatus } returns input.resolution
                every { cashPnl } returns input.pnl
                every { realizedPnl } returns input.pnl
            }
        }

        // Act
        val actual = PolymarketPositionStatusConverter.convert(position)

        // Assert
        assertThat(actual).isEqualTo(input.expected)
    }

    data class TestModel(
        val expected: PolymarketPositionStatus,
        val resolution: PolymarketResolutionStatus = PolymarketResolutionStatus.OPEN,
        val result: PolymarketPositionResult? = null,
        val closedStatus: PolymarketClosedPositionStatus? = null,
        val pnl: BigDecimal = BigDecimal.ONE,
    )

    private companion object {
        @JvmStatic
        fun provideTestModels(): List<TestModel> = listOf(
            TestModel(PolymarketPositionStatus.ACTIVE),
            TestModel(PolymarketPositionStatus.PENDING, resolution = PolymarketResolutionStatus.PENDING_RESOLUTION),
            TestModel(PolymarketPositionStatus.PENDING, resolution = PolymarketResolutionStatus.PROPOSED),
            TestModel(PolymarketPositionStatus.DISPUTED, resolution = PolymarketResolutionStatus.DISPUTED),
            TestModel(PolymarketPositionStatus.UNKNOWN, resolution = PolymarketResolutionStatus.RESOLVED),
            TestModel(PolymarketPositionStatus.UNKNOWN, resolution = PolymarketResolutionStatus.UNKNOWN),
            TestModel(PolymarketPositionStatus.WON, result = PolymarketPositionResult.WON),
            TestModel(PolymarketPositionStatus.LOST, result = PolymarketPositionResult.LOST),
            TestModel(PolymarketPositionStatus.SPLIT, result = PolymarketPositionResult.SPLIT),
            TestModel(PolymarketPositionStatus.UNKNOWN, result = PolymarketPositionResult.UNKNOWN),
            TestModel(PolymarketPositionStatus.CLOSED, closedStatus = PolymarketClosedPositionStatus.CLOSED),
            TestModel(PolymarketPositionStatus.WON, closedStatus = PolymarketClosedPositionStatus.WON),
            TestModel(PolymarketPositionStatus.LOST, closedStatus = PolymarketClosedPositionStatus.LOST),
            TestModel(PolymarketPositionStatus.SPLIT, closedStatus = PolymarketClosedPositionStatus.SPLIT),
            TestModel(PolymarketPositionStatus.UNKNOWN, closedStatus = PolymarketClosedPositionStatus.UNKNOWN),
        ).flatMap { model -> listOf(model.copy(pnl = BigDecimal.ONE), model.copy(pnl = BigDecimal.ONE.negate())) }
    }
}