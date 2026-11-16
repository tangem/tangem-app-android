package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.PolymarketPriceHistoryRepository
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketHistoryToken
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketMarketVersion
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.domain.polymarket.model.PolymarketPriceHistoryError
import com.tangem.domain.polymarket.model.PolymarketPriceHistoryQuery
import com.tangem.domain.polymarket.model.PolymarketPriceInterval
import com.tangem.domain.polymarket.model.PolymarketPricePoint
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class GetPolymarketPriceHistoryUseCaseTest {

    private val repository: PolymarketPriceHistoryRepository = mockk()
    private val useCase = GetPolymarketPriceHistoryUseCase(repository)
    private val query = PolymarketPriceHistoryQuery(
        PolymarketPriceHistoryQuery.Window.Interval(PolymarketPriceInterval.HOUR),
        fidelityMinutes = 1,
    )
    private val yes = PolymarketHistoryToken("market", "yes", PolymarketMarketVersion.V2)
    private val no = yes.copy(assetId = "no")
    private val points = listOf(PolymarketPricePoint(1_000, BigDecimal("0.5")))

    @Test
    fun `GIVEN one failed row WHEN requesting history THEN other rows survive with independent results`() = runTest {
        // Arrange
        val event = event()
        coEvery { repository.getPriceHistory("yes", query) } returns points.right()
        coEvery { repository.getPriceHistory("no", query) } returns PolymarketPriceHistoryError.Network.left()

        // Act
        val actual = useCase(event, setOf(yes, no), query)

        // Assert
        assertThat(actual).isEqualTo(mapOf(yes to points.right(), no to PolymarketPriceHistoryError.Network.left()))
    }

    @Test
    fun `GIVEN empty successful row WHEN requesting history THEN empty is distinct from failure`() = runTest {
        // Arrange
        val event = event()
        coEvery { repository.getPriceHistory("yes", query) } returns emptyList<PolymarketPricePoint>().right()

        // Act
        val actual = useCase(event, setOf(yes), query)

        // Assert
        assertThat(actual).isEqualTo(mapOf(yes to emptyList<PolymarketPricePoint>().right()))
    }

    @Test
    fun `GIVEN stale market version or foreign token WHEN requesting history THEN invalid rows never reach API`() = runTest {
        // Arrange
        val event = event()
        val invalid = setOf(
            yes.copy(marketId = "another-market"),
            yes.copy(version = PolymarketMarketVersion.V1),
            yes.copy(assetId = "another-token"),
            yes.copy(assetId = ""),
        )

        // Act
        val actual = useCase(event, invalid, query)

        // Assert
        assertThat(actual).isEqualTo(invalid.associateWith { PolymarketPriceHistoryError.InvalidToken.left() })
        coVerify(exactly = 0) { repository.getPriceHistory(any(), any()) }
    }

    @Test
    fun `GIVEN market from another event WHEN requesting history THEN it is rejected`() = runTest {
        // Arrange
        val event = event(marketEventId = "another-event")

        // Act
        val actual = useCase(event, setOf(yes), query)

        // Assert
        assertThat(actual).isEqualTo(mapOf(yes to PolymarketPriceHistoryError.InvalidToken.left()))
        coVerify(exactly = 0) { repository.getPriceHistory(any(), any()) }
    }

    @Test
    fun `GIVEN slow rows WHEN period request is cancelled THEN every child is cancelled without a result`() = runTest {
        // Arrange
        val event = event()
        val started = mapOf("yes" to CompletableDeferred<Unit>(), "no" to CompletableDeferred<Unit>())
        val cancelled = mutableSetOf<String>()
        coEvery { repository.getPriceHistory(any(), query) } coAnswers {
            val assetId = firstArg<String>()
            started.getValue(assetId).complete(Unit)
            try {
                awaitCancellation()
            } finally {
                cancelled.add(assetId)
            }
        }
        val request = async { useCase(event, setOf(yes, no), query) }
        started.values.forEach { it.await() }

        // Act
        request.cancelAndJoin()

        // Assert
        assertThat(request.isCancelled).isTrue()
        assertThat(cancelled).containsExactly("yes", "no")
    }

    @Test
    fun `GIVEN two periods finishing in reverse order WHEN requesting history THEN each result keeps its own request`() = runTest {
        // Arrange
        val event = event()
        val secondQuery = query.copy(window = PolymarketPriceHistoryQuery.Window.Interval(PolymarketPriceInterval.DAY))
        val firstReply = CompletableDeferred<Either<PolymarketPriceHistoryError, List<PolymarketPricePoint>>>()
        val firstStarted = CompletableDeferred<Unit>()
        coEvery { repository.getPriceHistory("yes", query) } coAnswers {
            firstStarted.complete(Unit)
            firstReply.await()
        }
        coEvery { repository.getPriceHistory("yes", secondQuery) } returns emptyList<PolymarketPricePoint>().right()
        val first = async { useCase(event, setOf(yes), query) }
        firstStarted.await()

        // Act
        val second = useCase(event, setOf(yes), secondQuery)
        firstReply.complete(points.right())
        val firstResult = first.await()

        // Assert
        assertThat(second).isEqualTo(mapOf(yes to emptyList<PolymarketPricePoint>().right()))
        assertThat(firstResult).isEqualTo(mapOf(yes to points.right()))
        coVerify(exactly = 1) { repository.getPriceHistory("yes", query) }
        coVerify(exactly = 1) { repository.getPriceHistory("yes", secondQuery) }
    }

    private fun event(marketEventId: String = "event"): PolymarketEvent {
        val outcomes = listOf("yes", "no").map { token -> mockk<PolymarketOutcome> { every { assetId } returns token } }
        val market = mockk<PolymarketMarket> {
            every { id } returns "market"
            every { eventId } returns marketEventId
            every { version } returns PolymarketMarketVersion.V2
            every { this@mockk.outcomes } returns outcomes
        }
        return mockk {
            every { id } returns "event"
            every { markets } returns listOf(market)
        }
    }
}