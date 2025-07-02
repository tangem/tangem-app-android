package com.tangem.data.networks.store

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.datastore.MockStateDataStore
import com.tangem.common.test.domain.network.MockNetworkStatusFactory
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
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
internal class GetTest {

    private val runtimeStore = RuntimeSharedStore<WalletIdWithSimpleStatus>()
    private val persistenceStore = MockStateDataStore<WalletIdWithStatusDM>(default = emptyMap())

    private val store = DefaultNetworksStatusesStore(
        runtimeStore = runtimeStore,
        persistenceDataStore = persistenceStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @Test
    fun `get if runtime store is empty`() = runTest {
        val actual = store.get(userWalletId = userWalletId)

        val values = getEmittedValues(flow = actual)

        Truth.assertThat(values).isEqualTo(emptyList<Set<SimpleNetworkStatus>>())
    }

    @Test
    fun `get if runtime store contains empty map`() = runTest {
        runtimeStore.store(value = emptyMap())

        val actual = store.get(userWalletId = userWalletId)

        val values = getEmittedValues(flow = actual)

        Truth.assertThat(values).isEqualTo(emptyList<Set<SimpleNetworkStatus>>())
    }

    @Test
    fun `get if runtime store contains portfolio with empty statuses`() = runTest {
        runtimeStore.store(
            value = mapOf(userWalletId.stringValue to emptySet()),
        )

        val actual = store.get(userWalletId = userWalletId)

        val values = getEmittedValues(flow = actual)

        Truth.assertThat(values.size).isEqualTo(1)
        Truth.assertThat(values).isEqualTo(listOf(emptySet<SimpleNetworkStatus>()))
    }

    @Test
    fun `get if runtime store is not empty`() = runTest {
        val status = MockNetworkStatusFactory.createVerified()

        runtimeStore.store(
            value = mapOf(userWalletId.stringValue to setOf(status.toSimple())),
        )

        val actual = store.get(userWalletId = userWalletId)

        val values = getEmittedValues(flow = actual)

        Truth.assertThat(values.size).isEqualTo(1)
        Truth.assertThat(values).isEqualTo(listOf(setOf(status.toSimple())))
    }

    @Test
    fun `getSyncOrNull if runtime store is empty`() = runTest {
        val network = MockNetworkStatusFactory.createVerified().network

        val actual = store.getSyncOrNull(userWalletId = userWalletId, network = network)

        Truth.assertThat(actual).isEqualTo(null)
    }

    @Test
    fun `getSyncOrNull if runtime store contains empty map`() = runTest {
        val network = MockNetworkStatusFactory.createVerified().network

        runtimeStore.store(value = emptyMap())

        val actual = store.getSyncOrNull(userWalletId = userWalletId, network = network)

        Truth.assertThat(actual).isEqualTo(null)
    }

    @Test
    fun `getSyncOrNull if runtime store contains portfolio with empty statuses`() = runTest {
        val network = MockNetworkStatusFactory.createVerified().network

        runtimeStore.store(
            value = mapOf(userWalletId.stringValue to emptySet()),
        )

        val actual = store.getSyncOrNull(userWalletId = userWalletId, network = network)

        Truth.assertThat(actual).isEqualTo(null)
    }

    @Test
    fun `getSyncOrNull if runtime store is not empty`() = runTest {
        val unreachableStatus = MockNetworkStatusFactory.createUnreachable(
            network = MockCryptoCurrencyFactory().createCoin(Blockchain.Ethereum).network,
        )
        val missedDerivationStatus = MockNetworkStatusFactory.createMissedDerivation(
            network = MockCryptoCurrencyFactory().createCoin(Blockchain.Bitcoin).network,
        )
        val noAccountStatus = MockNetworkStatusFactory.createNoAccount(
            network = MockCryptoCurrencyFactory().createCoin(Blockchain.Solana).network,
        )
        val verifiedStatus = MockNetworkStatusFactory.createVerified(
            network = MockCryptoCurrencyFactory().createCoin(Blockchain.Stellar).network,
        )

        runtimeStore.store(
            value = mapOf(
                userWalletId.stringValue to setOf(
                    unreachableStatus.toSimple(),
                    missedDerivationStatus.toSimple(),
                    noAccountStatus.toSimple(),
                    verifiedStatus.toSimple(),
                ),
            ),
        )

        val actual = listOf(unreachableStatus, missedDerivationStatus, noAccountStatus, verifiedStatus).map {
            store.getSyncOrNull(userWalletId = userWalletId, network = it.network)
        }

        val expected = listOf(
            unreachableStatus.toSimple(),
            missedDerivationStatus.toSimple(),
            noAccountStatus.toSimple(),
            verifiedStatus.toSimple(),
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    private companion object {

        val userWalletId = UserWalletId(stringValue = "011")
    }
}