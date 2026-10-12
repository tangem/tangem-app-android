package com.tangem.data.polymarket

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketClosedPositionStatus
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionId
import com.tangem.domain.polymarket.model.PolymarketPositionResult
import com.tangem.domain.polymarket.model.PolymarketPositionsError
import com.tangem.domain.polymarket.model.PolymarketPositionsPage
import com.tangem.domain.polymarket.model.PolymarketResolutionStatus
import com.tangem.grow.datasource.polymarket.PolymarketApi
import com.tangem.grow.datasource.polymarket.data.PolymarketDataApi
import com.tangem.grow.datasource.polymarket.data.models.PolymarketPositionsValueResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketClosedPositionsResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketPositionResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketPositionsResponse
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.time.Instant

internal class DefaultPolymarketPositionsRepositoryTest {

    private val api: PolymarketApi = mockk()
    private val dataApi: PolymarketDataApi = mockk()
    private val repository = DefaultPolymarketPositionsRepository(api, dataApi, TestingCoroutineDispatcherProvider())
    private val moshi = Moshi.Builder().build()
    private val addresses: PolymarketAddresses = mockk {
        every { userWalletId } returns UserWalletId(WALLET_ID)
        every { depositWalletAddress } returns DEPOSIT_ADDRESS
    }

    @Test
    fun `GIVEN decimal strings and missing metadata WHEN reading open positions THEN precision survives`() = runTest {
        // Arrange
        coEvery { api.getPositions(any(), any(), any(), any()) } returns ApiResponse.Success(openResponse())
        val expected = PolymarketPosition.Open(
            id = POSITION_ID,
            title = "Will it happen?",
            iconUrl = null,
            outcome = "Over",
            outcomeIndex = null,
            endDate = null,
            avgPrice = BigDecimal("0.3157"),
            curPrice = BigDecimal("0.5"),
            realizedPnl = BigDecimal("12.34"),
            marketId = null,
            eventId = null,
            eventTitle = null,
            size = BigDecimal("123456789.123456789123456789"),
            initialValue = BigDecimal("10"),
            currentValue = BigDecimal("20"),
            cashPnl = BigDecimal("10"),
            percentPnl = BigDecimal("100"),
            totalBought = BigDecimal("5"),
            isRedeemable = false,
            isNegRisk = true,
            isAcceptingOrders = false,
            isLive = false,
            resolutionStatus = PolymarketResolutionStatus.UNKNOWN,
            result = null,
        )

        // Act
        val result = repository.getOpenPositions(addresses, limit = 3, offset = 20)

        // Assert
        assertThat(result).isEqualTo(Either.Right(PolymarketPositionsPage(listOf(expected), hasNext = true)))
        coVerify(exactly = 1) { api.getPositions(WALLET_ID, DEPOSIT_ADDRESS, 3, 20) }
    }

    @Test
    fun `GIVEN a claimed losing position with earlier profit WHEN reading history THEN both are preserved`() = runTest {
        // Arrange
        val response = closedResponse()
        coEvery { api.getClosedPositions(any(), any(), any(), any()) } returns ApiResponse.Success(response)
        val expected = PolymarketPosition.Closed(
            id = POSITION_ID,
            title = "Will it happen?",
            iconUrl = null,
            outcome = "No",
            outcomeIndex = 1,
            endDate = null,
            avgPrice = BigDecimal("0.5"),
            curPrice = BigDecimal.ZERO,
            realizedPnl = BigDecimal("2.75"),
            totalBought = BigDecimal.TEN,
            closedAt = Instant.parse("2026-09-07T10:00:00Z"),
            status = PolymarketClosedPositionStatus.LOST,
        )

        // Act
        val result = repository.getClosedPositions(addresses, limit = 10, offset = 30)

        // Assert
        assertThat(result).isEqualTo(Either.Right(PolymarketPositionsPage(listOf(expected), hasNext = false)))
        coVerify(exactly = 1) { api.getClosedPositions(WALLET_ID, DEPOSIT_ADDRESS, 10, 30) }
    }

