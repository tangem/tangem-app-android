package com.tangem.data.polymarket

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.polymarket.model.PolymarketPriceHistoryError
import com.tangem.domain.polymarket.model.PolymarketPriceHistoryQuery
import com.tangem.domain.polymarket.model.PolymarketPriceInterval
import com.tangem.domain.polymarket.model.PolymarketPricePoint
import com.tangem.grow.datasource.polymarket.clob.PolymarketClobApi
import com.tangem.grow.datasource.polymarket.clob.models.PolymarketPriceHistoryResponse
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.time.Instant

internal class DefaultPolymarketPriceHistoryRepositoryTest {

    private val api: PolymarketClobApi = mockk()
    private val repository = DefaultPolymarketPriceHistoryRepository(api, TestingCoroutineDispatcherProvider())
    private val query = PolymarketPriceHistoryQuery(
        PolymarketPriceHistoryQuery.Window.Interval(PolymarketPriceInterval.HOUR),
        fidelityMinutes = 1,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Intervals {
        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN interval query WHEN loading THEN only token fidelity and interval are sent`(model: IntervalCase) = runTest {
            // Arrange
            val query = query.copy(window = PolymarketPriceHistoryQuery.Window.Interval(model.interval))
            coEvery { api.getPriceHistory("token", 1, model.wireValue, null, null) } returns ApiResponse.Success(
                PolymarketPriceHistoryResponse(listOf(PolymarketPriceHistoryResponse.Point(1_787_000_100, "0.5"))),
            )

            // Act
            val actual = repository.getPriceHistory("token", query)

            // Assert
            assertThat(actual).isEqualTo(Either.Right(listOf(PolymarketPricePoint(1_787_000_100_000, BigDecimal("0.5")))))
            coVerify(exactly = 1) { api.getPriceHistory("token", 1, model.wireValue, null, null) }
        }

        private fun provideTestModels() = listOf(
            IntervalCase(PolymarketPriceInterval.HOUR, "1h"),
            IntervalCase(PolymarketPriceInterval.SIX_HOURS, "6h"),
            IntervalCase(PolymarketPriceInterval.DAY, "1d"),
            IntervalCase(PolymarketPriceInterval.WEEK, "1w"),
            IntervalCase(PolymarketPriceInterval.MONTH, "1m"),
            IntervalCase(PolymarketPriceInterval.MAX, "max"),
        )
    }

    @Test
    fun `GIVEN bounded MAX query WHEN loading THEN UTC seconds are sent without interval`() = runTest {
        // Arrange
        val range = PolymarketPriceHistoryQuery.Window.Range(Instant.ofEpochSecond(1_787_000_100), Instant.ofEpochSecond(1_787_000_200))
        coEvery { api.getPriceHistory("token", 15, null, 1_787_000_100, 1_787_000_200) } returns ApiResponse.Success(
            PolymarketPriceHistoryResponse(emptyList()),
        )

        // Act
        val actual = repository.getPriceHistory("token", PolymarketPriceHistoryQuery(range, 15))

        // Assert
        assertThat(actual).isEqualTo(Either.Right(emptyList<PolymarketPricePoint>()))
        coVerify(exactly = 1) { api.getPriceHistory("token", 15, null, 1_787_000_100, 1_787_000_200) }
    }

    @Test
    fun `GIVEN open MAX range WHEN loading THEN no end timestamp is sent`() = runTest {
        // Arrange
        val range = PolymarketPriceHistoryQuery.Window.Range(Instant.ofEpochSecond(1_787_000_100))
        coEvery { api.getPriceHistory("token", 720, null, 1_787_000_100, null) } returns ApiResponse.Success(
            PolymarketPriceHistoryResponse(emptyList()),
        )

        // Act
        val actual = repository.getPriceHistory("token", PolymarketPriceHistoryQuery(range, 720))

        // Assert
        assertThat(actual).isEqualTo(Either.Right(emptyList<PolymarketPricePoint>()))
        coVerify(exactly = 1) { api.getPriceHistory("token", 720, null, 1_787_000_100, null) }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Errors {
        @ParameterizedTest
        @ProvideTestModels
        @Suppress("UNCHECKED_CAST")
        fun `GIVEN API failure WHEN loading THEN failure is returned instead of an empty history`(model: ErrorCase) = runTest {
            // Arrange
            coEvery { api.getPriceHistory(any(), any(), any(), any(), any()) } returns
                (ApiResponse.Error(model.source) as ApiResponse<PolymarketPriceHistoryResponse>)

            // Act
            val actual = repository.getPriceHistory("token", query)

            // Assert
            assertThat(actual).isEqualTo(Either.Left(model.expected))
        }

        private fun provideTestModels() = listOf(
            ErrorCase(ApiResponseError.NetworkException(), PolymarketPriceHistoryError.Network),
            ErrorCase(ApiResponseError.TimeoutException(), PolymarketPriceHistoryError.Network),
            ErrorCase(
                ApiResponseError.HttpException(ApiResponseError.HttpException.Code.BAD_REQUEST, null, "bad range"),
                PolymarketPriceHistoryError.Unknown(400, "bad range"),
            ),
            ErrorCase(
                ApiResponseError.UnknownException(IllegalArgumentException("bad body")),
                PolymarketPriceHistoryError.Unknown(null, "bad body"),
            ),
        )
    }

    @Test
    fun `GIVEN blank token WHEN loading THEN no request is sent`() = runTest {
        // Arrange
        val assetId = " "

        // Act
        val actual = repository.getPriceHistory(assetId, query)

        // Assert
        assertThat(actual).isEqualTo(Either.Left(PolymarketPriceHistoryError.InvalidToken))
        coVerify(exactly = 0) { api.getPriceHistory(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN cancellation WHEN loading THEN cancellation propagates`() = runTest {
        // Arrange
        val cancellation = CancellationException("period switched")
        coEvery { api.getPriceHistory(any(), any(), any(), any(), any()) } throws cancellation

        // Act
        val actual = runCatching { repository.getPriceHistory("token", query) }.exceptionOrNull()

        // Assert
        assertThat(actual).isSameInstanceAs(cancellation)
    }

    data class IntervalCase(val interval: PolymarketPriceInterval, val wireValue: String)
    data class ErrorCase(val source: ApiResponseError, val expected: PolymarketPriceHistoryError)
}