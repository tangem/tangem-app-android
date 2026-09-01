package com.tangem.domain.polymarket.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PredictionOrder
import com.tangem.domain.polymarket.model.PredictionOrderDraft
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionOrderValidationError
import com.tangem.domain.polymarket.model.PredictionSignedOrder
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ValidatePredictionOrderUseCaseTest {

    private val useCase = ValidatePredictionOrderUseCase()

    @Test
    fun `GIVEN an order built from its own quote WHEN validated THEN it passes`() {
        // Act
        val actual = validate(Input())

        // Assert
        assertThat(actual.isRight()).isTrue()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun validate(model: ValidationModel) {
        // Act
        val actual = validate(model.input)

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN a sell order WHEN the received price is at the floor THEN it passes`() {
        // Arrange
        val input = sellInput()

        // Act
        val actual = validate(input)

        // Assert
        assertThat(actual.isRight()).isTrue()
    }

    @Test
    fun `GIVEN a sell order WHEN the received price is below the floor THEN it is refused`() {
        // Arrange
        val input = sellInput(worstCasePrice = BigDecimal("0.80"))

        // Act
        val actual = validate(input)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(
            PredictionOrderValidationError.PriceWorseThanTolerated::class.java,
        )
    }

    @Test
    fun `GIVEN legs a rounding unit past the tolerated price WHEN validated THEN they still pass`() {
        // Arrange
        val collateral = BigInteger("72000001")
        val input = Input(
            draft = draft(makerAmount = collateral),
            order = order(makerAmount = collateral),
        )

        // Act
        val actual = validate(input)

        // Assert
        assertThat(actual.isRight()).isTrue()
    }

    @Test
    fun `GIVEN legs further past the tolerated price than rounding explains WHEN validated THEN refused`() {
        // Arrange
        val collateral = BigInteger("72000002")
        val input = Input(
            draft = draft(makerAmount = collateral),
            order = order(makerAmount = collateral),
        )

        // Act
        val actual = validate(input)

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(
            PredictionOrderValidationError.PriceWorseThanTolerated(
                implied = BigDecimal("0.73500002"),
                tolerated = WORST_CASE_PRICE,
            ),
        )
    }

    @Test
    fun `GIVEN shares the quote truncated to its own scale WHEN validated THEN the order passes`() {
        // Arrange
        val input = truncatedSharesInput()

        // Act
        val actual = validate(input)

        // Assert
        assertThat(actual.isRight()).isTrue()
    }

    @Test
    fun `GIVEN a buy priced past what the quote scale explains WHEN validated THEN it is refused`() {
        // Arrange
        val input = truncatedSharesInput(collateral = BigInteger("4910095"))

        // Act
        val actual = validate(input)

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(
            PredictionOrderValidationError.PriceWorseThanTolerated(
                implied = BigDecimal("0.98001976"),
                tolerated = QUANTIZED_WORST_CASE_PRICE,
            ),
        )
    }

    @Test
    fun `GIVEN a buy at the far edge of the quote scale WHEN validated THEN it still passes`() {
        // Arrange
        val input = truncatedSharesInput(collateral = BigInteger("4910094"))

        // Act
        val actual = validate(input)

        // Assert
        assertThat(actual.isRight()).isTrue()
    }

    @Test
    fun `GIVEN a coarse quote scale WHEN the price is genuinely worse THEN it is still refused`() {
        // Arrange
        val input = truncatedSharesInput(collateral = BigInteger("6000000"))

        // Act
        val actual = validate(input)

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(
            PredictionOrderValidationError.PriceWorseThanTolerated(
                implied = BigDecimal("1.19755698"),
                tolerated = QUANTIZED_WORST_CASE_PRICE,
            ),
        )
    }

    @Test
    fun `GIVEN sell proceeds the quote truncated WHEN validated THEN the order passes`() {
        // Arrange
        val input = truncatedProceedsInput(collateral = BigInteger("71999990"))

        // Act
        val actual = validate(input)

        // Assert
        assertThat(actual.isRight()).isTrue()
    }

    @Test
    fun `GIVEN sell proceeds shorter than the quote scale explains WHEN validated THEN it is refused`() {
        // Arrange
        val input = truncatedProceedsInput(collateral = BigInteger("71999989"))

        // Act
        val actual = validate(input)

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(
            PredictionOrderValidationError.PriceWorseThanTolerated(
                implied = BigDecimal("0.71999989"),
                tolerated = BigDecimal("0.72"),
            ),
        )
    }

    private fun validate(input: Input) = useCase(
        signedOrder = PredictionSignedOrder(order = input.order, signature = input.signature),
        draft = input.draft,
        depositWalletAddress = input.depositWalletAddress,
        marketTokenId = input.marketTokenId,
        isNegRisk = input.isNegRisk,
        quoteReceivedAtMillis = input.quoteReceivedAtMillis,
        nowMillis = input.nowMillis,
    )

    private fun truncatedSharesInput(collateral: BigInteger = QUANTIZED_COLLATERAL): Input = Input(
        draft = draft(
            makerAmount = collateral,
            takerAmount = QUANTIZED_SHARES,
            takerAmountQuantum = QUANTIZED_SHARES_QUANTUM,
            worstCasePrice = QUANTIZED_WORST_CASE_PRICE,
        ),
        order = order(makerAmount = collateral, takerAmount = QUANTIZED_SHARES),
    )

    private fun truncatedProceedsInput(collateral: BigInteger): Input {
        val shares = BigInteger("100000000")

        return Input(
            draft = draft(
                side = PredictionOrderSide.SELL,
                makerAmount = shares,
                takerAmount = collateral,
                takerAmountQuantum = BigInteger.TEN,
                worstCasePrice = BigDecimal("0.72"),
            ),
            order = order(side = PredictionOrderSide.SELL, makerAmount = shares, takerAmount = collateral),
        )
    }

    private fun sellInput(worstCasePrice: BigDecimal = BigDecimal("0.72")): Input {
        val shares = BigInteger("100000000")
        val collateral = BigInteger("72000000")

        return Input(
            draft = draft(
                side = PredictionOrderSide.SELL,
                makerAmount = shares,
                takerAmount = collateral,
                worstCasePrice = worstCasePrice,
            ),
            order = order(side = PredictionOrderSide.SELL, makerAmount = shares, takerAmount = collateral),
        )
    }

    internal data class ValidationModel(val input: Input, val expected: PredictionOrderValidationError)

    internal data class Input(
        val draft: PredictionOrderDraft = draft(),
        val order: PredictionOrder = order(),
        val signature: String = WRAPPED_SIGNATURE,
        val depositWalletAddress: String = DEPOSIT_WALLET,
        val marketTokenId: String = TOKEN_ID,
        val isNegRisk: Boolean = false,
        val quoteReceivedAtMillis: Long = NOW_MILLIS - 5_000,
        val nowMillis: Long = NOW_MILLIS,
    )

    @Suppress("LongMethod")
    private fun provideTestModels() = listOf(
        ValidationModel(
            input = Input(marketTokenId = "42"),
            expected = PredictionOrderValidationError.MarketMismatch(expectedTokenId = "42", isNegRiskExpected = false),
        ),
        ValidationModel(
            input = Input(isNegRisk = true),
            expected = PredictionOrderValidationError.MarketMismatch(
                expectedTokenId = TOKEN_ID,
                isNegRiskExpected = true,
            ),
        ),
        ValidationModel(
            input = Input(
                draft = draft(makerAmount = BigInteger.ZERO),
                order = order(makerAmount = BigInteger.ZERO),
            ),
            expected = PredictionOrderValidationError.NonPositiveLeg(
                makerAmount = BigInteger.ZERO,
                takerAmount = TAKER_AMOUNT,
            ),
        ),
        ValidationModel(
            input = Input(order = order(makerAmount = MAKER_AMOUNT * BigInteger.TWO)),
            expected = PredictionOrderValidationError.LegsNotFromQuote(
                signedMakerAmount = MAKER_AMOUNT * BigInteger.TWO,
                signedTakerAmount = TAKER_AMOUNT,
            ),
        ),
        ValidationModel(
            input = Input(order = order(tokenId = BigInteger("42"))),
            expected = PredictionOrderValidationError.MarketMismatch(
                expectedTokenId = TOKEN_ID,
                isNegRiskExpected = false,
            ),
        ),
        ValidationModel(
            input = Input(draft = draft(worstCasePrice = BigDecimal.ZERO)),
            expected = PredictionOrderValidationError.PriceOutOfRange(worstCasePrice = BigDecimal.ZERO),
        ),
        ValidationModel(
            input = Input(draft = draft(worstCasePrice = BigDecimal.ONE)),
            expected = PredictionOrderValidationError.PriceOutOfRange(worstCasePrice = BigDecimal.ONE),
        ),
        ValidationModel(
            input = Input(draft = draft(worstCasePrice = BigDecimal("0.70"))),
            expected = PredictionOrderValidationError.PriceWorseThanTolerated(
                implied = BigDecimal("0.73500000"),
                tolerated = BigDecimal("0.70"),
            ),
        ),
        ValidationModel(
            input = Input(order = order(signatureType = 1)),
            expected = PredictionOrderValidationError.WrongSignatureType(signatureType = 1),
        ),
        ValidationModel(
            input = Input(order = order(maker = SOMEONE_ELSE)),
            expected = PredictionOrderValidationError.MakerIsNotDepositWallet(
                maker = SOMEONE_ELSE,
                expected = DEPOSIT_WALLET,
            ),
        ),
        ValidationModel(
            input = Input(order = order(signer = SOMEONE_ELSE)),
            expected = PredictionOrderValidationError.SignerIsNotMaker(
                signer = SOMEONE_ELSE,
                maker = DEPOSIT_WALLET,
            ),
        ),
        ValidationModel(
            input = Input(order = order(side = PredictionOrderSide.SELL)),
            expected = PredictionOrderValidationError.SideMismatch(
                signed = PredictionOrderSide.SELL,
                quoted = PredictionOrderSide.BUY,
            ),
        ),
        ValidationModel(
            input = Input(draft = draft(builderCode = ""), order = order(builder = "")),
            expected = PredictionOrderValidationError.MissingBuilderCode(builder = ""),
        ),
        ValidationModel(
            input = Input(order = order(builder = "0xsomeoneelse")),
            expected = PredictionOrderValidationError.BuilderCodeMismatch(
                signed = "0xsomeoneelse",
                quoted = BUILDER_CODE,
            ),
        ),
        ValidationModel(
            input = Input(order = order(timestampMillis = NOW_MILLIS / 1_000)),
            expected = PredictionOrderValidationError.TimestampNotInMillis(timestampMillis = NOW_MILLIS / 1_000),
        ),
        ValidationModel(
            input = Input(order = order(timestampMillis = NOW_MILLIS + 1)),
            expected = PredictionOrderValidationError.TimestampInFuture(
                timestampMillis = NOW_MILLIS + 1,
                nowMillis = NOW_MILLIS,
            ),
        ),
        ValidationModel(
            input = Input(signature = BARE_SIGNATURE),
            expected = PredictionOrderValidationError.SignatureNotWrapped(length = BARE_SIGNATURE.length - 2),
        ),
        ValidationModel(
            input = Input(quoteReceivedAtMillis = NOW_MILLIS - 60_001),
            expected = PredictionOrderValidationError.StaleQuote(ageMillis = 60_001),
        ),
        ValidationModel(
            input = Input(quoteReceivedAtMillis = NOW_MILLIS + 1),
            expected = PredictionOrderValidationError.StaleQuote(ageMillis = -1),
        ),
    )

    private companion object {
        const val TOKEN_ID = "1116017998421700000000000000000000000000000000000000000000000000047"
        const val DEPOSIT_WALLET = "0x5A057dc6394196fBa51D84457DA3FB8FA241Fc20"
        const val SOMEONE_ELSE = "0x4130efa9e628a991520be60f2e948a4f9be52fbc"
        const val BUILDER_CODE = "0x00000000000000000000000000000000000000000000000000000000000000aa"
        const val NOW_MILLIS = 1_787_300_000_000L

        val MAKER_AMOUNT: BigInteger = BigInteger("72000000")
        val TAKER_AMOUNT: BigInteger = BigInteger("97959184")
        val WORST_CASE_PRICE: BigDecimal = BigDecimal("0.735")

        val QUANTIZED_COLLATERAL: BigInteger = BigInteger("4910000")
        val QUANTIZED_SHARES: BigInteger = BigInteger("5010200")
        val QUANTIZED_WORST_CASE_PRICE: BigDecimal = BigDecimal("0.98")
        val QUANTIZED_SHARES_QUANTUM: BigInteger = BigInteger("100")

        val BARE_SIGNATURE = "0x" + "11".repeat(n = 65)
        val WRAPPED_SIGNATURE = "11".repeat(n = 317)

        fun draft(
            side: PredictionOrderSide = PredictionOrderSide.BUY,
            makerAmount: BigInteger = MAKER_AMOUNT,
            takerAmount: BigInteger = TAKER_AMOUNT,
            takerAmountQuantum: BigInteger = BigInteger.ONE,
            worstCasePrice: BigDecimal = WORST_CASE_PRICE,
            builderCode: String = BUILDER_CODE,
        ) = PredictionOrderDraft(
            tokenId = TOKEN_ID,
            isNegRisk = false,
            side = side,
            makerAmount = makerAmount,
            takerAmount = takerAmount,
            takerAmountQuantum = takerAmountQuantum,
            worstCasePrice = worstCasePrice,
            builderCode = builderCode,
        )

        @Suppress("LongParameterList")
        fun order(
            maker: String = DEPOSIT_WALLET,
            signer: String = maker,
            tokenId: BigInteger = BigInteger(TOKEN_ID),
            makerAmount: BigInteger = MAKER_AMOUNT,
            takerAmount: BigInteger = TAKER_AMOUNT,
            side: PredictionOrderSide = PredictionOrderSide.BUY,
            signatureType: Int = 3,
            timestampMillis: Long = NOW_MILLIS - 1_000,
            builder: String = BUILDER_CODE,
        ) = PredictionOrder(
            salt = 1_234_567_890L,
            maker = maker,
            signer = signer,
            tokenId = tokenId,
            makerAmount = makerAmount,
            takerAmount = takerAmount,
            side = side,
            signatureType = signatureType,
            timestampMillis = timestampMillis,
            metadata = "0x" + "00".repeat(n = 32),
            builder = builder,
        )
    }
}