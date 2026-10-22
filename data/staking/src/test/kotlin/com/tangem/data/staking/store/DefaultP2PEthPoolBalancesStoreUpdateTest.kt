package com.tangem.data.staking.store

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
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolAccountResponse
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.datastore.MockStateDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class DefaultP2PEthPoolBalancesStoreUpdateTest {

    private val runtimeStore = RuntimeSharedStore<WalletIdWithP2PStakingBalances>()
    private val persistenceStore = MockStateDataStore<WalletIdWithP2PEthPoolResponses>(default = emptyMap())

    private fun createStore() = DefaultP2PEthPoolBalancesStore(
        runtimeStore = runtimeStore,
        persistenceStore = persistenceStore,
        scope = TestAppCoroutineScope(),
    )

    private suspend fun persisted() = persistenceStore.data.first()

    private val noPersistedData = emptyMap<String, Set<P2PEthPoolAccountResponse>>()

    @Nested
    inner class Refresh {

        @Test
        fun `GIVEN empty runtime WHEN refresh single id THEN wallet gets empty set and persistence untouched`() =
            runTest {
                // Arrange
                val store = createStore()

                // Act
                store.refresh(userWalletId, idA)

                // Assert
                assertThat(runtimeStore.getSyncOrNull()).isEqualTo(mapOf(userWalletId to emptySet<StakingBalance>()))
                assertThat(persisted()).isEqualTo(noPersistedData)
            }

        @Test
        fun `GIVEN actual balance WHEN refresh single id THEN source becomes CACHE`() = runTest {
            // Arrange
            val store = createStore()
            runtimeStore.store(mapOf(userWalletId to setOf(p2pData(idA, StatusSource.ACTUAL, response(idA)))))

            // Act
            store.refresh(userWalletId, idA)

            // Assert
            val expected = mapOf(userWalletId to setOf(p2pData(idA, StatusSource.CACHE, response(idA))))
            assertThat(runtimeStore.getSyncOrNull()).isEqualTo(expected)
        }

        @Test
        fun `GIVEN actual balances WHEN refresh multiple ids THEN only requested ids become CACHE`() = runTest {
            // Arrange
            val store = createStore()
            runtimeStore.store(
                mapOf(
                    userWalletId to setOf(
                        p2pData(idA, StatusSource.ACTUAL, response(idA)),
                        p2pData(idB, StatusSource.ACTUAL, response(idB)),
                    ),
                ),
            )

            // Act
            store.refresh(userWalletId, setOf(idA))

            // Assert
            val expected = mapOf(
                userWalletId to setOf(
                    p2pData(idA, StatusSource.CACHE, response(idA)),
                    p2pData(idB, StatusSource.ACTUAL, response(idB)),
                ),
            )
            assertThat(runtimeStore.getSyncOrNull()).isEqualTo(expected)
        }

        @Test
        fun `GIVEN Error balance WHEN refresh THEN Error kept as is`() = runTest {
            // Arrange
            val store = createStore()
            runtimeStore.store(mapOf(userWalletId to setOf(StakingBalance.Error(idA))))

            // Act
            store.refresh(userWalletId, idA)

            // Assert
            assertThat(runtimeStore.getSyncOrNull()).isEqualTo(mapOf(userWalletId to setOf(StakingBalance.Error(idA))))
        }
    }

    @Nested
    inner class StoreActual {

        @Test
        fun `GIVEN empty stores WHEN storeActual THEN runtime grouped by delegator and persistence keeps raw responses`() =
            runTest {
                // Arrange
                val store = createStore()
                val values = setOf(response(idA, VAULT_1), response(idA, VAULT_2), response(idB, VAULT_1))

                // Act
                store.storeActual(userWalletId, values)

                // Assert
                val expectedRuntime = mapOf(
                    userWalletId to setOf(
                        p2pData(idA, StatusSource.ACTUAL, response(idA, VAULT_1), response(idA, VAULT_2)),
                        p2pData(idB, StatusSource.ACTUAL, response(idB, VAULT_1)),
                    ),
                )
                assertThat(runtimeStore.getSyncOrNull()).isEqualTo(expectedRuntime)
                assertThat(persisted()).isEqualTo(mapOf("011" to values))
            }

        @Test
        fun `GIVEN zero balance response WHEN storeActual THEN Empty ACTUAL in runtime`() = runTest {
            // Arrange
            val store = createStore()
            val values = setOf(MockP2PEthPoolAccountResponseFactory.createWithEmptyBalance(idA, VAULT_1))

            // Act
            store.storeActual(userWalletId, values)

            // Assert
            val expected = mapOf(userWalletId to setOf(StakingBalance.Empty(idA, StatusSource.ACTUAL)))
            assertThat(runtimeStore.getSyncOrNull()).isEqualTo(expected)
            assertThat(persisted()).isEqualTo(mapOf("011" to values))
        }

        @Test
        fun `GIVEN cached balance of same delegator WHEN storeActual THEN runtime replaced by staking id and others kept`() =
            runTest {
                // Arrange
                val store = createStore()
                runtimeStore.store(
                    mapOf(
                        userWalletId to setOf(
                            p2pData(idA, StatusSource.CACHE, response(idA, staked = BigDecimal("1.5"))),
                            StakingBalance.Error(idB),
                        ),
                    ),
                )

                // Act
                store.storeActual(userWalletId, setOf(response(idA, staked = BigDecimal("2"))))

                // Assert
                val expected = mapOf(
                    userWalletId to setOf(
                        p2pData(idA, StatusSource.ACTUAL, response(idA, staked = BigDecimal("2"))),
                        StakingBalance.Error(idB),
                    ),
                )
                assertThat(runtimeStore.getSyncOrNull()).isEqualTo(expected)
            }

        @Test
        fun `GIVEN persisted response for same delegator and vault WHEN storeActual THEN persisted response replaced`() =
            runTest {
                // Arrange
                persistenceStore.updateData { mapOf("011" to setOf(response(idA, VAULT_1, BigDecimal("1.5")))) }
                val store = createStore()

                // Act
                store.storeActual(userWalletId, setOf(response(idA, VAULT_1, BigDecimal("2"))))

                // Assert
                assertThat(persisted()).isEqualTo(mapOf("011" to setOf(response(idA, VAULT_1, BigDecimal("2")))))
            }

        @Test
        fun `GIVEN delegator lost a vault WHEN storeActual THEN runtime drops it`() =
            runTest {
                // Arrange
                persistenceStore.updateData { mapOf("011" to setOf(response(idA, VAULT_1), response(idA, VAULT_2))) }
                val store = createStore()

                // Act
                store.storeActual(userWalletId, setOf(response(idA, VAULT_1, BigDecimal("2"))))

                // Assert
                val expectedRuntime = mapOf(
                    userWalletId to setOf(
                        p2pData(idA, StatusSource.ACTUAL, response(idA, VAULT_1, BigDecimal("2"))),
                    ),
                )
                assertThat(runtimeStore.getSyncOrNull()).isEqualTo(expectedRuntime)
            }

        @Test
        fun `GIVEN persisted response of same delegator in other vault WHEN storeActual THEN both responses persisted`() =
            runTest {
                // Arrange
                persistenceStore.updateData { mapOf("011" to setOf(response(idA, VAULT_2))) }
                val store = createStore()

                // Act
                store.storeActual(userWalletId, setOf(response(idA, VAULT_1)))

                // Assert
                assertThat(persisted()).isEqualTo(mapOf("011" to setOf(response(idA, VAULT_2), response(idA, VAULT_1))))
            }

        @Test
        fun `GIVEN persisted response of other delegator in same vault WHEN storeActual THEN both responses persisted`() =
            runTest {
                // Arrange
                persistenceStore.updateData { mapOf("011" to setOf(response(idB, VAULT_1))) }
                val store = createStore()

                // Act
                store.storeActual(userWalletId, setOf(response(idA, VAULT_1)))

                // Assert
                assertThat(persisted()).isEqualTo(mapOf("011" to setOf(response(idB, VAULT_1), response(idA, VAULT_1))))
            }

        @Test
        fun `GIVEN another wallet stored WHEN storeActual THEN other wallet untouched`() = runTest {
            // Arrange
            persistenceStore.updateData { mapOf("022" to setOf(response(idB))) }
            val store = createStore()
            runtimeStore.store(mapOf(otherWalletId to setOf(StakingBalance.Error(idB))))

            // Act
            store.storeActual(userWalletId, setOf(response(idA)))

            // Assert
            assertThat(runtimeStore.getSyncOrNull()?.get(otherWalletId)).isEqualTo(setOf(StakingBalance.Error(idB)))
            assertThat(persisted()["022"]).isEqualTo(setOf(response(idB)))
        }
    }

    @Nested
    inner class StoreEmpty {

        @Test
        fun `GIVEN missing staking id WHEN storeEmpty THEN Empty ACTUAL added`() = runTest {
            // Arrange
            val store = createStore()

            // Act
            store.storeEmpty(userWalletId, setOf(idA))

            // Assert
            val expected = mapOf(userWalletId to setOf(StakingBalance.Empty(idA, StatusSource.ACTUAL)))
            assertThat(runtimeStore.getSyncOrNull()).isEqualTo(expected)
            assertThat(persisted()).isEqualTo(noPersistedData)
        }

        @Test
        fun `GIVEN persisted responses WHEN storeEmpty THEN persistence untouched`() = runTest {
            // Arrange
            persistenceStore.updateData { mapOf("011" to setOf(response(idA))) }
            val store = createStore()

            // Act
            store.storeEmpty(userWalletId, setOf(idA))

            // Assert
            assertThat(persisted()).isEqualTo(mapOf("011" to setOf(response(idA))))
        }
    }

    @Nested
    inner class StoreError {

        @Test
        fun `GIVEN missing staking id WHEN storeError THEN Error added`() = runTest {
            // Arrange
            val store = createStore()

            // Act
            store.storeError(userWalletId, setOf(idA))

            // Assert
            assertThat(runtimeStore.getSyncOrNull()).isEqualTo(mapOf(userWalletId to setOf(StakingBalance.Error(idA))))
            assertThat(persisted()).isEqualTo(noPersistedData)
        }

        @Test
        fun `GIVEN one present and one missing id WHEN storeError THEN both updated`() = runTest {
            // Arrange
            val store = createStore()
            runtimeStore.store(mapOf(userWalletId to setOf(p2pData(idA, StatusSource.CACHE, response(idA)))))

            // Act
            store.storeError(userWalletId, setOf(idA, idB))

            // Assert
            val expected = mapOf(
                userWalletId to setOf(
                    p2pData(idA, StatusSource.ONLY_CACHE, response(idA)),
                    StakingBalance.Error(idB),
                ),
            )
            assertThat(runtimeStore.getSyncOrNull()).isEqualTo(expected)
        }
    }
}