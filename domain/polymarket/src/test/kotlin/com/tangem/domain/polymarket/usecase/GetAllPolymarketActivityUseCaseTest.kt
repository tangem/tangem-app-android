package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketActivityRepository
import com.tangem.domain.polymarket.model.PolymarketActivity
import com.tangem.domain.polymarket.model.PolymarketActivityError
import com.tangem.domain.polymarket.model.PolymarketActivityFilter
import com.tangem.domain.polymarket.model.PolymarketActivityListConfig
import com.tangem.domain.polymarket.model.PolymarketActivityPage
import com.tangem.domain.polymarket.model.PolymarketAddresses
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

internal class GetAllPolymarketActivityUseCaseTest {

    private val repository: PolymarketActivityRepository = mockk()
    private val addresses: DerivePolymarketAddressesUseCase = mockk()
    private val wallet = UserWalletId("011")
    private val filter = PolymarketActivityFilter(PolymarketActivity.Type.BUY)
    private val config = PolymarketActivityListConfig(PolymarketAddresses("owner", "deposit", wallet), filter)
    private val useCase = GetAllPolymarketActivityUseCase(repository, addresses)

    @Test
    fun `GIVEN overlapping pages WHEN exporting THEN keeps the first record per id and the selected filter`() = runTest {
        // Arrange
        coEvery { addresses.stored(wallet) } returns config.addresses
        val first = activity("first")
        val second = activity("second")
        coEvery { repository.getActivity(config, null, 100) } returns
            Either.Right(PolymarketActivityPage(listOf(first), "next"))
        coEvery { repository.getActivity(config, "next", 100) } returns
            Either.Right(PolymarketActivityPage(listOf(first.copy(amount = BigDecimal.TEN), second), null))

        // Act
        val result = useCase(wallet, filter)

        // Assert
        assertThat(result).isEqualTo(Either.Right(listOf(first, second)))
        coVerify(exactly = 0) { addresses.invoke(any()) }
    }

    @Test
    fun `GIVEN a failed later page WHEN exporting THEN returns an error instead of a partial history`() = runTest {
        // Arrange
        coEvery { addresses.stored(wallet) } returns config.addresses
        coEvery { repository.getActivity(config, null, 100) } returns
            Either.Right(PolymarketActivityPage(listOf(activity()), "next"))
        coEvery { repository.getActivity(config, "next", 100) } returns Either.Left(PolymarketActivityError.Network)

        // Act
        val result = useCase(wallet, filter)

        // Assert
        assertThat(result).isEqualTo(Either.Left(PolymarketActivityError.Network))
    }

    @Test
    fun `GIVEN missing stored addresses WHEN exporting THEN fails without prompting or requesting history`() = runTest {
        // Arrange
        coEvery { addresses.stored(wallet) } returns null

        // Act
        val result = useCase(wallet, filter)

        // Assert
        assertThat(result.isLeft()).isTrue()
        coVerify(exactly = 0) { addresses.invoke(any()) }
        coVerify(exactly = 0) { repository.getActivity(any(), any(), any()) }
    }

    @Test
    fun `GIVEN a repeated cursor WHEN exporting THEN stops instead of looping forever`() = runTest {
        // Arrange
        coEvery { addresses.stored(wallet) } returns config.addresses
        coEvery { repository.getActivity(config, any(), 100) } returns
            Either.Right(PolymarketActivityPage(emptyList(), "next"))

        // Act
        val result = useCase(wallet, filter)

        // Assert
        assertThat(result.isLeft()).isTrue()
        coVerify(exactly = 2) { repository.getActivity(config, any(), 100) }
    }

    @Test
    fun `GIVEN an export in flight WHEN cancelled THEN does not fetch another page`() = runTest {
        // Arrange
        val started = CompletableDeferred<Unit>()
        coEvery { addresses.stored(wallet) } returns config.addresses
        coEvery { repository.getActivity(config, null, 100) } coAnswers {
            started.complete(Unit)
            CompletableDeferred<Either<PolymarketActivityError, PolymarketActivityPage>>().await()
        }
        val job = launch { useCase(wallet, filter) }
        started.await()

        // Act
        job.cancel()
        job.join()

        // Assert
        assertThat(job.isCancelled).isTrue()
        coVerify(exactly = 1) { repository.getActivity(config, any(), 100) }
    }

    private fun activity(id: String = "first") = PolymarketActivity(
        id = id,
        type = PolymarketActivity.Type.BUY,
        occurredAt = Instant.EPOCH,
        amount = BigDecimal.ONE,
    )
}