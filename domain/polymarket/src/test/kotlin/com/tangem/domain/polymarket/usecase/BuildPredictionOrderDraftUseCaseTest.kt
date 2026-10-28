package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketExchangeDomain
import com.tangem.domain.polymarket.model.PolymarketExchangeDomain.Companion.CTF
import com.tangem.domain.polymarket.model.PolymarketExchangeDomain.Companion.EXCHANGE_V3
import com.tangem.domain.polymarket.model.PolymarketExchangeDomain.Companion.NEG_RISK_CTF
import com.tangem.domain.polymarket.model.PolymarketMarketVersion
import com.tangem.domain.polymarket.model.PolymarketMarketVersion.V1
import com.tangem.domain.polymarket.model.PolymarketMarketVersion.V2
import com.tangem.domain.polymarket.model.PredictionOrderDraftError
import com.tangem.domain.polymarket.model.PredictionOrderFees
import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
    fun `GIVEN a buy quote stating shares to four decimals WHEN built THEN the taker quantum is that scale`() {
        // Arrange
        val quote = createQuote(notional = BigDecimal("4.91"), shares = BigDecimal("5.0102"))

        // Act
        val actual = useCase(quote = quote, assetId = ASSET_ID, isNegRisk = false, side = PredictionOrderSide.BUY)

        // Assert
        assertThat(requireNotNull(actual.getOrNull()).takerAmountQuantum).isEqualTo(BigInteger("100"))
    }

    @Test
    fun `GIVEN a buy quote stating shares to the on-chain scale WHEN built THEN the taker quantum is one unit`() {
        // Arrange
        val quote = createQuote(notional = BigDecimal("72.00"), shares = BigDecimal("97.959180"))

        // Act
        val actual = useCase(quote = quote, assetId = ASSET_ID, isNegRisk = false, side = PredictionOrderSide.BUY)

        // Assert
        assertThat(requireNotNull(actual.getOrNull()).takerAmountQuantum).isEqualTo(BigInteger.ONE)
    }

    @Test
    fun `GIVEN a sell quote WHEN built THEN the taker quantum follows the proceeds scale and not the shares`() {
        // Arrange
        val quote = createQuote(
            side = PredictionOrderSide.SELL,
            notional = BigDecimal("70.00000"),
            shares = BigDecimal("100.00"),
        )

        // Act
        val actual = useCase(quote = quote, assetId = ASSET_ID, isNegRisk = false, side = PredictionOrderSide.SELL)

        // Assert
        assertThat(requireNotNull(actual.getOrNull()).takerAmountQuantum).isEqualTo(BigInteger.TEN)
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
    fun `GIVEN a sell below the minimum order size WHEN built THEN it is accepted`() {
        // Arrange
        val quote = createQuote(
            status = PredictionQuoteStatus.BELOW_MIN_ORDER_SIZE,
            side = PredictionOrderSide.SELL,
        )

        // Act
        val actual = useCase(quote = quote, assetId = ASSET_ID, isNegRisk = false, side = PredictionOrderSide.SELL)

        // Assert
        assertThat(actual.getOrNull()).isNotNull()
    }

    @Test
    fun `GIVEN a buy below the minimum order size WHEN built THEN it is refused`() {
        // Arrange
        val quote = createQuote(
            status = PredictionQuoteStatus.BELOW_MIN_ORDER_SIZE,
            side = PredictionOrderSide.BUY,
        )

        // Act
        val actual = useCase(quote = quote, assetId = ASSET_ID, isNegRisk = false, side = PredictionOrderSide.BUY)

        // Assert
        assertThat(actual).isEqualTo(
            PredictionOrderDraftError.NotPlaceable(status = PredictionQuoteStatus.BELOW_MIN_ORDER_SIZE).left(),
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
    fun `GIVEN a quote WHEN built THEN the draft keeps the asset and the builder code`() {
        // Act
        val actual = useCase(
            quote = createQuote(),
            assetId = ASSET_ID,
            isNegRisk = false,
            side = PredictionOrderSide.BUY,
        )

        // Assert
        val draft = requireNotNull(actual.getOrNull())
        assertThat(draft.tokenId).isEqualTo(ASSET_ID)
        assertThat(draft.builderCode).isEqualTo(BUILDER_CODE)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN the quoted exchange domain WHEN built THEN only a combination from the table is signed for`(
        model: DomainModel,
    ) {
        // Arrange
        val quote = createQuote(
            version = model.version,
            exchange = model.exchange,
            signingDomainVersion = model.signingDomainVersion,
        )

        // Act
        val actual = useCase(
            quote = quote,
            assetId = ASSET_ID,
            isNegRisk = model.isNegRisk,
            side = PredictionOrderSide.BUY,
        )

        // Assert
        assertThat(actual.map { it.domain }).isEqualTo(model.expected)
    }

    @Suppress("LongMethod")
    private fun provideTestModels(): List<DomainModel> = listOf(
        DomainModel(
            version = PolymarketMarketVersion.V1,
            isNegRisk = false,
            exchange = CTF_EXCHANGE,
            signingDomainVersion = "2",
            expected = PolymarketExchangeDomain.CTF.right(),
        ),
        DomainModel(
            version = PolymarketMarketVersion.V1,
            isNegRisk = false,
            exchange = CTF_EXCHANGE.lowercase(),
            signingDomainVersion = "2",
            expected = PolymarketExchangeDomain.CTF.right(),
        ),
        DomainModel(
            version = PolymarketMarketVersion.V1,
            isNegRisk = true,
            exchange = NEG_RISK_CTF_EXCHANGE,
            signingDomainVersion = "2",
            expected = PolymarketExchangeDomain.NEG_RISK_CTF.right(),
        ),
        DomainModel(
            version = PolymarketMarketVersion.V2,
            isNegRisk = false,
            exchange = EXCHANGE_V3_ADDRESS,
            signingDomainVersion = "3",
            expected = PolymarketExchangeDomain.EXCHANGE_V3.right(),
        ),
        DomainModel(
            version = PolymarketMarketVersion.V2,
            isNegRisk = true,
            exchange = EXCHANGE_V3_ADDRESS,
            signingDomainVersion = "3",
            expected = PolymarketExchangeDomain.EXCHANGE_V3.right(),
        ),
        DomainModel(
            version = PolymarketMarketVersion.UNKNOWN,
            isNegRisk = false,
            exchange = null,
            signingDomainVersion = null,
            expected = PredictionOrderDraftError.UnknownMarketVersion.left(),
        ),
        DomainModel(
            version = PolymarketMarketVersion.UNKNOWN,
            isNegRisk = false,
            exchange = EXCHANGE_V3_ADDRESS,
            signingDomainVersion = "3",
            expected = PredictionOrderDraftError.UnknownMarketVersion.left(),
        ),
        mismatch(V1, isNegRisk = false, NEG_RISK_CTF_EXCHANGE, "2", CTF),
        mismatch(V1, isNegRisk = true, CTF_EXCHANGE, "2", NEG_RISK_CTF),
        mismatch(V1, isNegRisk = false, CTF_EXCHANGE, "3", CTF),
        mismatch(V1, isNegRisk = false, EXCHANGE_V3_ADDRESS, "3", CTF),
        mismatch(V1, isNegRisk = false, FOREIGN_EXCHANGE, "2", CTF),
        mismatch(V1, isNegRisk = false, null, null, CTF),
        mismatch(V2, isNegRisk = false, CTF_EXCHANGE, "2", EXCHANGE_V3),
        mismatch(V2, isNegRisk = true, NEG_RISK_CTF_EXCHANGE, "2", EXCHANGE_V3),
        mismatch(V2, isNegRisk = false, EXCHANGE_V3_ADDRESS, "2", EXCHANGE_V3),
        mismatch(V2, isNegRisk = false, FOREIGN_EXCHANGE, "3", EXCHANGE_V3),
        mismatch(V2, isNegRisk = false, null, null, EXCHANGE_V3),
    )

    @Suppress("LongParameterList")
    private fun mismatch(
        version: PolymarketMarketVersion,
        isNegRisk: Boolean,
        exchange: String?,
        signingDomainVersion: String?,
        expected: PolymarketExchangeDomain,
    ) = DomainModel(
        version = version,
        isNegRisk = isNegRisk,
        exchange = exchange,
        signingDomainVersion = signingDomainVersion,
        expected = PredictionOrderDraftError.ExchangeMismatch(
            expected = expected,
            quotedExchange = exchange,
            quotedDomainVersion = signingDomainVersion,
        ).left(),
    )

    internal data class DomainModel(
        val version: PolymarketMarketVersion,
        val isNegRisk: Boolean,
        val exchange: String?,
        val signingDomainVersion: String?,
        val expected: Either<PredictionOrderDraftError, PolymarketExchangeDomain>,
    )

    private fun createQuote(
        status: PredictionQuoteStatus = PredictionQuoteStatus.FULL,
        side: PredictionOrderSide = PredictionOrderSide.BUY,
        shares: BigDecimal = BigDecimal("97.95918"),
        notional: BigDecimal = BigDecimal("72.00"),
        worstCasePrice: BigDecimal = BigDecimal("0.735"),
        version: PolymarketMarketVersion = PolymarketMarketVersion.V1,
        exchange: String? = CTF_EXCHANGE,
        signingDomainVersion: String? = "2",
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
        minOrderAmount = BigDecimal.ONE,
        tickSize = BigDecimal("0.001"),
        isLive = false,
        version = version,
        exchange = exchange,
        signingDomainVersion = signingDomainVersion,
    )

    private companion object {
        const val ASSET_ID = "1116017998421700000000000000000000000000000000000000000000000000047"
        const val BUILDER_CODE = "0x00000000000000000000000000000000000000000000000000000000000000aa"
        const val COMPARISON_SCALE = 6
        const val CTF_EXCHANGE = "0xE111180000d2663C0091e4f400237545B87B996B"
        const val NEG_RISK_CTF_EXCHANGE = "0xe2222d279d744050d28e00520010520000310F59"
        const val EXCHANGE_V3_ADDRESS = "0xe3333700cA9d93003F00f0F71f8515005F6c00Aa"
        const val FOREIGN_EXCHANGE = "0x000000000000000000000000000000000000dEaD"
    }
}