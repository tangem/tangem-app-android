package com.tangem.data.staking.store

import androidx.datastore.core.DataStore
import com.google.common.truth.Truth
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.common.test.data.staking.MockYieldBalanceWrapperDTOFactory
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.data.staking.toDomain
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.getEmittedValues
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
[REDACTED_AUTHOR]
 */
internal class StakingBalancesStoreGetMethodTest {

    private val runtimeStore = RuntimeSharedStore<WalletIdWithStakingBalances>()
    private val persistenceStore = MockStateDataStore<WalletIdWithWrappers>(default = emptyMap())

    private val store = DefaultStakeKitBalancesStore(
        runtimeStore = runtimeStore,
        persistenceStore = persistenceStore,
        scope = TestAppCoroutineScope(),
    )

    @Test
    fun `GIVEN runtime initialized from empty persistence WHEN get THEN emits empty set`() = runTest {
        // Arrange
        val actual = store.get(userWalletId = userWalletId)

        // Act
        val values = getEmittedValues(flow = actual)

        // Assert
        val expected = listOf(emptySet<StakingBalance>())
        Truth.assertThat(values).isEqualTo(expected)
    }

    @Test
    fun `GIVEN runtime holds empty map WHEN get THEN emits empty set`() = runTest {
        // Arrange
        runtimeStore.store(value = emptyMap())

        val actual = store.get(userWalletId = userWalletId)

        // Act
        val values = getEmittedValues(flow = actual)

        // Assert
        val expected = listOf(emptySet<StakingBalance>())
        Truth.assertThat(values).isEqualTo(expected)
    }

    @Test
    fun `GIVEN wallet with empty balances WHEN get THEN emits empty set once`() = runTest {
        // Arrange
        runtimeStore.store(
            value = mapOf(userWalletId to emptySet()),
        )

        val actual = store.get(userWalletId = userWalletId)

        // Act
        val values = getEmittedValues(flow = actual)

        // Assert
        Truth.assertThat(values.size).isEqualTo(1)
        Truth.assertThat(values).isEqualTo(listOf(emptySet<StakingBalance>()))
    }

    @Test
    fun `GIVEN wallet with balance WHEN get THEN emits that balance`() = runTest {
        // Arrange
        val wrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance()

        runtimeStore.store(
            value = mapOf(userWalletId to setOf(wrapper.toDomain())),
        )

        val actual = store.get(userWalletId = userWalletId)

        // Act
        val values = getEmittedValues(flow = actual)

        // Assert
        Truth.assertThat(values.size).isEqualTo(1)
        Truth.assertThat(values).isEqualTo(listOf(setOf(wrapper.toDomain())))
    }

    @Test
    fun `GIVEN balance with staking id WHEN getSyncOrNull THEN returns it`() = runTest {
        // Arrange
        val wrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance()
        runtimeStore.store(value = mapOf(userWalletId to setOf(wrapper.toDomain())))

        // Act
        val actual = store.getSyncOrNull(userWalletId, MockYieldBalanceWrapperDTOFactory.defaultStakingId)

        // Assert
        Truth.assertThat(actual).isEqualTo(wrapper.toDomain())
    }

    @Test
    fun `GIVEN no balance with staking id WHEN getSyncOrNull THEN returns null`() = runTest {
        // Arrange
        val wrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance()
        runtimeStore.store(value = mapOf(userWalletId to setOf(wrapper.toDomain())))

        // Act
        val actual = store.getSyncOrNull(userWalletId, StakingID("solana-sol-native-multivalidator-staking", "0x1"))

        // Assert
        Truth.assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN unknown wallet WHEN getSyncOrNull THEN returns null`() = runTest {
        // Arrange
        val wrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance()
        runtimeStore.store(value = mapOf(userWalletId to setOf(wrapper.toDomain())))

        // Act
        val actual = store.getSyncOrNull(UserWalletId("022"), MockYieldBalanceWrapperDTOFactory.defaultStakingId)

        // Assert
        Truth.assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN wallet balances WHEN getAllSyncOrNull THEN returns whole set`() = runTest {
        // Arrange
        val wrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance()
        runtimeStore.store(value = mapOf(userWalletId to setOf(wrapper.toDomain())))

        // Act
        val actual = store.getAllSyncOrNull(userWalletId)

        // Assert
        Truth.assertThat(actual).isEqualTo(setOf(wrapper.toDomain()))
    }

    @Test
    fun `GIVEN unknown wallet WHEN getAllSyncOrNull THEN returns null`() = runTest {
        // Arrange
        val wrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance()
        runtimeStore.store(value = mapOf(userWalletId to setOf(wrapper.toDomain())))

        // Act
        val actual = store.getAllSyncOrNull(UserWalletId("022"))

        // Assert
        Truth.assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN runtime never initialized WHEN getSyncOrNull THEN returns null`() = runTest {
        // Arrange
        val localStore = createStoreWithNothingPersisted()

        // Act
        val actual = localStore.getSyncOrNull(userWalletId, MockYieldBalanceWrapperDTOFactory.defaultStakingId)

        // Assert
        Truth.assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN runtime never initialized WHEN getAllSyncOrNull THEN returns null`() = runTest {
        // Arrange
        val localStore = createStoreWithNothingPersisted()

        // Act
        val actual = localStore.getAllSyncOrNull(userWalletId)

        // Assert
        Truth.assertThat(actual).isNull()
    }

    private fun createStoreWithNothingPersisted(): DefaultStakeKitBalancesStore {
        val emptyPersistence: DataStore<WalletIdWithWrappers> = mockk()
        every { emptyPersistence.data } returns emptyFlow()

        return DefaultStakeKitBalancesStore(
            runtimeStore = RuntimeSharedStore(),
            persistenceStore = emptyPersistence,
            scope = TestAppCoroutineScope(),
        )
    }

    private companion object {

        val userWalletId = UserWalletId(stringValue = "011")
    }
}