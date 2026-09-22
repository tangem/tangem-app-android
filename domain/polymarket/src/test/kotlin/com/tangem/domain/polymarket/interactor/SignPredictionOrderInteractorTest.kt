package com.tangem.domain.polymarket.interactor

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PredictionOrderDraft
import com.tangem.domain.polymarket.model.PredictionOrderDraftError
import com.tangem.domain.polymarket.model.PredictionOrderFailure
import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.domain.polymarket.model.PredictionOrderValidationError
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.domain.polymarket.model.PredictionSignedOrder
import com.tangem.domain.polymarket.signing.PolymarketTypedDataSigner
import com.tangem.domain.polymarket.signing.PredictionOrderPayload
import com.tangem.domain.polymarket.usecase.BuildPredictionOrderDraftUseCase
import com.tangem.domain.polymarket.usecase.ValidatePredictionOrderUseCase
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SignPredictionOrderInteractorTest {

    private val buildDraft: BuildPredictionOrderDraftUseCase = mockk()
    private val validateOrder: ValidatePredictionOrderUseCase = mockk()
    private val signer: PolymarketTypedDataSigner = mockk()

    private val interactor = SignPredictionOrderInteractor(
        buildDraft = buildDraft,
        validateOrder = validateOrder,
        signer = signer,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(buildDraft, validateOrder, signer)

        every { buildDraft(any(), any(), any(), any()) } returns DRAFT.right()
        coEvery { signer.signOrder(any(), any()) } returns SIGNED_ORDER.right()
        every { validateOrder(any(), any(), any(), any(), any(), any(), any()) } returns Unit.right()
    }

    @Test
    fun `GIVEN repeated signing WHEN salt generated THEN it always fits a JSON number`() = runTest {
        // Arrange
        val payload = slot<PredictionOrderPayload>()
        coEvery { signer.signOrder(any(), capture(payload)) } returns SIGNED_ORDER.right()
        val salts = mutableSetOf<Long>()

        // Act
        repeat(times = SALT_DRAWS) {
            interactor(request = createRequest())
            salts += payload.captured.salt
        }

        // Assert
        assertThat(salts.all { it in 0..MAX_JS_SAFE_INTEGER }).isTrue()
        assertThat(salts.size).isGreaterThan(1)
    }

    @Test
    fun `GIVEN a quote WHEN signed THEN the order is built from it and signed for the deposit wallet`() = runTest {
        // Arrange
        val payload = slot<PredictionOrderPayload>()
        coEvery { signer.signOrder(any(), capture(payload)) } returns SIGNED_ORDER.right()

        // Act
        val actual = interactor(request = createRequest())

        // Assert
        assertThat(actual).isEqualTo(SIGNED_ORDER.right())
        assertThat(payload.captured.draft).isEqualTo(DRAFT)
        assertThat(payload.captured.depositWalletAddress).isEqualTo(DEPOSIT_WALLET)
        assertThat(payload.captured.timestampMillis).isGreaterThan(MILLIS_THRESHOLD)
    }

    @Test
    fun `GIVEN the checks run WHEN signed THEN they judge the quote's own age and not the signing moment`() =
        runTest {
            // Arrange
            val receivedAt = slot<Long>()
            val now = slot<Long>()
            every {
                validateOrder(any(), any(), any(), any(), any(), capture(receivedAt), capture(now))
            } returns Unit.right()

            // Act
            interactor(request = createRequest())

            // Assert
            assertThat(receivedAt.captured).isEqualTo(RECEIVED_AT)
            assertThat(now.captured).isGreaterThan(RECEIVED_AT)
        }

    @Test
    fun `GIVEN the quote cannot become an order WHEN signed THEN nothing is asked of the card`() = runTest {
        // Arrange
        val error = PredictionOrderDraftError.NotPlaceable(status = PredictionQuoteStatus.MARKET_CLOSED)
        every { buildDraft(any(), any(), any(), any()) } returns error.left()

        // Act
        val actual = interactor(request = createRequest())

        // Assert
        assertThat(actual).isEqualTo(PredictionOrderFailure.Draft(error = error).left())
        coVerify(exactly = 0) { signer.signOrder(any(), any()) }
        verifyNothingValidated()
    }

    @Test
    fun `GIVEN signing fails WHEN signed THEN there is nothing to check`() = runTest {
        // Arrange
        coEvery { signer.signOrder(any(), any()) } returns PolymarketSigningError.UserCancelled.left()

        // Act
        val actual = interactor(request = createRequest())

        // Assert
        assertThat(actual).isEqualTo(
            PredictionOrderFailure.Signing(error = PolymarketSigningError.UserCancelled).left(),
        )
        verifyNothingValidated()
    }

    @Test
    fun `GIVEN the order fails its checks WHEN signed THEN the failure is returned and not the order`() = runTest {
        // Arrange
        val error = PredictionOrderValidationError.StaleQuote(ageMillis = 120_000)
        every { validateOrder(any(), any(), any(), any(), any(), any(), any()) } returns error.left()

        // Act
        val actual = interactor(request = createRequest())

        // Assert
        assertThat(actual).isEqualTo(PredictionOrderFailure.Invalid(error = error).left())
    }

    @Test
    fun `GIVEN a signed order WHEN checked THEN the checks see that very order and its own market`() = runTest {
        // Arrange
        val signedOrder = slot<PredictionSignedOrder>()
        val draft = slot<PredictionOrderDraft>()
        val tokenId = slot<String>()
        val isNegRisk = slot<Boolean>()
        every {
            validateOrder(capture(signedOrder), capture(draft), any(), capture(tokenId), capture(isNegRisk), any(), any())
        } returns Unit.right()

        // Act
        interactor(request = createRequest())

        // Assert
        assertThat(signedOrder.captured).isEqualTo(SIGNED_ORDER)
        assertThat(draft.captured).isEqualTo(DRAFT)
        assertThat(tokenId.captured).isEqualTo(ASSET_ID)
        assertThat(isNegRisk.captured).isFalse()
    }

    private fun createRequest() = PredictionSignRequest(
        addresses = ADDRESSES,
        quote = QUOTE,
        assetId = ASSET_ID,
        side = PredictionOrderSide.BUY,
        isNegRisk = false,
        quoteReceivedAtMillis = RECEIVED_AT,
    )

    private fun verifyNothingValidated() {
        verify(exactly = 0) { validateOrder(any(), any(), any(), any(), any(), any(), any()) }
    }

    private companion object {
        const val SALT_DRAWS = 64
        const val MAX_JS_SAFE_INTEGER = 9_007_199_254_740_991L

        const val ASSET_ID = "1116017998421700000000000000000000000000000000000000000000000000047"
        const val DEPOSIT_WALLET = "0x5A057dc6394196fBa51D84457DA3FB8FA241Fc20"
        const val RECEIVED_AT = 1_787_300_000_000L
        const val MILLIS_THRESHOLD = 1_000_000_000_000L

        val ADDRESSES: PolymarketAddresses = mockk(relaxed = true) {
            every { depositWalletAddress } returns DEPOSIT_WALLET
            every { ownerAddress } returns "0x4130efa9e628a991520be60f2e948a4f9be52fbc"
            every { userWalletId } returns UserWalletId("011")
        }

        val SIGNED_ORDER: PredictionSignedOrder = mockk(relaxed = true)

        val DRAFT = PredictionOrderDraft(
            tokenId = ASSET_ID,
            isNegRisk = false,
            side = PredictionOrderSide.BUY,
            makerAmount = BigInteger("72000000"),
            takerAmount = BigInteger("97959184"),
            takerAmountQuantum = BigInteger.ONE,
            worstCasePrice = BigDecimal("0.735"),
            builderCode = "0x" + "00".repeat(n = 32),
        )

        val QUOTE: PredictionOrderQuote = mockk(relaxed = true)
    }
}