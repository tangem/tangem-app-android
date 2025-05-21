package com.tangem.data.networks.store

import com.google.common.truth.Truth
import com.tangem.common.test.datastore.MockStateDataStore
import com.tangem.common.test.domain.network.MockNetworkStatusFactory
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.data.networks.toSimple
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
internal class SetSourceAsOnlyCacheTest {

    private val runtimeStore = RuntimeSharedStore<WalletIdWithSimpleStatus>()
    private val persistenceStore = MockStateDataStore<WalletIdWithStatusDM>(default = emptyMap())

    private val store = DefaultNetworksStatusesStoreV2(
        runtimeStore = runtimeStore,
        persistenceDataStore = persistenceStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @Test
    fun `setSourceAsOnlyCache if runtime store is empty`() = runTest {
        store.setSourceAsOnlyCache(userWalletId = userWalletId, network = network)

        val status = MockNetworkStatusFactory.createUnreachable()
        val runtimeExpected = mapOf(userWalletId.stringValue to setOf(status.toSimple()))

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<NetworkStatusDM>>())
    }

    @Test
    fun `setSourceAsOnlyCache with value if runtime store is empty`() = runTest {
        val status = MockNetworkStatusFactory.createUnreachable()

        store.setSourceAsOnlyCache(
            userWalletId = userWalletId,
            network = network,
            value = status.value as NetworkStatus.Unreachable,
        )

        val runtimeExpected = mapOf(userWalletId.stringValue to setOf(status.toSimple()))

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<NetworkStatusDM>>())
    }

    @Test
    fun `setSourceAsOnlyCache if runtime store contains empty map`() = runTest {
        runtimeStore.store(value = emptyMap())

        store.setSourceAsOnlyCache(userWalletId = userWalletId, network = network)

        val status = MockNetworkStatusFactory.createUnreachable()
        val runtimeExpected = mapOf(userWalletId.stringValue to setOf(status.toSimple()))

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<NetworkStatusDM>>())
    }

    @Test
    fun `setSourceAsOnlyCache with value if runtime store contains empty map`() = runTest {
        val status = MockNetworkStatusFactory.createUnreachable()

        runtimeStore.store(value = emptyMap())

        store.setSourceAsOnlyCache(
            userWalletId = userWalletId,
            network = network,
            value = status.value as NetworkStatus.Unreachable,
        )

        val runtimeExpected = mapOf(userWalletId.stringValue to setOf(status.toSimple()))

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<NetworkStatusDM>>())
    }

    @Test
    fun `setSourceAsOnlyCache if runtime store contains portfolio with empty statuses`() = runTest {
        runtimeStore.store(
            value = mapOf(userWalletId.stringValue to emptySet()),
        )

        store.setSourceAsOnlyCache(userWalletId = userWalletId, network = network)

        val status = MockNetworkStatusFactory.createUnreachable()
        val runtimeExpected = mapOf(userWalletId.stringValue to setOf(status.toSimple()))

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<NetworkStatusDM>>())
    }

    @Test
    fun `setSourceAsOnlyCache with value if runtime store contains portfolio with empty statuses`() = runTest {
        val status = MockNetworkStatusFactory.createUnreachable()

        runtimeStore.store(
            value = mapOf(userWalletId.stringValue to emptySet()),
        )

        store.setSourceAsOnlyCache(
            userWalletId = userWalletId,
            network = network,
            value = status.value as NetworkStatus.Unreachable,
        )

        val runtimeExpected = mapOf(userWalletId.stringValue to setOf(status.toSimple()))

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<NetworkStatusDM>>())
    }

    @Test
    fun `setSourceAsOnlyCache the single network if runtime store contains status with this network`() = runTest {
        val status = MockNetworkStatusFactory.createVerified(network).toSimple()

        runtimeStore.store(
            value = mapOf(userWalletId.stringValue to setOf(status)),
        )

        store.setSourceAsOnlyCache(userWalletId = userWalletId, network = network)

        val runtimeExpected = mapOf(
            userWalletId.stringValue to setOf(
                status.copy(value = status.value.copySealed(source = StatusSource.ONLY_CACHE)),
            ),
        )

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<NetworkStatusDM>>())
    }

    @Test
    fun `setSourceAsOnlyCache the multi networks if runtime store is empty`() = runTest {
        store.setSourceAsOnlyCache(userWalletId = userWalletId, networks = networks)

        val statuses = networks.map { MockNetworkStatusFactory.createUnreachable(it).toSimple() }.toSet()
        val runtimeExpected = mapOf(userWalletId.stringValue to statuses)

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<NetworkStatusDM>>())
    }

    @Test
    fun `setSourceAsOnlyCache the multi networks if runtime store contains empty map`() = runTest {
        runtimeStore.store(value = emptyMap())

        store.setSourceAsOnlyCache(userWalletId = userWalletId, networks = networks)

        val statuses = networks.map { MockNetworkStatusFactory.createUnreachable(it).toSimple() }.toSet()
        val runtimeExpected = mapOf(userWalletId.stringValue to statuses)

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<NetworkStatusDM>>())
    }

    @Test
    fun `setSourceAsOnlyCache the multi networks if runtime store contains portfolio with empty statuses`() = runTest {
        runtimeStore.store(
            value = mapOf(userWalletId.stringValue to emptySet()),
        )

        store.setSourceAsOnlyCache(userWalletId = userWalletId, networks = networks)

        val statuses = networks.map { MockNetworkStatusFactory.createUnreachable(it).toSimple() }.toSet()
        val runtimeExpected = mapOf(userWalletId.stringValue to statuses)

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<NetworkStatusDM>>())
    }

    @Test
    fun `setSourceAsOnlyCache the multi networks if runtime store contains status with this network`() = runTest {
        val firstStatus = MockNetworkStatusFactory.createVerified(network = networks.first()).toSimple()
        val secondStatus = MockNetworkStatusFactory.createVerified(network = networks.last()).toSimple()

        runtimeStore.store(
            value = mapOf(userWalletId.stringValue to setOf(firstStatus, secondStatus)),
        )

        store.setSourceAsOnlyCache(userWalletId = userWalletId, networks = networks)

        val runtimeExpected = mapOf(
            userWalletId.stringValue to setOf(
                firstStatus.copy(value = firstStatus.value.copySealed(source = StatusSource.ONLY_CACHE)),
                secondStatus.copy(value = secondStatus.value.copySealed(source = StatusSource.ONLY_CACHE)),
            ),
        )

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<NetworkStatusDM>>())
    }

    private companion object {

        val userWalletId = UserWalletId(stringValue = "011")

        val network = MockCryptoCurrencyFactory().ethereum.network

        val networks = MockCryptoCurrencyFactory().ethereumAndStellar.mapTo(hashSetOf()) { it.network }
    }
}