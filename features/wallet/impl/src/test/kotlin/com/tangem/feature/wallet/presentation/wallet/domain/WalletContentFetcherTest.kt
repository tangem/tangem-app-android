package com.tangem.feature.wallet.presentation.wallet.domain

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.wallet.WalletBalanceFetcher
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class WalletContentFetcherTest {

    private val walletBalanceFetcher: WalletBalanceFetcher = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(walletBalanceFetcher)
    }

    @Test
    fun `GIVEN nothing fetched yet WHEN invoke THEN wallet balance is fetched`() = runTest {
        // Arrange
        val fetcher = createFetcher()
        coEvery { walletBalanceFetcher(params) } returns Either.Right(Unit)

        // Act
        fetcher(userWalletId = userWalletId)

        // Assert
        coVerify(exactly = 1) { walletBalanceFetcher(params) }
    }

    @Test
    fun `GIVEN fetching completed WHEN invoke THEN fetching is skipped`() = runTest {
        // Arrange
        val fetcher = createFetcher()
        coEvery { walletBalanceFetcher(params) } returns Either.Right(Unit)
        fetcher(userWalletId = userWalletId)

        // Act
        fetcher(userWalletId = userWalletId)

        // Assert
        coVerify(exactly = 1) { walletBalanceFetcher(params) }
    }

    @Test
    fun `GIVEN fetching completed WHEN invoke with force update THEN wallet balance is fetched again`() = runTest {
        // Arrange
        val fetcher = createFetcher()
        coEvery { walletBalanceFetcher(params) } returns Either.Right(Unit)
        fetcher(userWalletId = userWalletId)

        // Act
        fetcher(userWalletId = userWalletId, forceUpdate = true)

        // Assert
        coVerify(exactly = 2) { walletBalanceFetcher(params) }
    }

    @Test
    fun `GIVEN fetching cancelled WHEN invoke THEN wallet balance is fetched again`() = runTest {
        // Arrange
        val fetcher = createFetcher()
        coEvery { walletBalanceFetcher(params) } coAnswers { awaitCancellation() }

        val fetchingJob = launch { fetcher(userWalletId = userWalletId) }
        runCurrent()
        fetchingJob.cancel()
        runCurrent()

        coEvery { walletBalanceFetcher(params) } returns Either.Right(Unit)

        // Act
        fetcher(userWalletId = userWalletId)

        // Assert
        coVerify(exactly = 2) { walletBalanceFetcher(params) }
    }

    @Test
    fun `GIVEN fetching completed WHEN wallet deleted THEN next invoke fetches wallet balance again`() = runTest {
        // Arrange
        val fetcher = createFetcher()
        coEvery { walletBalanceFetcher(params) } returns Either.Right(Unit)
        fetcher(userWalletId = userWalletId)

        // Act
        fetcher.clear(userWalletIds = listOf(userWalletId))
        fetcher(userWalletId = userWalletId)

        // Assert
        coVerify(exactly = 2) { walletBalanceFetcher(params) }
    }

    @Test
    fun `GIVEN fetching in progress WHEN wallet deleted THEN fetching is cancelled`() = runTest {
        // Arrange
        val fetcher = createFetcher()
        coEvery { walletBalanceFetcher(params) } coAnswers { awaitCancellation() }

        val fetchingJob = launch { fetcher(userWalletId = userWalletId) }
        runCurrent()

        // Act
        fetcher.clear(userWalletIds = listOf(userWalletId))
        runCurrent()

        // Assert
        assertThat(fetchingJob.isCompleted).isTrue()
    }

    @Test
    fun `GIVEN fetching completed WHEN another wallet deleted THEN fetching is still skipped`() = runTest {
        // Arrange
        val fetcher = createFetcher()
        coEvery { walletBalanceFetcher(params) } returns Either.Right(Unit)
        fetcher(userWalletId = userWalletId)

        // Act
        fetcher.clear(userWalletIds = listOf(otherUserWalletId))
        fetcher(userWalletId = userWalletId)

        // Assert
        coVerify(exactly = 1) { walletBalanceFetcher(params) }
    }

    private fun TestScope.createFetcher(): WalletContentFetcher {
        val testDispatcher = StandardTestDispatcher(testScheduler)

        return WalletContentFetcher(
            walletBalanceFetcher = walletBalanceFetcher,
            dispatchers = TestingCoroutineDispatcherProvider(
                main = testDispatcher,
                mainImmediate = testDispatcher,
                io = testDispatcher,
                default = testDispatcher,
                single = testDispatcher,
            ),
        )
    }

    private companion object {
        val userWalletId = UserWalletId(stringValue = "0011")
        val otherUserWalletId = UserWalletId(stringValue = "0022")
        val params = WalletBalanceFetcher.Params(userWalletId = userWalletId)
    }
}