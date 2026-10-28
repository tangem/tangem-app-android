package com.tangem.data.staking.store

import com.google.common.truth.Truth
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.common.test.data.staking.MockYieldBalanceWrapperDTOFactory
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.data.staking.toDomain
import com.tangem.grow.datasource.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWalletId
import androidx.datastore.core.DataStore
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
[REDACTED_AUTHOR]
 */
internal class StakingBalancesStoreUpdateMethodsTest {

    private val runtimeStore = RuntimeSharedStore<WalletIdWithStakingBalances>()
    private val persistenceStore = MockStateDataStore<WalletIdWithWrappers>(default = emptyMap())

    private val failingRuntime: RuntimeSharedStore<WalletIdWithStakingBalances> = mockk()
    private val failingPersistence: DataStore<WalletIdWithWrappers> = mockk()

    private val store = DefaultStakeKitBalancesStore(
        runtimeStore = runtimeStore,
        persistenceStore = persistenceStore,
        scope = TestAppCoroutineScope(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(failingRuntime, failingPersistence)
    }

    @Test
    fun `GIVEN empty runtime WHEN refresh single id THEN wallet gets empty set and persistence untouched`() = runTest {
        // Act
        store.refresh(userWalletId = userWalletId, stakingId = stakingId)

        // Assert
        val runtimeExpected = mapOf(userWalletId to emptySet<StakingBalance>())

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<YieldBalanceWrapperDTO>>())
    }