    @Test
    fun `GIVEN an indexing delay WHEN reading one outcome THEN null remains a successful response`() = runTest {
        // Arrange
        val response = moshi.adapter(PolymarketPositionResponse::class.java).fromJson("""{"position":null}""")!!
        coEvery { api.getPosition(any(), any(), any(), any()) } returns ApiResponse.Success(response)

        // Act
        val result = repository.getPosition(addresses, POSITION_ID)

        // Assert
        assertThat(result).isEqualTo(Either.Right(null))
        coVerify(exactly = 1) { api.getPosition(WALLET_ID, DEPOSIT_ADDRESS, "condition", "asset") }
    }

    @Test
    fun `GIVEN an empty portfolio WHEN reading positions THEN the result is empty and successful`() = runTest {
        // Arrange
        coEvery { api.getPositions(any(), any(), any(), any()) } returns ApiResponse.Success(
            moshi.adapter(PolymarketPositionsResponse::class.java).fromJson("""{"positions":[],"hasNext":false}""")!!,
        )

        // Act
        val result = repository.getOpenPositions(addresses, limit = 3, offset = 0)

        // Assert
        assertThat(result).isEqualTo(Either.Right(PolymarketPositionsPage(emptyList<PolymarketPosition.Open>(), false)))
    }

    @ParameterizedTest
    @ProvideTestModels
    @Suppress("UNCHECKED_CAST")
    fun `GIVEN an API error WHEN reading positions THEN the result preserves the failure`(model: ApiErrorTestModel) =
        runTest {
            // Arrange
            coEvery { api.getPositions(any(), any(), any(), any()) } returns
                ApiResponse.Error(model.apiError) as ApiResponse<PolymarketPositionsResponse>

            // Act
            val result = repository.getOpenPositions(addresses, limit = 3, offset = 0)

            // Assert
            assertThat(result).isEqualTo(Either.Left(model.expected))
        }

    @Test
    fun `GIVEN a valued wallet WHEN reading the positions value THEN the first entry is returned`() = runTest {
        // Arrange
        coEvery { dataApi.getPositionsValue(user = any()) } returns ApiResponse.Success(
            listOf(PolymarketPositionsValueResponse(user = "0xdw", value = BigDecimal("12.34"))),
        )

        // Act
        val result = repository.getPositionsValue(addresses)

        // Assert
        assertThat(result).isEqualTo(Either.Right(BigDecimal("12.34")))
    }

    @Test
    fun `GIVEN no holdings WHEN reading the positions value THEN zero is returned`() = runTest {
        // Arrange
        coEvery { dataApi.getPositionsValue(user = any()) } returns ApiResponse.Success(emptyList())

        // Act
        val result = repository.getPositionsValue(addresses)

        // Assert
        assertThat(result).isEqualTo(Either.Right(BigDecimal.ZERO))
    }

    @Test
    fun `GIVEN the data API is unreachable WHEN reading the positions value THEN the failure is typed`() = runTest {
        // Arrange
        @Suppress("UNCHECKED_CAST")
        coEvery { dataApi.getPositionsValue(user = any()) } returns
            ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<List<PolymarketPositionsValueResponse>>

        // Act
        val result = repository.getPositionsValue(addresses)

        // Assert
        assertThat(result).isEqualTo(Either.Left(PolymarketPositionsError.Network))
    }

    @Test
    fun `GIVEN request cancellation WHEN reading a position THEN cancellation propagates`() = runTest {
        // Arrange
        val cancellation = CancellationException("Screen closed")
        coEvery { api.getPosition(any(), any(), any(), any()) } throws cancellation

        // Act
        val error = runCatching { repository.getPosition(addresses, POSITION_ID) }.exceptionOrNull()

        // Assert
        assertThat(error).isSameInstanceAs(cancellation)
    }

