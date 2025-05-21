package com.tangem.data.networks.store

import com.google.common.truth.Truth
import com.tangem.common.test.datastore.MockStateDataStore
import com.tangem.common.test.domain.network.MockNetworkStatusFactory
import com.tangem.data.networks.models.SimpleNetworkStatus
import com.tangem.data.networks.toDataModel
import com.tangem.data.networks.toSimple
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
[REDACTED_AUTHOR]
 */
@RunWith(Parameterized::class)
internal class ParameterizedStoreTest(private val model: Model) {

    private val runtimeStore = RuntimeSharedStore<WalletIdWithSimpleStatus>()
    private val persistenceStore = MockStateDataStore<WalletIdWithStatusDM>(default = emptyMap())

    private val store = DefaultNetworksStatusesStoreV2(
        runtimeStore = runtimeStore,
        persistenceDataStore = persistenceStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @Test
    fun `test store method`() = runTest {
        store.store(userWalletId = userWalletId, status = model.status)

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(model.runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(model.persistenceExpected)
    }

    data class Model(
        val status: NetworkStatus,
        val runtimeExpected: Map<String, Set<SimpleNetworkStatus>>,
        val persistenceExpected: WalletIdWithStatusDM,
    )

    private companion object {

        val userWalletId = UserWalletId(stringValue = "011")

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Model> {
            return listOf(
                MockNetworkStatusFactory.createVerified().let { status ->
                    Model(
                        status = status,
                        runtimeExpected = userWalletIdWithSimple(status),
                        persistenceExpected = userWalletIdWithData(status),
                    )
                },
                MockNetworkStatusFactory.createNoAccount().let { status ->
                    Model(
                        status = status,
                        runtimeExpected = userWalletIdWithSimple(status),
                        persistenceExpected = userWalletIdWithData(status),
                    )
                },
                MockNetworkStatusFactory.createMissedDerivation().let { status ->
                    Model(
                        status = status,
                        runtimeExpected = userWalletIdWithSimple(status),
                        persistenceExpected = mapOf(),
                    )
                },
                MockNetworkStatusFactory.createUnreachable().let { status ->
                    Model(
                        status = status,
                        runtimeExpected = userWalletIdWithSimple(status),
                        persistenceExpected = mapOf(),
                    )
                },
            )
        }

        fun userWalletIdWithSimple(status: NetworkStatus) = mapOf(
            userWalletId.stringValue to setOf(status.toSimple()),
        )

        fun userWalletIdWithData(status: NetworkStatus) = mapOf(
            userWalletId.stringValue to setOf(status.toDataModel()!!),
        )
    }
}