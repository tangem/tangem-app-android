package com.tangem.data.collectibles.account.flow

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.collectibles.account.store.CollectiblesAccountStatusStore
import com.tangem.data.collectibles.account.store.WalletIdWithCollectiblesStatusDTO
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusFetcher
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.account.CollectiblesVaultBalance
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Reading the vault's USDC waits on the blockchain SDK, so every path currently ends without an amount. What the
 * fetcher still has to get right is which of them leaves the cache alone and which records a missing derivation.
 */
internal class DefaultCollectiblesAccountStatusFetcherTest {

    private val ownerDeriver: CollectiblesOwnerDeriver = mockk()

    @Test
    fun `GIVEN a vault that cannot be read yet WHEN invoke THEN the cached amount is kept and marked stale`() =
        runTest {
            // Arrange
            coEvery { ownerDeriver.storedOwner(WALLET) } returns OWNER
            val store = createStore(testScope = this, recorded = ONBOARDED.copy(vaultBalance = CACHED))

            // Act
            val actual = createFetcher(store).invoke(PARAMS)

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            assertThat(store.getSyncOrNull(WALLET)).isEqualTo(
                CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ONLY_CACHE, vaultBalance = CACHED),
            )
        }

    /** A wallet restored on a new phone: the vault exists, its owner key has not been derived here yet. */
    @Test
    fun `GIVEN a record and no derived owner key WHEN invoke THEN the derivation is reported as missing`() = runTest {
        // Arrange
        coEvery { ownerDeriver.storedOwner(WALLET) } returns null
        val store = createStore(testScope = this, recorded = ONBOARDED.copy(vaultBalance = CACHED))

        // Act
        createFetcher(store).invoke(PARAMS)

        // Assert — the cached amount goes with it: it was read through a key this device no longer has
        assertThat(store.getSyncOrNull(WALLET)).isEqualTo(CollectiblesAccountStatusValue.MissedDerivation)
    }

    @Test
    fun `GIVEN nothing recorded WHEN invoke THEN nothing is stored and the owner key is never asked for`() = runTest {
        // Arrange
        val store = createStore(testScope = this, recorded = null)

        // Act
        createFetcher(store).invoke(PARAMS)

        // Assert
        assertThat(store.getSyncOrNull(WALLET)).isNull()
    }

    @Test
    fun `GIVEN a locked wallet WHEN invoke THEN the cached amount is kept and marked stale`() = runTest {
        // Arrange
        val store = createStore(testScope = this, recorded = ONBOARDED.copy(vaultBalance = CACHED))

        // Act
        createFetcher(store, userWallet = LOCKED_WALLET).invoke(PARAMS)

        // Assert
        assertThat(store.getSyncOrNull(WALLET)).isEqualTo(
            CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ONLY_CACHE, vaultBalance = CACHED),
        )
    }

    @Test
    fun `GIVEN the owner key read throws WHEN invoke THEN the failure is reported and the cache is kept`() = runTest {
        // Arrange
        coEvery { ownerDeriver.storedOwner(WALLET) } throws IllegalStateException("keystore is locked")
        val store = createStore(testScope = this, recorded = ONBOARDED.copy(vaultBalance = CACHED))

        // Act
        val actual = createFetcher(store).invoke(PARAMS)

        // Assert
        assertThat(actual.isLeft()).isTrue()
        assertThat(store.getSyncOrNull(WALLET)).isEqualTo(
            CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ONLY_CACHE, vaultBalance = CACHED),
        )
    }

    private suspend fun createStore(testScope: TestScope, recorded: CollectiblesAccountStatusValue?) =
        CollectiblesAccountStatusStore(
            runtimeStore = RuntimeSharedStore(),
            persistenceDataStore = MockStateDataStore<WalletIdWithCollectiblesStatusDTO>(default = emptyMap()),
            scope = TestAppCoroutineScope(testScope),
        ).apply {
            recorded?.let { store(userWalletId = WALLET, value = it) }
        }

    private fun createFetcher(store: CollectiblesAccountStatusStore, userWallet: UserWallet = USER_WALLET) =
        DefaultCollectiblesAccountStatusFetcher(
            collectiblesAccountStatusStore = store,
            userWalletsListRepository = mockk {
                every { userWallets } returns MutableStateFlow<List<UserWallet>?>(listOf(userWallet))
            },
            collectiblesOwnerDeriver = ownerDeriver,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )

    private companion object {
        const val OWNER = "8kXHqmvJ3sYVsBLnr5aXqjy6r2ZxHhCTvJH1DPDcnUXG"

        val WALLET = UserWalletId("011")
        val PARAMS = CollectiblesAccountStatusFetcher.Params(userWalletId = WALLET)

        val USER_WALLET: UserWallet = mockk<UserWallet.Hot>(relaxed = true) {
            every { walletId } returns WALLET
            every { isLocked } returns false
        }

        val LOCKED_WALLET: UserWallet = mockk<UserWallet.Hot>(relaxed = true) {
            every { walletId } returns WALLET
            every { isLocked } returns true
        }

        val ONBOARDED = CollectiblesAccountStatusValue.Onboarded(source = StatusSource.CACHE)
        val CACHED = CollectiblesVaultBalance(usdcAmount = BigDecimal("9"))
    }
}