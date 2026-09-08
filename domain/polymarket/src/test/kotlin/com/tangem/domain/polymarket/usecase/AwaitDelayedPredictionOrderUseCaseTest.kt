package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PredictionOrderState
import com.tangem.domain.polymarket.model.PredictionOrderStateError
import com.tangem.domain.polymarket.model.PredictionOrderStatus
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AwaitDelayedPredictionOrderUseCaseTest {

    private val repository: PolymarketRepository = mockk()
    private val getApiCredentials: GetPolymarketApiCredentialsUseCase = mockk()

    private val useCase = AwaitDelayedPredictionOrderUseCase(
        polymarketRepository = repository,
        getApiCredentials = getApiCredentials,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository, getApiCredentials)
        coEvery { getApiCredentials(userWalletId = USER_WALLET_ID) } returns CREDENTIALS
    }

    @Test
    fun `GIVEN the order is matched on the first read WHEN invoked THEN the match is returned after one read`() =
        runTest {
            // Arrange
            val matched = order(status = PredictionOrderStatus.Matched)
            coEvery { repository.getOrder(OWNER, CREDENTIALS, ORDER_ID) } returns matched.right()

            // Act
            val actual = useCase(addresses = ADDRESSES, orderId = ORDER_ID)

            // Assert
            assertThat(actual).isEqualTo(DelayedPredictionOrderOutcome.Matched(order = matched))
            assertThat(currentTime).isEqualTo(READ_INTERVAL_MILLIS)
            coVerify(exactly = 1) { repository.getOrder(OWNER, CREDENTIALS, ORDER_ID) }
        }

    @ParameterizedTest
    @MethodSource("provideUnknownStatusErrors")
    fun `GIVEN reads leave the status unknown before the match WHEN invoked THEN those reads are tried again`(
        error: PredictionOrderStateError,
    ) = runTest {
        // Arrange
        val matched = order(status = PredictionOrderStatus.Matched)
        coEvery { repository.getOrder(OWNER, CREDENTIALS, ORDER_ID) } returnsMany listOf(
            error.left(),
            error.left(),
            matched.right(),
        )

        // Act
        val actual = useCase(addresses = ADDRESSES, orderId = ORDER_ID)

        // Assert
        assertThat(actual).isEqualTo(DelayedPredictionOrderOutcome.Matched(order = matched))
        assertThat(currentTime).isEqualTo(READ_INTERVAL_MILLIS * MAX_READS)
        coVerify(exactly = 3) { repository.getOrder(OWNER, CREDENTIALS, ORDER_ID) }
    }

    @ParameterizedTest
    @MethodSource("provideUnknownStatusErrors")
    fun `GIVEN every read leaves the status unknown WHEN invoked THEN the order stays unconfirmed after the last read`(
        error: PredictionOrderStateError,
    ) = runTest {
        // Arrange
        coEvery { repository.getOrder(OWNER, CREDENTIALS, ORDER_ID) } returns error.left()

        // Act
        val actual = useCase(addresses = ADDRESSES, orderId = ORDER_ID)

        // Assert
        assertThat(actual).isEqualTo(DelayedPredictionOrderOutcome.Unconfirmed)
        assertThat(currentTime).isEqualTo(READ_INTERVAL_MILLIS * MAX_READS)
        coVerify(exactly = MAX_READS) { repository.getOrder(OWNER, CREDENTIALS, ORDER_ID) }
    }

    private fun provideUnknownStatusErrors() = listOf(
        PredictionOrderStateError.Timeout,
        PredictionOrderStateError.Network,
        PredictionOrderStateError.Unknown(httpCode = 500, detail = null),
        PredictionOrderStateError.Unknown(httpCode = 503, detail = "upstream unavailable"),
    )

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN the book answers with something other than a match WHEN invoked THEN the wait fails at once`(
        model: FinalAnswerModel,
    ) = runTest {
        // Arrange
        coEvery { repository.getOrder(OWNER, CREDENTIALS, ORDER_ID) } returns model.answer

        // Act
        val actual = useCase(addresses = ADDRESSES, orderId = ORDER_ID)

        // Assert
        assertThat(actual).isInstanceOf(DelayedPredictionOrderOutcome.Failed::class.java)
        assertThat(currentTime).isEqualTo(READ_INTERVAL_MILLIS)
        coVerify(exactly = 1) { repository.getOrder(OWNER, CREDENTIALS, ORDER_ID) }
    }

    internal data class FinalAnswerModel(
        val name: String,
        val answer: Either<PredictionOrderStateError, PredictionOrderState>,
    ) {
        override fun toString(): String = name
    }

    private fun provideTestModels() = listOf(
        FinalAnswerModel(
            name = "live",
            answer = order(status = PredictionOrderStatus.Other(raw = "ORDER_STATUS_LIVE")).right(),
        ),
        FinalAnswerModel(
            name = "canceled",
            answer = order(status = PredictionOrderStatus.Other(raw = "ORDER_STATUS_CANCELED")).right(),
        ),
        FinalAnswerModel(name = "no status", answer = order(status = PredictionOrderStatus.Other(raw = null)).right()),
        FinalAnswerModel(name = "404", answer = PredictionOrderStateError.NotFound.left()),
        FinalAnswerModel(
            name = "4xx",
            answer = PredictionOrderStateError.Unknown(httpCode = 400, detail = "bad request").left(),
        ),
        FinalAnswerModel(
            name = "no http code",
            answer = PredictionOrderStateError.Unknown(httpCode = null, detail = "unreadable body").left(),
        ),
        FinalAnswerModel(
            name = "refused credentials",
            answer = PredictionOrderStateError.Unauthorized(detail = "invalid signature").left(),
        ),
    )

    @Test
    fun `GIVEN no stored credentials WHEN invoked THEN nothing is read and the wait fails`() = runTest {
        // Arrange
        coEvery { getApiCredentials(userWalletId = USER_WALLET_ID) } returns null

        // Act
        val actual = useCase(addresses = ADDRESSES, orderId = ORDER_ID)

        // Assert
        assertThat(actual).isInstanceOf(DelayedPredictionOrderOutcome.Failed::class.java)
        coVerify(exactly = 0) { repository.getOrder(any(), any(), any()) }
    }

    @Test
    fun `GIVEN the first read is pending WHEN time passes THEN the first read waits out one interval`() = runTest {
        // Arrange
        coEvery { repository.getOrder(OWNER, CREDENTIALS, ORDER_ID) } returns
            order(status = PredictionOrderStatus.Matched).right()

        // Act
        val job = launch { useCase(addresses = ADDRESSES, orderId = ORDER_ID) }
        advanceTimeBy(delayTimeMillis = READ_INTERVAL_MILLIS)

        // Assert
        coVerify(exactly = 0) { repository.getOrder(any(), any(), any()) }
        advanceTimeBy(delayTimeMillis = 1)
        coVerify(exactly = 1) { repository.getOrder(OWNER, CREDENTIALS, ORDER_ID) }
        job.cancel()
    }

    private fun order(status: PredictionOrderStatus) = PredictionOrderState(
        status = status,
        price = BigDecimal("0.52"),
        originalSize = BigDecimal("100"),
        sizeMatched = BigDecimal("100"),
    )

    private companion object {
        const val ORDER_ID = "0x6b2e"
        const val OWNER = "0x1111111111111111111111111111111111111111"
        const val READ_INTERVAL_MILLIS = 10_000L
        const val MAX_READS = 3

        val USER_WALLET_ID = UserWalletId("011")
        val CREDENTIALS = PolymarketApiCredentials(apiKey = "key", secret = "secret", passphrase = "pass")
        val ADDRESSES = PolymarketAddresses(
            ownerAddress = OWNER,
            depositWalletAddress = "0x2222222222222222222222222222222222222222",
            userWalletId = USER_WALLET_ID,
        )
    }
}