    @Test
    fun `GIVEN actual balance WHEN refresh single id THEN source becomes CACHE`() = runTest {
        // Arrange
        val balance = MockYieldBalanceWrapperDTOFactory.createWithBalance().toDomain(source = StatusSource.ACTUAL)

        runtimeStore.store(
            value = mapOf(userWalletId to setOf(balance)),
        )

        // Act
        store.refresh(userWalletId = userWalletId, stakingId = stakingId)

        // Assert
        val runtimeExpected = mapOf(
            userWalletId to setOf(
                balance.copySealed(source = StatusSource.CACHE),
            ),
        )

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<YieldBalanceWrapperDTO>>())
    }

    @Test
    fun `GIVEN empty runtime WHEN refresh multiple ids THEN wallet gets empty set and persistence untouched`() = runTest {
        // Act
        store.refresh(userWalletId = userWalletId, stakingIds = stakingIds)

        // Assert
        val runtimeExpected = mapOf(userWalletId to emptySet<StakingBalance>())

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<YieldBalanceWrapperDTO>>())
    }

    @Test
    fun `GIVEN actual balances WHEN refresh multiple ids THEN all sources become CACHE`() = runTest {
        // Arrange
        val firstBalance = MockYieldBalanceWrapperDTOFactory.createWithBalance(stakingId = stakingIds.first())
            .toDomain(source = StatusSource.ACTUAL)

        val secondBalance = MockYieldBalanceWrapperDTOFactory.createWithBalance(stakingId = stakingIds.last())
            .toDomain(source = StatusSource.ACTUAL)

        runtimeStore.store(
            value = mapOf(userWalletId to setOf(firstBalance, secondBalance)),
        )

        // Act
        store.refresh(userWalletId = userWalletId, stakingIds = stakingIds)

        // Assert
        val runtimeExpected = mapOf(
            userWalletId to setOf(
                firstBalance.copySealed(source = StatusSource.CACHE),
                secondBalance.copySealed(source = StatusSource.CACHE),
            ),
        )

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<YieldBalanceWrapperDTO>>())
    }

    @Test
    fun `GIVEN cached balance with same id WHEN storeActual THEN runtime and persistence replaced`() = runTest {
        // Arrange
        val prevWrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance()

        runtimeStore.store(
            value = mapOf(userWalletId to setOf(prevWrapper.toDomain(source = StatusSource.CACHE))),
        )

        persistenceStore.updateData {
            it.toMutableMap().apply {
                put(userWalletId.stringValue, setOf(prevWrapper))
            }
        }

        val newWrapper = MockYieldBalanceWrapperDTOFactory.createWithEmptyBalance()

        // Act
        store.storeActual(userWalletId, setOf(newWrapper))

        // Assert
        val runtimeExpected = mapOf(
            userWalletId to setOf(newWrapper.toDomain(source = StatusSource.ACTUAL)),
        )

        val persistenceExpected = mapOf(
            userWalletId.stringValue to setOf(newWrapper),
        )

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(persistenceExpected)
    }

    @Test
    fun `GIVEN balance with other staking id stored WHEN storeActual THEN runtime and persistence keep both`() =
        runTest {
            // Arrange
            val solWrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance(solanaId)
            runtimeStore.store(mapOf(userWalletId to setOf(solWrapper.toDomain(source = StatusSource.CACHE))))
            persistenceStore.updateData { mapOf(userWalletId.stringValue to setOf(solWrapper)) }
            val tonWrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance(stakingId)

            // Act
            store.storeActual(userWalletId, setOf(tonWrapper))

            // Assert
            val runtimeExpected = mapOf(
                userWalletId to setOf(
                    solWrapper.toDomain(source = StatusSource.CACHE),
                    tonWrapper.toDomain(source = StatusSource.ACTUAL),
                ),
            )
            val persistenceExpected = mapOf(userWalletId.stringValue to setOf(solWrapper, tonWrapper))

            Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
            Truth.assertThat(persistenceStore.data.first()).isEqualTo(persistenceExpected)
        }

    @Test
    fun `GIVEN empty runtime WHEN storeError THEN Error balance added`() = runTest {
        // Act
        store.storeError(userWalletId = userWalletId, stakingIds = setOf(stakingId))

        // Assert
        val runtimeExpected = mapOf(
            userWalletId to setOf(StakingBalance.Error(stakingId)),
        )

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<YieldBalanceWrapperDTO>>())
    }

    @Test
    fun `GIVEN cached balance WHEN storeError THEN source becomes ONLY_CACHE`() = runTest {
        // Arrange
        val wrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance(stakingId)

        runtimeStore.store(
            value = mapOf(
                userWalletId to setOf(wrapper.toDomain(source = StatusSource.CACHE)),
            ),
        )

        // Act
        store.storeError(userWalletId = userWalletId, stakingIds = setOf(stakingId))

        // Assert
        // storeError keeps the existing (CACHE) balance and downgrades its source to ONLY_CACHE.
        // toDomain(ONLY_CACHE) can't express this: the converter maps any non-CACHE source to ACTUAL.
        val runtimeExpected = mapOf(
            userWalletId to setOf(wrapper.toDomain(source = StatusSource.CACHE).copySealed(source = StatusSource.ONLY_CACHE)),
        )

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<YieldBalanceWrapperDTO>>())
    }

    @Test
    fun `GIVEN two balances WHEN clear one id THEN removed from runtime and persistence`() = runTest {
        // Arrange
        val tonWrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance(stakingId)
        val solWrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance(solanaId)
        runtimeStore.store(mapOf(userWalletId to setOf(tonWrapper.toDomain(), solWrapper.toDomain())))
        persistenceStore.updateData { mapOf("011" to setOf(tonWrapper, solWrapper)) }

        // Act
        store.clear(userWalletId, setOf(stakingId))

        // Assert
        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(mapOf(userWalletId to setOf(solWrapper.toDomain())))
        Truth.assertThat(persistenceStore.data.first()).isEqualTo(mapOf("011" to setOf(solWrapper)))
    }

    @Test
    fun `GIVEN persisted wrapper without integration id WHEN clear THEN that wrapper kept`() = runTest {
        // Arrange
        val withoutIntegration = MockYieldBalanceWrapperDTOFactory.createWithBalance(stakingId).copy(integrationId = null)
        persistenceStore.updateData { mapOf("011" to setOf(withoutIntegration)) }

        // Act
        store.clear(userWalletId, setOf(stakingId))

        // Assert
        Truth.assertThat(persistenceStore.data.first()).isEqualTo(mapOf("011" to setOf(withoutIntegration)))
    }

    @Test
    fun `GIVEN two wallets WHEN remove one THEN both stores keep only the other`() = runTest {
        // Arrange
        val tonWrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance(stakingId)
        val solWrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance(solanaId)
        runtimeStore.store(
            mapOf(userWalletId to setOf(tonWrapper.toDomain()), otherWalletId to setOf(solWrapper.toDomain())),
        )
        persistenceStore.updateData { mapOf("011" to setOf(tonWrapper), "022" to setOf(solWrapper)) }

        // Act
        store.remove(listOf(userWalletId))

        // Assert
        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(mapOf(otherWalletId to setOf(solWrapper.toDomain())))
        Truth.assertThat(persistenceStore.data.first()).isEqualTo(mapOf("022" to setOf(solWrapper)))
    }

    @Test
    fun `GIVEN empty wallet list WHEN remove THEN stores unchanged`() = runTest {
        // Arrange
        val tonWrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance(stakingId)
        val solWrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance(solanaId)
        runtimeStore.store(
            mapOf(userWalletId to setOf(tonWrapper.toDomain()), otherWalletId to setOf(solWrapper.toDomain())),
        )
        persistenceStore.updateData { mapOf("011" to setOf(tonWrapper), "022" to setOf(solWrapper)) }
        val runtimeBefore = runtimeStore.getSyncOrNull()
        val persistenceBefore = persistenceStore.data.first()

        // Act
        store.remove(emptyList())

        // Assert
        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeBefore)
        Truth.assertThat(persistenceStore.data.first()).isEqualTo(persistenceBefore)
    }

    @Test
    fun `GIVEN nothing persisted WHEN remove empty wallet list THEN runtime stays uninitialized and persistence not written`() =
        runTest {
            // Arrange
            every { failingPersistence.data } returns emptyFlow()
            val localRuntime = RuntimeSharedStore<WalletIdWithStakingBalances>()
            val localStore = DefaultStakeKitBalancesStore(localRuntime, failingPersistence, TestAppCoroutineScope())

            // Act
            localStore.remove(emptyList())

            // Assert
            Truth.assertThat(localRuntime.getSyncOrNull()).isNull()
            coVerify(exactly = 0) { failingPersistence.updateData(any()) }
        }

    @Test
    fun `GIVEN runtime update throws WHEN remove THEN persistence still cleared and no exception`() = runTest {
        // Arrange
        coEvery { failingRuntime.store(any()) } just Runs
        coEvery { failingRuntime.update(any(), any()) } throws IllegalStateException("runtime")
        val tonWrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance(stakingId)
        val solWrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance(solanaId)
        persistenceStore.updateData { mapOf("011" to setOf(tonWrapper), "022" to setOf(solWrapper)) }
        val failingStore = DefaultStakeKitBalancesStore(failingRuntime, persistenceStore, TestAppCoroutineScope())

        // Act
        val error = runCatching { failingStore.remove(listOf(userWalletId)) }.exceptionOrNull()

        // Assert
        Truth.assertThat(error).isNull()
        coVerify(exactly = 1) { failingRuntime.update(any(), any()) }
        Truth.assertThat(persistenceStore.data.first()).isEqualTo(mapOf("022" to setOf(solWrapper)))
    }

    @Test
    fun `GIVEN persistence update throws WHEN remove THEN runtime still cleared and no exception`() = runTest {
        // Arrange
        every { failingPersistence.data } returns emptyFlow()
        coEvery { failingPersistence.updateData(any()) } throws IllegalStateException("persistence")
        val failingStore = DefaultStakeKitBalancesStore(runtimeStore, failingPersistence, TestAppCoroutineScope())
        runtimeStore.store(
            mapOf(
                userWalletId to setOf(StakingBalance.Error(stakingId)),
                otherWalletId to setOf(StakingBalance.Error(solanaId)),
            ),
        )

        // Act
        val error = runCatching { failingStore.remove(listOf(userWalletId)) }.exceptionOrNull()

        // Assert
        Truth.assertThat(error).isNull()
        coVerify(exactly = 1) { failingPersistence.updateData(any()) }
        Truth.assertThat(runtimeStore.getSyncOrNull())
            .isEqualTo(mapOf(otherWalletId to setOf(StakingBalance.Error(solanaId))))
    }

    // Pins current behaviour: wrappers without integrationId are never matched, so they pile up in persistence and are skipped in runtime
    @Test
    fun `GIVEN persisted wrapper without integration id WHEN storeActual another one THEN both kept in persistence and runtime gets nothing`() =
        runTest {
            // Arrange
            val old = MockYieldBalanceWrapperDTOFactory.createWithBalance(stakingId).copy(integrationId = null)
            val new = MockYieldBalanceWrapperDTOFactory.createWithEmptyBalance(stakingId).copy(integrationId = null)
            persistenceStore.updateData { mapOf("011" to setOf(old)) }
            val localStore = DefaultStakeKitBalancesStore(runtimeStore, persistenceStore, TestAppCoroutineScope())

            // Act
            localStore.storeActual(userWalletId, setOf(new))

            // Assert
            Truth.assertThat(persistenceStore.data.first()).isEqualTo(mapOf("011" to setOf(old, new)))
            Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(mapOf(userWalletId to emptySet<StakingBalance>()))
        }

    // Pins current behaviour: a blank address has no staking id in persistence, so persistence appends while runtime replaces
    @Test
    fun `GIVEN persisted wrapper with blank address WHEN storeActual same id THEN persistence appends and runtime replaces`() =
        runTest {
            // Arrange
            val blankId = StakingID(stakingId.integrationId, "")
            val old = MockYieldBalanceWrapperDTOFactory.createWithBalance(blankId)
            val new = MockYieldBalanceWrapperDTOFactory.createWithEmptyBalance(blankId)
            persistenceStore.updateData { mapOf("011" to setOf(old)) }
            val localStore = DefaultStakeKitBalancesStore(runtimeStore, persistenceStore, TestAppCoroutineScope())

            // Act
            localStore.storeActual(userWalletId, setOf(new))

            // Assert
            Truth.assertThat(persistenceStore.data.first()).isEqualTo(mapOf("011" to setOf(old, new)))
            Truth.assertThat(runtimeStore.getSyncOrNull())
                .isEqualTo(mapOf(userWalletId to setOf(StakingBalance.Empty(blankId, StatusSource.ACTUAL))))
        }

    @Test
    fun `GIVEN blank address wrappers piled up in persistence WHEN store restarted THEN both balances restored with same staking id`() =
        runTest {
            // Arrange
            val blankId = StakingID(stakingId.integrationId, "")
            val old = MockYieldBalanceWrapperDTOFactory.createWithBalance(blankId)
            val new = MockYieldBalanceWrapperDTOFactory.createWithEmptyBalance(blankId)
            persistenceStore.updateData { mapOf("011" to setOf(old)) }
            DefaultStakeKitBalancesStore(runtimeStore, persistenceStore, TestAppCoroutineScope())
                .storeActual(userWalletId, setOf(new))
            val restartedRuntime = RuntimeSharedStore<WalletIdWithStakingBalances>()

            // Act
            DefaultStakeKitBalancesStore(restartedRuntime, persistenceStore, TestAppCoroutineScope())

            // Assert
            val expected = mapOf(
                userWalletId to setOf(
                    old.toDomain(source = StatusSource.CACHE),
                    new.toDomain(source = StatusSource.CACHE),
                ),
            )
            Truth.assertThat(restartedRuntime.getSyncOrNull()).isEqualTo(expected)
        }

    @Test
    fun `GIVEN new wrapper without integration id WHEN storeActual on empty stores THEN persisted but skipped in runtime`() =
        runTest {
            // Arrange
            val wrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance().copy(integrationId = null)

            // Act
            store.storeActual(userWalletId, setOf(wrapper))

            // Assert
            Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(mapOf(userWalletId to emptySet<StakingBalance>()))
            Truth.assertThat(persistenceStore.data.first()["011"]).isEqualTo(setOf(wrapper))
        }

    private companion object {

        val userWalletId = UserWalletId(stringValue = "011")

        val otherWalletId = UserWalletId(stringValue = "022")

        val stakingId = MockYieldBalanceWrapperDTOFactory.defaultStakingId

        val stakingIds = setOf(
            stakingId,
            StakingID(
                integrationId = "solana-sol-native-multivalidator-staking",
                address = "0x1",
            ),
        )

        val solanaId = stakingIds.last()
    }
}