package com.tangem.data.networks.store

import com.google.common.truth.Truth
import com.tangem.common.test.datastore.MockStateDataStore
import com.tangem.common.test.domain.network.MockNetworkStatusFactory
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.data.networks.toDataModel
import com.tangem.data.networks.toSimple
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
internal class StoreStatusTest {

    private val runtimeStore = RuntimeSharedStore<WalletIdWithSimpleStatus>()
    private val persistenceStore = MockStateDataStore<WalletIdWithStatusDM>(default = emptyMap())

    private val store = DefaultNetworksStatusesStore(
        runtimeStore = runtimeStore,
        persistenceDataStore = persistenceStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @Test
    fun `storeStatus if runtime and cache stores are empty`() = runTest {
        val newStatus = MockNetworkStatusFactory.createVerified(network)

        store.storeStatus(userWalletId = userWalletId, status = newStatus)

        val runtimeExpected = mapOf(
            userWalletId.stringValue to setOf(newStatus.toSimple()),
        )

        val persistenceExpected = mapOf(
            userWalletId.stringValue to setOf(newStatus.toDataModel()!!),
        )

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(persistenceExpected)
    }

    @Test
    fun `storeStatus if runtime and cache stores contain empty map`() = runTest {
        runtimeStore.store(value = emptyMap())
        persistenceStore.updateData { emptyMap() }

        val newStatus = MockNetworkStatusFactory.createVerified(network)

        store.storeStatus(userWalletId = userWalletId, status = newStatus)

        val runtimeExpected = mapOf(
            userWalletId.stringValue to setOf(newStatus.toSimple()),
        )

        val persistenceExpected = mapOf(
            userWalletId.stringValue to setOf(newStatus.toDataModel()!!),
        )

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(persistenceExpected)
    }

    @Test
    fun `storeStatus if runtime and cache stores contain portfolio with empty statuses`() = runTest {
        runtimeStore.store(
            value = mapOf(userWalletId.stringValue to setOf()),
        )

        persistenceStore.updateData {
            mapOf(userWalletId.stringValue to setOf())
        }

        val newStatus = MockNetworkStatusFactory.createVerified(network)

        store.storeStatus(userWalletId = userWalletId, status = newStatus)

        val runtimeExpected = mapOf(
            userWalletId.stringValue to setOf(newStatus.toSimple()),
        )

        val persistenceExpected = mapOf(
            userWalletId.stringValue to setOf(newStatus.toDataModel()!!),
        )

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(persistenceExpected)
    }

    @Test
    fun `storeStatus if runtime and cache stores contain status with this network`() = runTest {
        val prevStatus = MockNetworkStatusFactory.createVerified(network)

        runtimeStore.store(
            value = mapOf(userWalletId.stringValue to setOf(prevStatus.toSimple())),
        )

        persistenceStore.updateData {
            mapOf(userWalletId.stringValue to setOf(prevStatus.toDataModel()!!))
        }

        val newStatus = MockNetworkStatusFactory.createVerified(network).copy(
            value = MockNetworkStatusFactory.createVerified(network).value.copySealed(source = StatusSource.ACTUAL),
        )

        store.storeStatus(userWalletId = userWalletId, status = newStatus)

        val runtimeExpected = mapOf(
            userWalletId.stringValue to setOf(newStatus.toSimple()),
        )

        val persistenceExpected = mapOf(
            userWalletId.stringValue to setOf(newStatus.toDataModel()!!),
        )

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(persistenceExpected)
    }

    @Test
    fun `store unreachable status if runtime and cache stores are empty`() = runTest {
        val newStatus = MockNetworkStatusFactory.createUnreachable(network)

        store.storeStatus(userWalletId = userWalletId, status = newStatus)

        val runtimeExpected = mapOf(
            userWalletId.stringValue to setOf(newStatus.toSimple()),
        )

        val persistenceExpected = emptyMap<String, NetworkStatusDM>()

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(persistenceExpected)
    }

    @Test
    fun `store unreachable status if runtime and cache stores contain empty map`() = runTest {
        runtimeStore.store(value = emptyMap())
        persistenceStore.updateData { emptyMap() }

        val newStatus = MockNetworkStatusFactory.createUnreachable(network)

        store.storeStatus(userWalletId = userWalletId, status = newStatus)

        val runtimeExpected = mapOf(
            userWalletId.stringValue to setOf(newStatus.toSimple()),
        )

        val persistenceExpected = emptyMap<String, NetworkStatusDM>()

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(persistenceExpected)
    }

    @Test
    fun `store unreachable status if runtime and cache stores contain portfolio with empty statuses`() = runTest {
        runtimeStore.store(
            value = mapOf(userWalletId.stringValue to setOf()),
        )

        persistenceStore.updateData {
            mapOf(userWalletId.stringValue to setOf())
        }

        val newStatus = MockNetworkStatusFactory.createUnreachable(network)

        store.storeStatus(userWalletId = userWalletId, status = newStatus)

        val runtimeExpected = mapOf(
            userWalletId.stringValue to setOf(newStatus.toSimple()),
        )

        val persistenceExpected = mapOf(userWalletId.stringValue to emptySet<NetworkStatusDM>())

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(persistenceExpected)
    }

    @Test
    fun `store unreachable status if runtime and cache stores contain verified status with this network`() = runTest {
        val prevStatus = MockNetworkStatusFactory.createVerified(network)

        runtimeStore.store(
            value = mapOf(userWalletId.stringValue to setOf(prevStatus.toSimple())),
        )

        persistenceStore.updateData {
            mapOf(userWalletId.stringValue to setOf(prevStatus.toDataModel()!!))
        }

        val newStatus = MockNetworkStatusFactory.createUnreachable(network)
        store.storeStatus(userWalletId = userWalletId, status = newStatus)

        val runtimeExpected = mapOf(
            userWalletId.stringValue to setOf(
                MockNetworkStatusFactory.createVerified(network = network, source = StatusSource.ONLY_CACHE).toSimple(),
            ),
        )

        val persistenceExpected = mapOf(
            userWalletId.stringValue to setOf(prevStatus.toDataModel()!!),
        )

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(persistenceExpected)
    }

    @Test
    fun `store unreachable status if stores contain missed derivation status with this network`() = runTest {
        val prevStatus = MockNetworkStatusFactory.createMissedDerivation(network)

        runtimeStore.store(
            value = mapOf(userWalletId.stringValue to setOf(prevStatus.toSimple())),
        )

        persistenceStore.updateData {
            mapOf(userWalletId.stringValue to emptySet())
        }

        val newStatus = MockNetworkStatusFactory.createUnreachable(network)

        store.storeStatus(userWalletId = userWalletId, status = newStatus)

        val runtimeExpected = mapOf(
            userWalletId.stringValue to setOf(newStatus.toSimple()),
        )

        val persistenceExpected = mapOf(userWalletId.stringValue to emptySet<NetworkStatusDM>())

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(persistenceExpected)
    }

    private companion object {

        val userWalletId = UserWalletId(stringValue = "011")

        val network = MockCryptoCurrencyFactory().ethereum.network
    }
}