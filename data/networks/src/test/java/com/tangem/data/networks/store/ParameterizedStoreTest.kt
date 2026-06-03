package com.tangem.data.networks.store

import com.google.common.truth.Truth
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.common.test.domain.network.MockNetworkStatusFactory
import com.tangem.data.networks.models.SimpleNetworkStatus
import com.tangem.data.networks.toDataModel
import com.tangem.data.networks.toSimple
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest

/**
[REDACTED_AUTHOR]
 */
internal class ParameterizedStoreTest {

    private val runtimeStore = RuntimeSharedStore<WalletIdWithSimpleStatus>()
    private val persistenceStore = MockStateDataStore<WalletIdWithStatusDM>(default = emptyMap())

    private val store = DefaultNetworksStatusesStore(
        context = mockk(),
        runtimeStore = runtimeStore,
        persistenceDataStore = persistenceStore,
        scope = TestAppCoroutineScope(),
    )

    @ParameterizedTest
    @ProvideTestModels
    fun `test store method`(model: Model) = runTest {
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
        fun provideTestModels(): Collection<Model> {
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