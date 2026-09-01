package com.tangem.domain.polymarket.usecase

import arrow.core.left
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PredictionOrderDraftError
import com.tangem.domain.polymarket.model.PredictionOrderFees
import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

internal class BuildPredictionOrderDraftUseCaseTest {

    private val useCase = BuildPredictionOrderDraftUseCase()

    @Test
    fun `GIVEN a buy quote WHEN built THEN the collateral leg is maker and the share leg is taker`() {
        // Arrange
        val quote = createQuote(notional = BigDecimal("72.00"), shares = BigDecimal("97.95918"))

        // Act
        val actual = useCase(quote = quote, assetId = ASSET_ID, isNegRisk = false, side = PredictionOrderSide.BUY)

        // Assert
        val draft = requireNotNull(actual.getOrNull())
        assertThat(draft.makerAmount).isEqualTo(BigInteger("72000000"))
        assertThat(draft.takerAmount).isEqualTo(BigInteger("97959180"))
    }

    @Test
    fun `GIVEN a sell quote WHEN built THEN the share leg is maker and the collateral leg is taker`() {
        // Arrange
        val quote = createQuote(
            side = PredictionOrderSide.SELL,
            notional = BigDecimal("70.00000"),
            shares = BigDecimal("100.00"),
        )

        // Act
        val actual = useCase(quote = quote, assetId = ASSET_ID, isNegRisk = false, side = PredictionOrderSide.SELL)

        // Assert
        val draft = requireNotNull(actual.getOrNull())
        assertThat(draft.makerAmount).isEqualTo(BigInteger("100000000"))
        assertThat(draft.takerAmount).isEqualTo(BigInteger("70000000"))
    }

    @Test
    fun `GIVEN an amount the backend truncated WHEN built THEN the quoted leg is used and not the typed one`() {
        // Arrange — the user typed 72.1234567; the backend normalised it onto the 2-decimal maker grid
        val quote = createQuote(notional = BigDecimal("72.12"), shares = BigDecimal("98.12244"))

        // Act
        val actual = useCase(quote = quote, assetId = ASSET_ID, isNegRisk = false, side = PredictionOrderSide.BUY)

        // Assert
        assertThat(requireNotNull(actual.getOrNull()).makerAmount).isEqualTo(BigInteger("72120000"))
    }

    @Test
    fun `GIVEN a buy draft WHEN read THEN the implied price is not worse than the cap`() {
        // Arrange
        val quote = createQuote(
            notional = BigDecimal("72.00"),
            shares = BigDecimal("97.95918"),
            worstCasePrice = BigDecimal("0.735"),
        )

        // Act
        val draft = requireNotNull(
            useCase(quote = quote, assetId = ASSET_ID, isNegRisk = false, side = PredictionOrderSide.BUY).getOrNull(),
        )

        // Assert — swapped legs would break this by orders of magnitude
        val implied = BigDecimal(draft.makerAmount)
            .divide(BigDecimal(draft.takerAmount), COMPARISON_SCALE, RoundingMode.HALF_UP)
        assertThat(implied.compareTo(BigDecimal("0.735")) <= 0).isTrue()
    }

    @Test
    fun `GIVEN a leg finer than the collateral scale WHEN built THEN it is refused rather than rounded`() {
        // Arrange — seven decimals cannot be represented in the 6-decimal on-chain unit
        val quote = createQuote(notional = BigDecimal("72.0000001"), shares = BigDecimal("97.95918"))

        // Act
        val actual = useCase(quote = quote, assetId = ASSET_ID, isNegRisk = false, side = PredictionOrderSide.BUY)

        // Assert
        assertThat(actual).isEqualTo(
            PredictionOrderDraftError.AmountNotRepresentable(value = BigDecimal("72.0000001")).left(),
        )
    }

    @Test
    fun `GIVEN a non-placeable quote WHEN built THEN it is refused`() {
        // Arrange
        val quote = createQuote(status = PredictionQuoteStatus.INSUFFICIENT_LIQUIDITY)

        // Act
        val actual = useCase(quote = quote, assetId = ASSET_ID, isNegRisk = false, side = PredictionOrderSide.BUY)

        // Assert
        assertThat(actual).isEqualTo(
            PredictionOrderDraftError.NotPlaceable(status = PredictionQuoteStatus.INSUFFICIENT_LIQUIDITY).left(),
        )
    }

    @Test
    fun `GIVEN a quote priced for the other side WHEN built THEN it is refused`() {
        // Arrange
        val quote = createQuote(side = PredictionOrderSide.SELL)

        // Act
        val actual = useCase(quote = quote, assetId = ASSET_ID, isNegRisk = false, side = PredictionOrderSide.BUY)

        // Assert
        assertThat(actual).isEqualTo(
            PredictionOrderDraftError.SideMismatch(
                quoted = PredictionOrderSide.SELL,
                requested = PredictionOrderSide.BUY,
            ).left(),
        )
    }

    @Test
    fun `GIVEN a neg-risk market WHEN built THEN the draft carries the flag for the domain choice`() {
        // Act
        val actual = useCase(
            quote = createQuote(),
            assetId = ASSET_ID,
            isNegRisk = true,
            side = PredictionOrderSide.BUY,
        )

        // Assert
        val draft = requireNotNull(actual.getOrNull())
        assertThat(draft.isNegRisk).isTrue()
        assertThat(draft.tokenId).isEqualTo(ASSET_ID)
        assertThat(draft.builderCode).isEqualTo(BUILDER_CODE)
    }

    private fun createQuote(
        status: PredictionQuoteStatus = PredictionQuoteStatus.FULL,
        side: PredictionOrderSide = PredictionOrderSide.BUY,
        shares: BigDecimal = BigDecimal("97.95918"),
        notional: BigDecimal = BigDecimal("72.00"),
        worstCasePrice: BigDecimal = BigDecimal("0.735"),
    ): PredictionOrderQuote = PredictionOrderQuote(
        status = status,
        side = side,
        shares = shares,
        notional = notional,
        expectedExecutionAmount = BigDecimal("100.00000"),
        averagePrice = BigDecimal("0.720"),
        worstCasePrice = worstCasePrice,
        fees = PredictionOrderFees(
            market = BigDecimal("1.00800"),
            builder = BigDecimal("1.80000"),
            total = BigDecimal("2.80800"),
        ),
        total = BigDecimal("74.80800"),
        builderCode = BUILDER_CODE,
        minOrderSize = BigDecimal("7"),
        tickSize = BigDecimal("0.001"),
        isLive = false,
    )

    private companion object {
        const val ASSET_ID = "1116017998421700000000000000000000000000000000000000000000000000047"
        const val BUILDER_CODE = "0x00000000000000000000000000000000000000000000000000000000000000aa"
        const val COMPARISON_SCALE = 6
    }
}