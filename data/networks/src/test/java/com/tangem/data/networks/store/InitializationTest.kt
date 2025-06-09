package com.tangem.data.networks.store

import androidx.datastore.core.DataStore
import com.google.common.truth.Truth
import com.tangem.common.test.datastore.MockStateDataStore
import com.tangem.common.test.domain.network.MockNetworkStatusFactory
import com.tangem.data.networks.models.SimpleNetworkStatus
import com.tangem.data.networks.toDataModel
import com.tangem.data.networks.toSimple
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
internal class InitializationTest {

    @Test
    fun `test initialization if cache store is empty`() = runTest {
        val runtimeStore = RuntimeSharedStore<WalletIdWithSimpleStatus>()
        val persistenceStore: DataStore<WalletIdWithStatusDM> = mockk()

        every { persistenceStore.data } returns emptyFlow()

        DefaultNetworksStatusesStore(
            runtimeStore = runtimeStore,
            persistenceDataStore = persistenceStore,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(null)
    }

    @Test
    fun `test initialization if cache store contains empty map`() = runTest {
        val runtimeStore = RuntimeSharedStore<WalletIdWithSimpleStatus>()
        val persistenceStore = MockStateDataStore<WalletIdWithStatusDM>(default = emptyMap())

        DefaultNetworksStatusesStore(
            runtimeStore = runtimeStore,
            persistenceDataStore = persistenceStore,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(emptyMap<String, Set<SimpleNetworkStatus>>())
    }

    @Test
    fun `test initialization if cache store is not empty`() = runTest {
        val runtimeStore = RuntimeSharedStore<WalletIdWithSimpleStatus>()
        val persistenceStore = MockStateDataStore<WalletIdWithStatusDM>(default = emptyMap())

        val status = MockNetworkStatusFactory.createVerified()

        persistenceStore.updateData {
            it.toMutableMap().apply {
                put(userWalletId.stringValue, setOf(status.toDataModel()!!))
            }
        }

        DefaultNetworksStatusesStore(
            runtimeStore = runtimeStore,
            persistenceDataStore = persistenceStore,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )

        val expectedStatus = status.toSimple().copy(value = status.value.copySealed(source = StatusSource.CACHE))
        val expected = mapOf(userWalletId.stringValue to setOf(expectedStatus))

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(expected)
    }

    private companion object {
        val userWalletId = UserWalletId(stringValue = "011")
    }
}