package com.tangem.data.staking.store

import androidx.datastore.core.DataStore
import com.google.common.truth.Truth
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.common.test.data.staking.MockYieldBalanceWrapperDTOFactory
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.data.staking.toDomain
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
[REDACTED_AUTHOR]
 */
internal class StakingBalancesStoreInitializationTest {

    @Test
    fun `GIVEN persistence emits nothing WHEN store created THEN runtime stays uninitialized`() = runTest {
        // Arrange
        val runtimeStore = RuntimeSharedStore<WalletIdWithStakingBalances>()
        val persistenceStore: DataStore<WalletIdWithWrappers> = mockk()

        every { persistenceStore.data } returns emptyFlow()

        // Act
        DefaultStakeKitBalancesStore(
            runtimeStore = runtimeStore,
            persistenceStore = persistenceStore,
            scope = TestAppCoroutineScope(),
        )

        // Assert
        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(null)
    }

    @Test
    fun `GIVEN persistence holds empty map WHEN store created THEN runtime holds empty map`() = runTest {
        // Arrange
        val runtimeStore = RuntimeSharedStore<WalletIdWithStakingBalances>()
        val persistenceStore = MockStateDataStore<WalletIdWithWrappers>(default = emptyMap())

        // Act
        DefaultStakeKitBalancesStore(
            runtimeStore = runtimeStore,
            persistenceStore = persistenceStore,
            scope = TestAppCoroutineScope(),
        )

        // Assert
        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(emptyMap<String, Set<StakingBalance>>())
    }

    @Test
    fun `GIVEN persisted wrapper WHEN store created THEN runtime holds CACHE balance`() = runTest {
        // Arrange
        val runtimeStore = RuntimeSharedStore<WalletIdWithStakingBalances>()
        val persistenceStore = MockStateDataStore<WalletIdWithWrappers>(default = emptyMap())

        val wrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance()

        persistenceStore.updateData {
            it.toMutableMap().apply {
                put(userWalletId.stringValue, setOf(wrapper))
            }
        }

        // Act
        DefaultStakeKitBalancesStore(
            runtimeStore = runtimeStore,
            persistenceStore = persistenceStore,
            scope = TestAppCoroutineScope(),
        )

        // Assert
        val expected = mapOf(userWalletId to setOf(wrapper.toDomain()))

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(expected)
    }

    private companion object {
        val userWalletId = UserWalletId(stringValue = "011")
    }
}