    @Test
    fun `GIVEN malformed money WHEN reading a position THEN the conversion returns a typed failure`() = runTest {
        // Arrange
        val dto = openResponse().positions.single().copy(currentValue = "not a decimal")
        coEvery { api.getPosition(any(), any(), any(), any()) } returns ApiResponse.Success(PolymarketPositionResponse(dto))

        // Act
        val result = repository.getPosition(addresses, POSITION_ID)

        // Assert
        assertThat(result.leftOrNull()).isInstanceOf(PolymarketPositionsError.Unknown::class.java)
    }

    @Test
    fun `GIVEN a malformed closing date WHEN reading history THEN the conversion returns a typed failure`() = runTest {
        // Arrange
        coEvery { api.getClosedPositions(any(), any(), any(), any()) } returns
            ApiResponse.Success(closedResponse(closedAt = "not a date"))

        // Act
        val result = repository.getClosedPositions(addresses, limit = 3, offset = 0)

        // Assert
        assertThat(result.leftOrNull()).isInstanceOf(PolymarketPositionsError.Unknown::class.java)
    }

    @Test
    fun `GIVEN new resolution and result statuses WHEN reading positions THEN the row survives as unknown`() = runTest {
        // Arrange
        val dto = openResponse().positions.single().copy(resolutionStatus = "NEW_STATE", status = "NEW_RESULT")
        coEvery { api.getPosition(any(), any(), any(), any()) } returns ApiResponse.Success(PolymarketPositionResponse(dto))

        // Act
        val result = repository.getPosition(addresses, POSITION_ID).getOrNull()

        // Assert
        assertThat(result?.resolutionStatus).isEqualTo(PolymarketResolutionStatus.UNKNOWN)
        assertThat(result?.result).isEqualTo(PolymarketPositionResult.UNKNOWN)
    }

    private fun openResponse(): PolymarketPositionsResponse =
        moshi.adapter(PolymarketPositionsResponse::class.java).fromJson(
            """
                {"positions":[{"conditionId":"condition","assetId":"asset","title":"Will it happen?",
                "outcome":"Over","size":"123456789.123456789123456789","avgPrice":"0.3157","curPrice":"0.5",
                "initialValue":"10","currentValue":"20","cashPnl":"10","percentPnl":"100","realizedPnl":"12.34",
                "totalBought":"5","redeemable":false,"negRisk":true,"acceptingOrders":false,"live":false,
                "resolutionStatus":"UNKNOWN"}],"hasNext":true}
            """.trimIndent(),
        )!!

    private fun closedResponse(closedAt: String = "2026-09-07T10:00:00Z"): PolymarketClosedPositionsResponse =
        moshi.adapter(PolymarketClosedPositionsResponse::class.java).fromJson(
            """
                {"positions":[{"conditionId":"condition","assetId":"asset","title":"Will it happen?",
                "outcome":"No","outcomeIndex":1,"avgPrice":"0.5","curPrice":"0","totalBought":"10",
                "realizedPnl":"2.75","closedAt":"$closedAt","status":"LOST"}],"hasNext":false}
            """.trimIndent(),
        )!!

    data class ApiErrorTestModel(val apiError: ApiResponseError, val expected: PolymarketPositionsError)

    private companion object {
        val WALLET_ID = "a".repeat(64)
        const val DEPOSIT_ADDRESS = "0x1234567890123456789012345678901234567890"
        val POSITION_ID = PolymarketPositionId(conditionId = "condition", assetId = "asset")

        @JvmStatic
        fun provideTestModels() = listOf(
            ApiErrorTestModel(ApiResponseError.NetworkException(), PolymarketPositionsError.Network),
            ApiErrorTestModel(ApiResponseError.TimeoutException(), PolymarketPositionsError.Network),
            ApiErrorTestModel(
                ApiResponseError.HttpException(
                    code = ApiResponseError.HttpException.Code.SERVICE_UNAVAILABLE,
                    message = "Service unavailable",
                    errorBody = "Try again later",
                ),
                PolymarketPositionsError.Unknown(httpCode = 503, detail = "Try again later"),
            ),
            ApiErrorTestModel(
                ApiResponseError.UnknownException(IllegalStateException("Unreadable response")),
                PolymarketPositionsError.Unknown(httpCode = null, detail = "Unreadable response"),
            ),
        )
    }
}