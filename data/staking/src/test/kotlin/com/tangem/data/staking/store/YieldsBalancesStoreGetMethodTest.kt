package com.tangem.data.staking.store

import com.google.common.truth.Truth
import com.tangem.common.test.data.staking.MockYieldBalanceWrapperDTOFactory
import com.tangem.common.test.datastore.MockStateDataStore
import com.tangem.common.test.utils.getEmittedValues
import com.tangem.data.staking.toDomain
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.staking.YieldBalance
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
internal class YieldsBalancesStoreGetMethodTest {

    private val runtimeStore = RuntimeSharedStore<WalletIdWithBalances>()
    private val persistenceStore = MockStateDataStore<WalletIdWithWrappers>(default = emptyMap())

    private val store = DefaultYieldsBalancesStore(
        runtimeStore = runtimeStore,
        persistenceStore = persistenceStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @Test
    fun `test get if runtime store is empty`() = runTest {
        val actual = store.get(userWalletId = userWalletId)

        val values = getEmittedValues(flow = actual)

        val expected = listOf(emptySet<YieldBalance>())
        Truth.assertThat(values).isEqualTo(expected)
    }

    @Test
    fun `test get if runtime store contains empty map`() = runTest {
        runtimeStore.store(value = emptyMap())

        val actual = store.get(userWalletId = userWalletId)

        val values = getEmittedValues(flow = actual)

        val expected = listOf(emptySet<YieldBalance>())
        Truth.assertThat(values).isEqualTo(expected)
    }

    @Test
    fun `test get if runtime store contains portfolio with empty balances`() = runTest {
        runtimeStore.store(
            value = mapOf(userWalletId to emptySet()),
        )

        val actual = store.get(userWalletId = userWalletId)

        val values = getEmittedValues(flow = actual)

        Truth.assertThat(values.size).isEqualTo(1)
        Truth.assertThat(values).isEqualTo(listOf(emptySet<YieldBalance>()))
    }

    @Test
    fun `test get if runtime store is not empty`() = runTest {
        val wrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance()

        runtimeStore.store(
            value = mapOf(userWalletId to setOf(wrapper.toDomain())),
        )

        val actual = store.get(userWalletId = userWalletId)

        val values = getEmittedValues(flow = actual)

        Truth.assertThat(values.size).isEqualTo(1)
        Truth.assertThat(values).isEqualTo(listOf(setOf(wrapper.toDomain())))
    }

    private companion object {

        val userWalletId = UserWalletId(stringValue = "011")
    }
}