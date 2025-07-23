package com.tangem.data.networks.store

import com.google.common.truth.Truth
import com.tangem.common.test.datastore.MockStateDataStore
import com.tangem.common.test.domain.network.MockNetworkStatusFactory
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.data.networks.models.SimpleNetworkStatus
import com.tangem.data.networks.toDataModel
import com.tangem.data.networks.toSimple
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
internal class UpdateStatusSourceTest {

    private val runtimeStore = RuntimeSharedStore<WalletIdWithSimpleStatus>()
    private val persistenceStore = MockStateDataStore<WalletIdWithStatusDM>(default = emptyMap())

    private val store = DefaultNetworksStatusesStore(
        runtimeStore = runtimeStore,
        persistenceDataStore = persistenceStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @Test
    fun `updateStatusSource if stores are empty`() = runTest {
        store.updateStatusSource(
            userWalletId = userWalletId,
            network = network,
            source = StatusSource.ACTUAL,
        )

        val runtimeExpected = mapOf(userWalletId.stringValue to emptySet<SimpleNetworkStatus>())

        val persistenceExpected = emptyMap<String, Set<NetworkStatusDM>>()

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(persistenceExpected)
    }

    @Test
    fun `updateStatusSource with ifNotFound if stores are empty`() = runTest {
        val default = MockNetworkStatusFactory.createUnreachable(network).copy(
            value = NetworkStatus.Unreachable(
                address = NetworkAddress.Single(
                    defaultAddress = NetworkAddress.Address(
                        value = "",
                        type = NetworkAddress.Address.Type.Primary,
                    ),
                ),
            ),
        )

        store.updateStatusSource(
            userWalletId = userWalletId,
            network = network,
            source = StatusSource.ACTUAL,
            ifNotFound = { default.toSimple() },
        )

        val runtimeExpected = mapOf(userWalletId.stringValue to setOf(default.toSimple()))

        val persistenceExpected = emptyMap<String, Set<NetworkStatusDM>>()

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(persistenceExpected)
    }

    @Test
    fun `updateStatusSource if stores contain empty map`() = runTest {
        runtimeStore.store(value = emptyMap())
        persistenceStore.updateData { emptyMap() }

        store.updateStatusSource(
            userWalletId = userWalletId,
            network = network,
            source = StatusSource.ACTUAL,
        )

        val runtimeExpected = mapOf(userWalletId.stringValue to emptySet<SimpleNetworkStatus>())

        val persistenceExpected = emptyMap<String, Set<NetworkStatusDM>>()

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(persistenceExpected)
    }

    @Test
    fun `updateStatusSource with ifNotFound if stores contain empty map`() = runTest {
        runtimeStore.store(value = emptyMap())
        persistenceStore.updateData { emptyMap() }

        val default = MockNetworkStatusFactory.createUnreachable(network).copy(
            value = NetworkStatus.Unreachable(
                address = NetworkAddress.Single(
                    defaultAddress = NetworkAddress.Address(
                        value = "",
                        type = NetworkAddress.Address.Type.Primary,
                    ),
                ),
            ),
        )

        store.updateStatusSource(
            userWalletId = userWalletId,
            network = network,
            source = StatusSource.ACTUAL,
            ifNotFound = { default.toSimple() },
        )

        val runtimeExpected = mapOf(userWalletId.stringValue to setOf(default.toSimple()))

        val persistenceExpected = emptyMap<String, Set<NetworkStatusDM>>()

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(persistenceExpected)
    }

    @Test
    fun `updateStatusSource if stores contain portfolio with empty statuses`() = runTest {
        runtimeStore.store(
            value = mapOf(userWalletId.stringValue to setOf()),
        )

        persistenceStore.updateData {
            mapOf(userWalletId.stringValue to setOf())
        }

        store.updateStatusSource(
            userWalletId = userWalletId,
            network = network,
            source = StatusSource.ACTUAL,
        )

        val runtimeExpected = mapOf(userWalletId.stringValue to emptySet<SimpleNetworkStatus>())
        val persistenceExpected = mapOf(userWalletId.stringValue to emptySet<NetworkStatusDM>())

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(persistenceExpected)
    }

    @Test
    fun `updateStatusSource with ifNotFound if stores contain portfolio with empty statuses`() = runTest {
        runtimeStore.store(
            value = mapOf(userWalletId.stringValue to setOf()),
        )

        persistenceStore.updateData {
            mapOf(userWalletId.stringValue to setOf())
        }

        val default = MockNetworkStatusFactory.createUnreachable(network).copy(
            value = NetworkStatus.Unreachable(
                address = NetworkAddress.Single(
                    defaultAddress = NetworkAddress.Address(
                        value = "",
                        type = NetworkAddress.Address.Type.Primary,
                    ),
                ),
            ),
        )

        store.updateStatusSource(
            userWalletId = userWalletId,
            network = network,
            source = StatusSource.ACTUAL,
            ifNotFound = { default.toSimple() },
        )

        val runtimeExpected = mapOf(userWalletId.stringValue to setOf(default.toSimple()))
        val persistenceExpected = mapOf(userWalletId.stringValue to emptySet<NetworkStatusDM>())

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(persistenceExpected)
    }

    @Test
    fun `updateStatusSource if stores contain status with this network`() = runTest {
        val prevStatus = MockNetworkStatusFactory.createVerified(network)

        runtimeStore.store(
            value = mapOf(userWalletId.stringValue to setOf(prevStatus.toSimple())),
        )

        persistenceStore.updateData {
            mapOf(userWalletId.stringValue to setOf(prevStatus.toDataModel()!!))
        }

        store.updateStatusSource(
            userWalletId = userWalletId,
            network = network,
            source = StatusSource.ACTUAL,
        )

        val newStatus = MockNetworkStatusFactory.createVerified(network)

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
    fun `updateStatusSource with ifNotFound if stores contain status with this network`() = runTest {
        val prevStatus = MockNetworkStatusFactory.createVerified(network)

        runtimeStore.store(
            value = mapOf(userWalletId.stringValue to setOf(prevStatus.toSimple())),
        )

        persistenceStore.updateData {
            mapOf(userWalletId.stringValue to setOf(prevStatus.toDataModel()!!))
        }

        store.updateStatusSource(
            userWalletId = userWalletId,
            network = network,
            source = StatusSource.ACTUAL,
            ifNotFound = { prevStatus.toSimple() },
        )

        val newStatus = MockNetworkStatusFactory.createVerified(network)

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
    fun `updateStatusSource of networks if stores are empty`() = runTest {
        store.updateStatusSource(
            userWalletId = userWalletId,
            networks = networks,
            source = StatusSource.ACTUAL,
        )

        val runtimeExpected = mapOf(userWalletId.stringValue to emptySet<SimpleNetworkStatus>())

        val persistenceExpected = emptyMap<String, Set<NetworkStatusDM>>()

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(persistenceExpected)
    }

    @Test
    fun `updateStatusSource of networks with ifNotFound if stores are empty`() = runTest {
        val default = MockNetworkStatusFactory.createUnreachable(network).copy(
            value = NetworkStatus.Unreachable(
                address = NetworkAddress.Single(
                    defaultAddress = NetworkAddress.Address(
                        value = "",
                        type = NetworkAddress.Address.Type.Primary,
                    ),
                ),
            ),
        )

        store.updateStatusSource(
            userWalletId = userWalletId,
            networks = networks,
            source = StatusSource.ACTUAL,
            ifNotFound = { default.toSimple() },
        )

        val runtimeExpected = mapOf(userWalletId.stringValue to setOf(default.toSimple()))

        val persistenceExpected = emptyMap<String, Set<NetworkStatusDM>>()

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(persistenceExpected)
    }

    @Test
    fun `updateStatusSource of networks if stores contain empty map`() = runTest {
        runtimeStore.store(value = emptyMap())
        persistenceStore.updateData { emptyMap() }

        store.updateStatusSource(
            userWalletId = userWalletId,
            networks = networks,
            source = StatusSource.ACTUAL,
        )

        val runtimeExpected = mapOf(userWalletId.stringValue to emptySet<SimpleNetworkStatus>())

        val persistenceExpected = emptyMap<String, Set<NetworkStatusDM>>()

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(persistenceExpected)
    }

    @Test
    fun `updateStatusSource of networks with ifNotFound if stores contain empty map`() = runTest {
        runtimeStore.store(value = emptyMap())
        persistenceStore.updateData { emptyMap() }

        val default = MockNetworkStatusFactory.createUnreachable(network).copy(
            value = NetworkStatus.Unreachable(
                address = NetworkAddress.Single(
                    defaultAddress = NetworkAddress.Address(
                        value = "",
                        type = NetworkAddress.Address.Type.Primary,
                    ),
                ),
            ),
        )

        store.updateStatusSource(
            userWalletId = userWalletId,
            networks = networks,
            source = StatusSource.ACTUAL,
            ifNotFound = { default.toSimple() },
        )

        val runtimeExpected = mapOf(userWalletId.stringValue to setOf(default.toSimple()))

        val persistenceExpected = emptyMap<String, Set<NetworkStatusDM>>()

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(persistenceExpected)
    }

    @Test
    fun `updateStatusSource of networks if stores contain portfolio with empty statuses`() = runTest {
        runtimeStore.store(
            value = mapOf(userWalletId.stringValue to setOf()),
        )

        persistenceStore.updateData {
            mapOf(userWalletId.stringValue to setOf())
        }

        store.updateStatusSource(
            userWalletId = userWalletId,
            networks = networks,
            source = StatusSource.ACTUAL,
        )

        val runtimeExpected = mapOf(userWalletId.stringValue to emptySet<SimpleNetworkStatus>())
        val persistenceExpected = mapOf(userWalletId.stringValue to emptySet<NetworkStatusDM>())

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(persistenceExpected)
    }

    @Test
    fun `updateStatusSource of networks with ifNotFound if stores contain portfolio with empty statuses`() = runTest {
        runtimeStore.store(
            value = mapOf(userWalletId.stringValue to setOf()),
        )

        persistenceStore.updateData {
            mapOf(userWalletId.stringValue to setOf())
        }

        val default = MockNetworkStatusFactory.createUnreachable(network).copy(
            value = NetworkStatus.Unreachable(
                address = NetworkAddress.Single(
                    defaultAddress = NetworkAddress.Address(
                        value = "",
                        type = NetworkAddress.Address.Type.Primary,
                    ),
                ),
            ),
        )

        store.updateStatusSource(
            userWalletId = userWalletId,
            networks = networks,
            source = StatusSource.ACTUAL,
            ifNotFound = { default.toSimple() },
        )

        val runtimeExpected = mapOf(userWalletId.stringValue to setOf(default.toSimple()))
        val persistenceExpected = mapOf(userWalletId.stringValue to emptySet<NetworkStatusDM>())

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(persistenceExpected)
    }

    @Test
    fun `updateStatusSource of networks if stores contain status with this network`() = runTest {
        val prevStatus = MockNetworkStatusFactory.createVerified(network)

        runtimeStore.store(
            value = mapOf(userWalletId.stringValue to setOf(prevStatus.toSimple())),
        )

        persistenceStore.updateData {
            mapOf(userWalletId.stringValue to setOf(prevStatus.toDataModel()!!))
        }

        store.updateStatusSource(
            userWalletId = userWalletId,
            networks = networks,
            source = StatusSource.ACTUAL,
        )

        val newStatus = MockNetworkStatusFactory.createVerified(network)

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
    fun `updateStatusSource of networks with ifNotFound if stores contain status with this network`() = runTest {
        val prevStatus = MockNetworkStatusFactory.createVerified(network)

        runtimeStore.store(
            value = mapOf(userWalletId.stringValue to setOf(prevStatus.toSimple())),
        )

        persistenceStore.updateData {
            mapOf(userWalletId.stringValue to setOf(prevStatus.toDataModel()!!))
        }

        store.updateStatusSource(
            userWalletId = userWalletId,
            networks = networks,
            source = StatusSource.ACTUAL,
            ifNotFound = { prevStatus.toSimple() },
        )

        val newStatus = MockNetworkStatusFactory.createVerified(network)

        val runtimeExpected = mapOf(
            userWalletId.stringValue to setOf(newStatus.toSimple()),
        )

        val persistenceExpected = mapOf(
            userWalletId.stringValue to setOf(newStatus.toDataModel()!!),
        )

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(persistenceExpected)
    }

    private companion object {

        val userWalletId = UserWalletId(stringValue = "011")

        val network = MockCryptoCurrencyFactory().ethereum.network

        val networks = MockCryptoCurrencyFactory().ethereumAndStellar.mapTo(hashSetOf()) { it.network }
    }
}