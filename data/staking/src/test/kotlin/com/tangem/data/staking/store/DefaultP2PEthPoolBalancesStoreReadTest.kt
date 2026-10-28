package com.tangem.data.staking.store

import androidx.datastore.core.DataStore
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.data.staking.MockP2PEthPoolAccountResponseFactory
import com.tangem.core.local.datastore.RuntimeSharedStore
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
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.core.getEmittedValues
import com.tangem.test.mock.staking.MockStaking
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class DefaultP2PEthPoolBalancesStoreReadTest {

    private val runtimeStore = RuntimeSharedStore<WalletIdWithP2PStakingBalances>()
    private val persistenceStore = MockStateDataStore<WalletIdWithP2PEthPoolResponses>(default = emptyMap())

    private val emptyPersistence: DataStore<WalletIdWithP2PEthPoolResponses> = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(emptyPersistence)
    }

    private fun createStore() = DefaultP2PEthPoolBalancesStore(
        runtimeStore = runtimeStore,
        persistenceStore = persistenceStore,
        scope = TestAppCoroutineScope(),
    )

    private fun createStoreWithNothingPersisted(): DefaultP2PEthPoolBalancesStore {
        every { emptyPersistence.data } returns emptyFlow()

        return DefaultP2PEthPoolBalancesStore(
            runtimeStore = runtimeStore,
            persistenceStore = emptyPersistence,
            scope = TestAppCoroutineScope(),
        )
    }

    @Nested
    inner class Initialization {

        @Test
        fun `GIVEN persistence emits nothing WHEN store created THEN runtime stays uninitialized`() = runTest {
            // Act
            createStoreWithNothingPersisted()

            // Assert
            assertThat(runtimeStore.getSyncOrNull()).isNull()
        }

        @Test
        fun `GIVEN persistence holds empty map WHEN store created THEN runtime holds empty map`() = runTest {
            // Act
            createStore()

            // Assert
            assertThat(runtimeStore.getSyncOrNull()).isEqualTo(emptyMap<UserWalletId, Set<StakingBalance>>())
        }

        @Test
        fun `GIVEN two vault responses of one delegator WHEN store created THEN one CACHE balance with both accounts`() =
            runTest {
                // Arrange
                persistenceStore.updateData { mapOf("011" to setOf(response(idA, VAULT_1), response(idA, VAULT_2))) }

                // Act
                createStore()

                // Assert
                val expected = mapOf(
                    userWalletId to setOf(
                        p2pData(idA, StatusSource.CACHE, response(idA, VAULT_1), response(idA, VAULT_2)),
                    ),
                )
                assertThat(runtimeStore.getSyncOrNull()).isEqualTo(expected)
            }

        @Test
        fun `GIVEN responses of two delegators WHEN store created THEN one balance per delegator`() = runTest {
            // Arrange
            persistenceStore.updateData { mapOf("011" to setOf(response(idA), response(idB))) }

            // Act
            createStore()

            // Assert
            val expected = mapOf(
                userWalletId to setOf(
                    p2pData(idA, StatusSource.CACHE, response(idA)),
                    p2pData(idB, StatusSource.CACHE, response(idB)),
                ),
            )
            assertThat(runtimeStore.getSyncOrNull()).isEqualTo(expected)
        }

        @Test
        fun `GIVEN zero balance response WHEN store created THEN Empty CACHE balance`() = runTest {
            // Arrange
            persistenceStore.updateData {
                mapOf("011" to setOf(MockP2PEthPoolAccountResponseFactory.createWithEmptyBalance(idA, VAULT_1)))
            }

            // Act
            createStore()

            // Assert
            val expected = mapOf(userWalletId to setOf(StakingBalance.Empty(idA, StatusSource.CACHE)))
            assertThat(runtimeStore.getSyncOrNull()).isEqualTo(expected)
        }

        @Test
        fun `GIVEN two wallets in persistence WHEN store created THEN keys mapped to UserWalletId`() = runTest {
            // Arrange
            persistenceStore.updateData {
                mapOf("011" to setOf(response(idA)), "022" to setOf(response(idB)))
            }

            // Act
            createStore()

            // Assert
            val expected = mapOf(
                userWalletId to setOf(p2pData(idA, StatusSource.CACHE, response(idA))),
                otherWalletId to setOf(p2pData(idB, StatusSource.CACHE, response(idB))),
            )
            assertThat(runtimeStore.getSyncOrNull()).isEqualTo(expected)
        }
    }

    @Nested
    inner class Get {

        @Test
        fun `GIVEN runtime never initialized WHEN get THEN nothing emitted`() = runTest {
            // Arrange
            val store = createStoreWithNothingPersisted()

            // Act
            val emitted = getEmittedValues(store.get(userWalletId))

            // Assert
            assertThat(emitted).isEmpty()
        }

        @Test
        fun `GIVEN balances of another wallet only WHEN get THEN emits empty set`() = runTest {
            // Arrange
            val store = createStore()
            runtimeStore.store(mapOf(otherWalletId to setOf(p2pData(idB, StatusSource.ACTUAL, response(idB)))))

            // Act
            val emitted = getEmittedValues(store.get(userWalletId))

            // Assert
            assertThat(emitted).isEqualTo(listOf(emptySet<StakingBalance>()))
        }

        @Test
        fun `GIVEN wallet balances WHEN get THEN emits them`() = runTest {
            // Arrange
            val store = createStore()
            val balance = p2pData(idA, StatusSource.ACTUAL, response(idA))
            runtimeStore.store(mapOf(userWalletId to setOf(balance)))

            // Act
            val emitted = getEmittedValues(store.get(userWalletId))

            // Assert
            assertThat(emitted).isEqualTo(listOf(setOf(balance)))
        }
    }

    @Nested
    inner class GetSyncOrNull {

        private val dataBalance = p2pData(idA, StatusSource.ACTUAL, response(idA))

        @Test
        fun `GIVEN balance with staking id WHEN getSyncOrNull THEN returns it`() = runTest {
            // Arrange
            val store = createStore()
            runtimeStore.store(mapOf(userWalletId to setOf(dataBalance, StakingBalance.Error(idB))))

            // Act
            val actual = store.getSyncOrNull(userWalletId, idA)

            // Assert
            assertThat(actual).isEqualTo(dataBalance)
        }

        @Test
        fun `GIVEN no balance with staking id WHEN getSyncOrNull THEN returns null`() = runTest {
            // Arrange
            val store = createStore()
            runtimeStore.store(mapOf(userWalletId to setOf(dataBalance, StakingBalance.Error(idB))))

            // Act
            val actual = store.getSyncOrNull(userWalletId, StakingID(MockStaking.P2P_ID, "0xunknown"))

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN unknown wallet WHEN getSyncOrNull THEN returns null`() = runTest {
            // Arrange
            val store = createStore()
            runtimeStore.store(mapOf(userWalletId to setOf(dataBalance, StakingBalance.Error(idB))))

            // Act
            val actual = store.getSyncOrNull(otherWalletId, idA)

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN runtime never initialized WHEN getSyncOrNull THEN returns null`() = runTest {
            // Arrange
            val store = createStoreWithNothingPersisted()

            // Act
            val actual = store.getSyncOrNull(userWalletId, idA)

            // Assert
            assertThat(actual).isNull()
        }
    }

    @Nested
    inner class GetAllSyncOrNull {

        private val dataBalance = p2pData(idA, StatusSource.ACTUAL, response(idA))

        @Test
        fun `GIVEN wallet balances WHEN getAllSyncOrNull THEN returns whole set`() = runTest {
            // Arrange
            val store = createStore()
            runtimeStore.store(mapOf(userWalletId to setOf(dataBalance, StakingBalance.Error(idB))))

            // Act
            val actual = store.getAllSyncOrNull(userWalletId)

            // Assert
            assertThat(actual).isEqualTo(setOf(dataBalance, StakingBalance.Error(idB)))
        }

        @Test
        fun `GIVEN unknown wallet WHEN getAllSyncOrNull THEN returns null`() = runTest {
            // Arrange
            val store = createStore()
            runtimeStore.store(mapOf(userWalletId to setOf(dataBalance, StakingBalance.Error(idB))))

            // Act
            val actual = store.getAllSyncOrNull(otherWalletId)

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN runtime never initialized WHEN getAllSyncOrNull THEN returns null`() = runTest {
            // Arrange
            val store = createStoreWithNothingPersisted()

            // Act
            val actual = store.getAllSyncOrNull(userWalletId)

            // Assert
            assertThat(actual).isNull()
        }
    }
}