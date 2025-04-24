package com.tangem.data.networks.store

import com.google.common.truth.Truth
import com.tangem.common.test.datastore.MockStateDataStore
import com.tangem.common.test.domain.network.MockNetworkStatusFactory
import com.tangem.common.test.utils.getEmittedValues
import com.tangem.data.networks.models.SimpleNetworkStatus
import com.tangem.data.networks.toSimple
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
internal class NetworksStatusesStoreGetMethodTest {

    private val runtimeStore = RuntimeSharedStore<WalletIdWithSimpleStatus>()
    private val persistenceStore = MockStateDataStore<WalletIdWithStatusDM>(default = emptyMap())

    private val store = DefaultNetworksStatusesStoreV2(
        runtimeStore = runtimeStore,
        persistenceDataStore = persistenceStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @Test
    fun `test get if runtime store is empty`() = runTest {
        val actual = store.get(userWalletId = userWalletId)

        val values = getEmittedValues(flow = actual)

        Truth.assertThat(values).isEqualTo(emptyList<Set<SimpleNetworkStatus>>())
    }

    @Test
    fun `test get if runtime store contains empty map`() = runTest {
        runtimeStore.store(value = emptyMap())

        val actual = store.get(userWalletId = userWalletId)

        val values = getEmittedValues(flow = actual)

        Truth.assertThat(values).isEqualTo(emptyList<Set<SimpleNetworkStatus>>())
    }

    @Test
    fun `test get if runtime store contains portfolio with empty statuses`() = runTest {
        runtimeStore.store(
            value = mapOf(userWalletId.stringValue to emptySet()),
        )

        val actual = store.get(userWalletId = userWalletId)

        val values = getEmittedValues(flow = actual)

        Truth.assertThat(values.size).isEqualTo(1)
        Truth.assertThat(values).isEqualTo(listOf(emptySet<SimpleNetworkStatus>()))
    }

    @Test
    fun `test get if runtime store is not empty`() = runTest {
        val status = MockNetworkStatusFactory.createVerified()

        runtimeStore.store(
            value = mapOf(userWalletId.stringValue to setOf(status.toSimple())),
        )

        val actual = store.get(userWalletId = userWalletId)

        val values = getEmittedValues(flow = actual)

        Truth.assertThat(values.size).isEqualTo(1)
        Truth.assertThat(values).isEqualTo(listOf(setOf(status.toSimple())))
    }

    private companion object {

        val userWalletId = UserWalletId(stringValue = "011")
    }
}