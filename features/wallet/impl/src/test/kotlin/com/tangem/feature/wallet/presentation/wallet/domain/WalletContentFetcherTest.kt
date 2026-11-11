package com.tangem.feature.wallet.presentation.wallet.domain

import arrow.core.Either
import arrow.core.None
import arrow.core.Some
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.wallet.WalletBalanceFetcher
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class WalletContentFetcherTest {

    private val walletBalanceFetcher: WalletBalanceFetcher = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val accountsCRUDRepository: AccountsCRUDRepository = mockk()
    private val singleAccountListFetcher: SingleAccountListFetcher = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(walletBalanceFetcher, userWalletsListRepository, accountsCRUDRepository, singleAccountListFetcher)
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(emptyList())
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

    @Nested
    inner class LockedWallet {

        @Test
        fun `GIVEN locked wallet without saved accounts WHEN invoke THEN only accounts are fetched`() = runTest {
            // Arrange
            val fetcher = createFetcher()
            setWallet(isLocked = true)
            coEvery { accountsCRUDRepository.getAccountListSync(userWalletId) } returns None
            coEvery { singleAccountListFetcher(accountListParams) } returns Either.Right(Unit)

            // Act
            fetcher(userWalletId = userWalletId)

            // Assert
            coVerify(exactly = 1) { singleAccountListFetcher(accountListParams) }
            coVerify(exactly = 0) { walletBalanceFetcher(any()) }
        }

        @Test
        fun `GIVEN locked wallet with saved accounts WHEN invoke THEN nothing is fetched`() = runTest {
            // Arrange
            val fetcher = createFetcher()
            setWallet(isLocked = true)
            coEvery { accountsCRUDRepository.getAccountListSync(userWalletId) } returns Some(mockk())

            // Act
            fetcher(userWalletId = userWalletId)

            // Assert
            coVerify(exactly = 0) { singleAccountListFetcher(any()) }
            coVerify(exactly = 0) { walletBalanceFetcher(any()) }
        }

        @Test
        fun `GIVEN locked single currency wallet WHEN invoke THEN nothing is fetched`() = runTest {
            // Arrange
            val fetcher = createFetcher()
            setWallet(isLocked = true, isMultiCurrency = false)

            // Act
            fetcher(userWalletId = userWalletId)

            // Assert
            coVerify(exactly = 0) { accountsCRUDRepository.getAccountListSync(any()) }
            coVerify(exactly = 0) { singleAccountListFetcher(any()) }
            coVerify(exactly = 0) { walletBalanceFetcher(any()) }
        }

        @Test
        fun `GIVEN accounts fetched for locked wallet WHEN it is unlocked THEN wallet balance is fetched`() = runTest {
            // Arrange
            val fetcher = createFetcher()
            setWallet(isLocked = true)
            coEvery { accountsCRUDRepository.getAccountListSync(userWalletId) } returns None
            coEvery { singleAccountListFetcher(accountListParams) } returns Either.Right(Unit)
            coEvery { walletBalanceFetcher(params) } returns Either.Right(Unit)
            fetcher(userWalletId = userWalletId)
            setWallet(isLocked = false)

            // Act
            fetcher(userWalletId = userWalletId)

            // Assert
            coVerify(exactly = 1) { walletBalanceFetcher(params) }
        }

        @Test
        fun `GIVEN accounts fetching in progress WHEN wallet deleted THEN fetching is cancelled`() = runTest {
            // Arrange
            val fetcher = createFetcher()
            setWallet(isLocked = true)
            coEvery { accountsCRUDRepository.getAccountListSync(userWalletId) } returns None
            coEvery { singleAccountListFetcher(accountListParams) } coAnswers { awaitCancellation() }

            val fetchingJob = launch { fetcher(userWalletId = userWalletId) }
            runCurrent()

            // Act
            fetcher.clear(userWalletIds = listOf(userWalletId))
            runCurrent()

            // Assert
            assertThat(fetchingJob.isCompleted).isTrue()
        }

        @Test
        fun `GIVEN accounts fetching in progress WHEN invoke again THEN accounts are fetched once`() = runTest {
            // Arrange
            val fetcher = createFetcher()
            setWallet(isLocked = true)
            coEvery { accountsCRUDRepository.getAccountListSync(userWalletId) } returns None
            coEvery { singleAccountListFetcher(accountListParams) } coAnswers { awaitCancellation() }

            val fetchingJob = launch { fetcher(userWalletId = userWalletId) }
            runCurrent()

            // Act
            fetcher(userWalletId = userWalletId)

            // Assert
            coVerify(exactly = 1) { singleAccountListFetcher(accountListParams) }
            fetchingJob.cancel()
        }

        private fun setWallet(isLocked: Boolean, isMultiCurrency: Boolean = true) {
            val userWallet = mockk<UserWallet.Cold> {
                every { this@mockk.walletId } returns userWalletId
                every { this@mockk.isLocked } returns isLocked
                every { this@mockk.isMultiCurrency } returns isMultiCurrency
            }

            every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))
        }
    }

    private fun TestScope.createFetcher(): WalletContentFetcher {
        val testDispatcher = StandardTestDispatcher(testScheduler)

        return WalletContentFetcher(
            walletBalanceFetcher = walletBalanceFetcher,
            userWalletsListRepository = userWalletsListRepository,
            accountsCRUDRepository = accountsCRUDRepository,
            singleAccountListFetcher = singleAccountListFetcher,
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
        val accountListParams = SingleAccountListFetcher.Params(userWalletId = userWalletId)
    }
}