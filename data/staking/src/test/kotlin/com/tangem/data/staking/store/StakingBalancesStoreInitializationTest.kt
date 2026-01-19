package com.tangem.data.staking.store

import androidx.datastore.core.DataStore
import com.google.common.truth.Truth
import com.tangem.common.test.data.staking.MockYieldBalanceWrapperDTOFactory
import com.tangem.common.test.datastore.MockStateDataStore
import com.tangem.data.staking.toDomain
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
internal class StakingBalancesStoreInitializationTest {

    @Test
    fun `test initialization if cache store is empty`() = runTest {
        val runtimeStore = RuntimeSharedStore<WalletIdWithStakingBalances>()
        val persistenceStore: DataStore<WalletIdWithWrappers> = mockk()

        every { persistenceStore.data } returns emptyFlow()

        DefaultStakeKitBalancesStore(
            runtimeStore = runtimeStore,
            persistenceStore = persistenceStore,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(null)
    }

    @Test
    fun `test initialization if cache store contains empty map`() = runTest {
        val runtimeStore = RuntimeSharedStore<WalletIdWithStakingBalances>()
        val persistenceStore = MockStateDataStore<WalletIdWithWrappers>(default = emptyMap())

        DefaultStakeKitBalancesStore(
            runtimeStore = runtimeStore,
            persistenceStore = persistenceStore,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(emptyMap<String, Set<StakingBalance>>())
    }

    @Test
    fun `test initialization if cache store is not empty`() = runTest {
        val runtimeStore = RuntimeSharedStore<WalletIdWithStakingBalances>()
        val persistenceStore = MockStateDataStore<WalletIdWithWrappers>(default = emptyMap())

        val wrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance()

        persistenceStore.updateData {
            it.toMutableMap().apply {
                put(userWalletId.stringValue, setOf(wrapper))
            }
        }

        DefaultStakeKitBalancesStore(
            runtimeStore = runtimeStore,
            persistenceStore = persistenceStore,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )

        val expected = mapOf(userWalletId to setOf(wrapper.toDomain()))

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(expected)
    }

    private companion object {
        val userWalletId = UserWalletId(stringValue = "011")
    }
}