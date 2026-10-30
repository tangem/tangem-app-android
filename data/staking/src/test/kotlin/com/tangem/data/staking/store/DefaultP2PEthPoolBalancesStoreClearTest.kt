package com.tangem.data.staking.store

import androidx.datastore.core.DataStore
import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.staking.store.P2PEthPoolBalancesStoreFixtures.ADDR_A
import com.tangem.data.staking.store.P2PEthPoolBalancesStoreFixtures.VAULT_1
import com.tangem.data.staking.store.P2PEthPoolBalancesStoreFixtures.VAULT_2
import com.tangem.data.staking.store.P2PEthPoolBalancesStoreFixtures.idA
import com.tangem.data.staking.store.P2PEthPoolBalancesStoreFixtures.idB
import com.tangem.data.staking.store.P2PEthPoolBalancesStoreFixtures.otherWalletId
import com.tangem.data.staking.store.P2PEthPoolBalancesStoreFixtures.p2pData
import com.tangem.data.staking.store.P2PEthPoolBalancesStoreFixtures.response
import com.tangem.data.staking.store.P2PEthPoolBalancesStoreFixtures.userWalletId
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolAccountResponse
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.mock.staking.MockStaking
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class DefaultP2PEthPoolBalancesStoreClearTest {

    private val runtimeStore = RuntimeSharedStore<WalletIdWithP2PStakingBalances>()
    private val persistenceStore = MockStateDataStore<WalletIdWithP2PEthPoolResponses>(default = emptyMap())

    private val failingRuntime: RuntimeSharedStore<WalletIdWithP2PStakingBalances> = mockk()
    private val failingPersistence: DataStore<WalletIdWithP2PEthPoolResponses> = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(failingRuntime, failingPersistence)
    }

    private fun createStore() = DefaultP2PEthPoolBalancesStore(
        runtimeStore = runtimeStore,
        persistenceStore = persistenceStore,
        scope = TestAppCoroutineScope(),
    )

    private suspend fun persisted() = persistenceStore.data.first()

    @Nested
    inner class Clear {

        private suspend fun createStoreWithTwoDelegators(): DefaultP2PEthPoolBalancesStore {
            persistenceStore.updateData {
                mapOf("011" to setOf(response(idA, VAULT_1), response(idA, VAULT_2), response(idB, VAULT_1)))
            }
            return createStore()
        }

        private val twoDelegatorsRuntime = mapOf(
            userWalletId to setOf(
                p2pData(idA, StatusSource.CACHE, response(idA, VAULT_1), response(idA, VAULT_2)),
                p2pData(idB, StatusSource.CACHE, response(idB, VAULT_1)),
            ),
        )

        @Test
        fun `GIVEN two delegators WHEN clear one id THEN removed from runtime and all its vaults from persistence`() =
            runTest {
                // Arrange
                val store = createStoreWithTwoDelegators()

                // Act
                store.clear(userWalletId, setOf(idA))

                // Assert
                val expectedRuntime = mapOf(userWalletId to setOf(p2pData(idB, StatusSource.CACHE, response(idB, VAULT_1))))
                assertThat(runtimeStore.getSyncOrNull()).isEqualTo(expectedRuntime)
                assertThat(persisted()).isEqualTo(mapOf("011" to setOf(response(idB, VAULT_1))))
            }

        @Test
        fun `GIVEN unknown wallet WHEN clear THEN empty entries created for that wallet`() = runTest {
            // Arrange
            val store = createStoreWithTwoDelegators()

            // Act
            store.clear(otherWalletId, setOf(idA))

            // Assert
            assertThat(runtimeStore.getSyncOrNull()?.get(otherWalletId)).isEqualTo(emptySet<StakingBalance>())
            assertThat(persisted()["022"]).isEqualTo(emptySet<P2PEthPoolAccountResponse>())
            assertThat(runtimeStore.getSyncOrNull()?.get(userWalletId)).isEqualTo(twoDelegatorsRuntime[userWalletId])
            assertThat(persisted()["011"])
                .isEqualTo(setOf(response(idA, VAULT_1), response(idA, VAULT_2), response(idB, VAULT_1)))
        }

        // Pins current behaviour: persistence clear matches only by address, runtime by full StakingID
        @Test
        fun `GIVEN id with foreign integration and same address WHEN clear THEN persistence cleared but runtime kept`() =
            runTest {
                // Arrange
                val store = createStoreWithTwoDelegators()

                // Act
                store.clear(userWalletId, setOf(StakingID(MockStaking.SOLANA_ID, ADDR_A)))

                // Assert
                assertThat(runtimeStore.getSyncOrNull()).isEqualTo(twoDelegatorsRuntime)
                assertThat(persisted()["011"]).isEqualTo(setOf(response(idB, VAULT_1)))
            }

        @Test
        fun `GIVEN empty id set WHEN clear THEN nothing removed`() = runTest {
            // Arrange
            val store = createStoreWithTwoDelegators()
            val persistedBefore = persisted()

            // Act
            store.clear(userWalletId, emptySet())

            // Assert
            assertThat(runtimeStore.getSyncOrNull()).isEqualTo(twoDelegatorsRuntime)
            assertThat(persisted()).isEqualTo(persistedBefore)
        }
    }

    @Nested
    inner class Remove {

        @Test
        fun `GIVEN two wallets WHEN remove one THEN both stores keep only the other`() = runTest {
            // Arrange
            persistenceStore.updateData { mapOf("011" to setOf(response(idA)), "022" to setOf(response(idB))) }
            val store = createStore()

            // Act
            store.remove(listOf(userWalletId))

            // Assert
            val expectedRuntime = mapOf(otherWalletId to setOf(p2pData(idB, StatusSource.CACHE, response(idB))))
            assertThat(runtimeStore.getSyncOrNull()).isEqualTo(expectedRuntime)
            assertThat(persisted()).isEqualTo(mapOf("022" to setOf(response(idB))))
        }

        @Test
        fun `GIVEN empty wallet list WHEN remove THEN stores unchanged`() = runTest {
            // Arrange
            persistenceStore.updateData { mapOf("011" to setOf(response(idA)), "022" to setOf(response(idB))) }
            val store = createStore()
            val runtimeBefore = runtimeStore.getSyncOrNull()
            val persistedBefore = persisted()

            // Act
            store.remove(emptyList())

            // Assert
            assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeBefore)
            assertThat(persisted()).isEqualTo(persistedBefore)
        }

        @Test
        fun `GIVEN nothing persisted WHEN remove empty wallet list THEN runtime stays uninitialized and persistence not written`() =
            runTest {
                // Arrange
                every { failingPersistence.data } returns emptyFlow()
                val store = DefaultP2PEthPoolBalancesStore(runtimeStore, failingPersistence, TestAppCoroutineScope())

                // Act
                store.remove(emptyList())

                // Assert
                assertThat(runtimeStore.getSyncOrNull()).isNull()
                coVerify(exactly = 0) { failingPersistence.updateData(any()) }
            }

        @Test
        fun `GIVEN runtime update throws WHEN remove THEN persistence still cleared and no exception`() = runTest {
            // Arrange
            coEvery { failingRuntime.store(any()) } just Runs
            coEvery { failingRuntime.update(any(), any()) } throws IllegalStateException("runtime")
            persistenceStore.updateData { mapOf("011" to setOf(response(idA)), "022" to setOf(response(idB))) }
            val store = DefaultP2PEthPoolBalancesStore(failingRuntime, persistenceStore, TestAppCoroutineScope())

            // Act
            val error = runCatching { store.remove(listOf(userWalletId)) }.exceptionOrNull()

            // Assert
            assertThat(error).isNull()
            coVerify(exactly = 1) { failingRuntime.update(any(), any()) }
            assertThat(persisted()).isEqualTo(mapOf("022" to setOf(response(idB))))
        }

        @Test
        fun `GIVEN persistence update throws WHEN remove THEN runtime still cleared and no exception`() = runTest {
            // Arrange
            every { failingPersistence.data } returns emptyFlow()
            coEvery { failingPersistence.updateData(any()) } throws IllegalStateException("persistence")
            val store = DefaultP2PEthPoolBalancesStore(runtimeStore, failingPersistence, TestAppCoroutineScope())
            runtimeStore.store(
                mapOf(
                    userWalletId to setOf(StakingBalance.Error(idA)),
                    otherWalletId to setOf(StakingBalance.Error(idB)),
                ),
            )

            // Act
            val error = runCatching { store.remove(listOf(userWalletId)) }.exceptionOrNull()

            // Assert
            assertThat(error).isNull()
            coVerify(exactly = 1) { failingPersistence.updateData(any()) }
            assertThat(runtimeStore.getSyncOrNull())
                .isEqualTo(mapOf(otherWalletId to setOf(StakingBalance.Error(idB))))
        }
    }
}