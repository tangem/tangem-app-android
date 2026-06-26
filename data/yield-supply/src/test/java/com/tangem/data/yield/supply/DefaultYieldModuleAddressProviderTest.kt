package com.tangem.data.yield.supply

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.WalletManager
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultYieldModuleAddressProviderTest {

    private val walletManager: WalletManager = mockk()
    private val walletManagersFacade: WalletManagersFacade = mockk()

    private val provider = DefaultYieldModuleAddressProvider(
        walletManagersFacade = walletManagersFacade,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val userWalletId = UserWalletId("abcdef012345")
    private val otherWalletId = UserWalletId("fedcba543210")
    private val network = network()

    @BeforeEach
    fun setUp() {
        clearMocks(walletManager, walletManagersFacade)
        provider.invalidate(null)
        coEvery {
            walletManagersFacade.getOrCreateWalletManager(any(), any(), any())
        } returns walletManager
    }

    @Test
    fun `GIVEN non-zero address WHEN getOrFetch THEN returns and caches it`() = runTest {
        // Arrange
        coEvery { walletManager.getYieldModuleAddress() } returns ADDRESS

        // Act
        val first = provider.getOrFetch(userWalletId, network)
        val second = provider.getOrFetch(userWalletId, network)

        // Assert
        assertThat(first).isEqualTo(ADDRESS)
        assertThat(second).isEqualTo(ADDRESS)
        coVerify(exactly = 1) { walletManagersFacade.getOrCreateWalletManager(any(), any(), any()) }
    }

    @Test
    fun `GIVEN zero address WHEN getOrFetch THEN returns null and does not cache`() = runTest {
        // Arrange
        coEvery { walletManager.getYieldModuleAddress() } returns EthereumUtils.ZERO_ADDRESS

        // Act
        val first = provider.getOrFetch(userWalletId, network)
        val second = provider.getOrFetch(userWalletId, network)

        // Assert — null result is never cached, so the manager is queried again
        assertThat(first).isNull()
        assertThat(second).isNull()
        coVerify(exactly = 2) { walletManager.getYieldModuleAddress() }
    }

    @Test
    fun `GIVEN missing wallet manager WHEN getOrFetch THEN throws`() = runTest {
        // Arrange
        coEvery { walletManagersFacade.getOrCreateWalletManager(any(), any(), any()) } returns null

        // Act
        val error = runCatching { provider.getOrFetch(userWalletId, network) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `GIVEN cached address WHEN invalidate for that wallet THEN it is refetched`() = runTest {
        // Arrange
        coEvery { walletManager.getYieldModuleAddress() } returns ADDRESS
        provider.getOrFetch(userWalletId, network)

        // Act
        provider.invalidate(userWalletId)
        provider.getOrFetch(userWalletId, network)

        // Assert
        coVerify(exactly = 2) { walletManagersFacade.getOrCreateWalletManager(any(), any(), any()) }
    }

    @Test
    fun `GIVEN two cached wallets WHEN invalidate one THEN only that one is refetched`() = runTest {
        // Arrange
        coEvery { walletManager.getYieldModuleAddress() } returns ADDRESS
        provider.getOrFetch(userWalletId, network)
        provider.getOrFetch(otherWalletId, network)

        // Act
        provider.invalidate(userWalletId)
        provider.getOrFetch(userWalletId, network) // refetched
        provider.getOrFetch(otherWalletId, network) // still cached

        // Assert — 2 initial fetches + 1 refetch for the invalidated wallet only
        coVerify(exactly = 3) { walletManagersFacade.getOrCreateWalletManager(any(), any(), any()) }
    }

    @Test
    fun `GIVEN cached addresses WHEN invalidate all THEN every wallet is refetched`() = runTest {
        // Arrange
        coEvery { walletManager.getYieldModuleAddress() } returns ADDRESS
        provider.getOrFetch(userWalletId, network)
        provider.getOrFetch(otherWalletId, network)

        // Act
        provider.invalidate(null)
        provider.getOrFetch(userWalletId, network)
        provider.getOrFetch(otherWalletId, network)

        // Assert — 2 initial + 2 after a full invalidation
        coVerify(exactly = 4) { walletManagersFacade.getOrCreateWalletManager(any(), any(), any()) }
    }

    @Test
    fun `GIVEN two concurrent fetches for the same key WHEN one is in flight THEN manager is created once`() = runTest {
        // Arrange — io dispatcher we control so both callers reach the mutex before the cache is populated
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val concurrentProvider = DefaultYieldModuleAddressProvider(
            walletManagersFacade = walletManagersFacade,
            dispatchers = TestingCoroutineDispatcherProvider(
                main = testDispatcher,
                mainImmediate = testDispatcher,
                io = testDispatcher,
                default = testDispatcher,
                single = testDispatcher,
            ),
        )
        val proceed = CompletableDeferred<Unit>()
        coEvery { walletManager.getYieldModuleAddress() } coAnswers {
            proceed.await()
            ADDRESS
        }

        // Act — both pass the lock-free pre-check; one holds the lock and fetches, the other waits on it
        val first = launch { concurrentProvider.getOrFetch(userWalletId, network) }
        val second = launch { concurrentProvider.getOrFetch(userWalletId, network) }
        runCurrent()
        // At the barrier both callers have passed the lock-free pre-check (cache still empty): one holds the mutex and
        // awaits the gate, the other is blocked on the lock. Asserting neither completed proves the second did NOT
        // short-circuit on the outer pre-check, so it must hit the in-lock double-check once released.
        assertThat(first.isCompleted).isFalse()
        assertThat(second.isCompleted).isFalse()
        proceed.complete(Unit)
        advanceUntilIdle()
        first.join()
        second.join()

        // Assert — the second caller is served from cache via the in-lock double-check
        coVerify(exactly = 1) { walletManagersFacade.getOrCreateWalletManager(any(), any(), any()) }
        coVerify(exactly = 1) { walletManager.getYieldModuleAddress() }
    }

    private fun network(): Network {
        val derivationPath = Network.DerivationPath.None
        return Network(
            id = Network.ID(value = "ethereum", derivationPath = derivationPath),
            name = "Ethereum",
            currencySymbol = "ETH",
            derivationPath = derivationPath,
            isTestnet = false,
            standardType = Network.StandardType.Unspecified("UNSPECIFIED"),
            hasFiatFeeRate = true,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )
    }

    private companion object {
        const val ADDRESS = "0x1234567890abcdef1234567890abcdef12345678"
    }
}