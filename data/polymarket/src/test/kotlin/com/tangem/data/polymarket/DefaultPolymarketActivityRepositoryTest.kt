package com.tangem.data.polymarket

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketActivity
import com.tangem.domain.polymarket.model.PolymarketActivityError
import com.tangem.domain.polymarket.model.PolymarketActivityFilter
import com.tangem.domain.polymarket.model.PolymarketActivityListConfig
import com.tangem.domain.polymarket.model.PolymarketActivityPage
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.grow.datasource.polymarket.PolymarketApi
import com.tangem.grow.datasource.polymarket.models.PolymarketActivityDto
import com.tangem.grow.datasource.polymarket.models.PolymarketActivityPageResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketActivityRedeemDto
import com.tangem.grow.datasource.polymarket.models.PolymarketDepositReconcileResponse
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultPolymarketActivityRepositoryTest {

    private val api: PolymarketApi = mockk()
    private val repository = DefaultPolymarketActivityRepository(api, TestingCoroutineDispatcherProvider())
    private val addresses: PolymarketAddresses = mockk {
        every { userWalletId } returns UserWalletId("011")
        every { depositWalletAddress } returns "0xdeposit"
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(api)
    }

    @Test
    fun `GIVEN a redemption filter WHEN loading THEN scopes the request and preserves amount and timestamp`() = runTest {
        // Arrange
        val filter = PolymarketActivityFilter(PolymarketActivity.Type.REDEEM, PolymarketActivity.RedeemResult.SPLIT)
        val response = PolymarketActivityPageResponse(
            items = listOf(
                activity().copy(
                    type = "REDEEM", amount = "1.123456789123456789", redeem = PolymarketActivityRedeemDto("SPLIT"),
                ),
            ),
            nextCursor = "next+/==",
        )
        coEvery { api.getActivity(any(), any(), any(), any(), any(), any()) } returns ApiResponse.Success(response)
        val expected = PolymarketActivityPage(
            items = listOf(
                PolymarketActivity(
                    id = "operation-1",
                    type = PolymarketActivity.Type.REDEEM,
                    occurredAt = Instant.parse("2026-09-28T10:00:00Z"),
                    amount = BigDecimal("1.123456789123456789"),
                    redeemResult = PolymarketActivity.RedeemResult.SPLIT,
                ),
            ),
            nextCursor = "next+/==",
        )

        // Act
        val actual = repository.getActivity(PolymarketActivityListConfig(addresses, filter), "previous+/==", 30)

        // Assert
        assertThat(actual).isEqualTo(Either.Right(expected))
        coVerify(exactly = 1) { api.getActivity("011", "0xdeposit", "REDEEM", "SPLIT", 30, "previous+/==") }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN an invalid row between valid rows WHEN loading THEN skips only that row`(
        invalid: PolymarketActivityDto,
    ) = runTest {
        // Arrange
        coEvery { api.getActivity(any(), any(), any(), any(), any(), any()) } returns ApiResponse.Success(
            PolymarketActivityPageResponse(
                items = listOf(activity(), invalid.copy(id = "invalid"), activity().copy(id = "operation-2")),
                nextCursor = "next+/==",
            ),
        )
        val expected = PolymarketActivity(
            id = "operation-1",
            type = PolymarketActivity.Type.DEPOSIT,
            occurredAt = Instant.parse("2026-09-28T10:00:00Z"),
            amount = BigDecimal("10.00"),
        )

        // Act
        val actual = repository.getActivity(PolymarketActivityListConfig(addresses, PolymarketActivityFilter()), null, 30)

        // Assert
        assertThat(actual).isEqualTo(
            Either.Right(PolymarketActivityPage(listOf(expected, expected.copy(id = "operation-2")), "next+/==")),
        )
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["next+/=="])
    fun `GIVEN only invalid rows WHEN loading THEN returns an empty page with the server cursor`(cursor: String?) =
        runTest {
            // Arrange
            coEvery { api.getActivity(any(), any(), any(), any(), any(), any()) } returns ApiResponse.Success(
                PolymarketActivityPageResponse(provideTestModels(), cursor),
            )

            // Act
            val actual = repository.getActivity(
                PolymarketActivityListConfig(addresses, PolymarketActivityFilter()), null, 30,
            )

            // Assert
            assertThat(actual).isEqualTo(Either.Right(PolymarketActivityPage(emptyList(), cursor)))
        }

    @Test
    fun `GIVEN a cancelled request WHEN loading THEN cancellation reaches the caller`() = runTest {
        // Arrange
        coEvery { api.getActivity(any(), any(), any(), any(), any(), any()) } throws CancellationException("left screen")

        // Act
        val error = runCatching {
            repository.getActivity(PolymarketActivityListConfig(addresses, PolymarketActivityFilter()), null, 30)
        }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(CancellationException::class.java)
    }

    @Test
    fun `GIVEN an accepted scan WHEN reconciling THEN returns the server delay for the selected wallet`() = runTest {
        // Arrange
        coEvery { api.reconcileDeposits(any(), any()) } returns ApiResponse.Success(
            PolymarketDepositReconcileResponse("SCANNING", 12),
        )

        // Act
        val actual = repository.reconcileDeposits(addresses)

        // Assert
        assertThat(actual).isEqualTo(Either.Right(12.seconds))
        coVerify(exactly = 1) { api.reconcileDeposits("011", "0xdeposit") }
    }

    @Test
    fun `GIVEN an invalid scan delay WHEN reconciling THEN returns a contract error`() = runTest {
        // Arrange
        coEvery { api.reconcileDeposits(any(), any()) } returns ApiResponse.Success(
            PolymarketDepositReconcileResponse("SCANNING", -1),
        )

        // Act
        val actual = repository.reconcileDeposits(addresses)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(PolymarketActivityError.Unknown::class.java)
    }

    private fun provideTestModels() = listOf(
        activity().copy(type = "TRANSFER"),
        activity().copy(occurredAt = "garbage"),
        activity().copy(amount = "abc"),
        activity().copy(type = "REDEEM", redeem = PolymarketActivityRedeemDto("VOID")),
    )

    private fun activity() = PolymarketActivityDto(
        id = "operation-1", type = "DEPOSIT", occurredAt = "2026-09-28T13:00:00+03:00", amount = "10.00",
    )
}