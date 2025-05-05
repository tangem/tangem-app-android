package com.tangem.data.networks.multi

import com.google.common.truth.Truth
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.common.test.domain.network.MockNetworkStatusFactory
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.test.utils.getEmittedValues
import com.tangem.data.networks.models.SimpleNetworkStatus
import com.tangem.data.networks.store.NetworksStatusesStoreV2
import com.tangem.data.networks.toSimple
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.configs.GenericCardConfig
import com.tangem.domain.networks.multi.MultiNetworkStatusProducer
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
internal class DefaultMultiNetworkStatusProducerTest {

    private val params = MultiNetworkStatusProducer.Params(userWalletId = userWallet.walletId)

    private val networksStatusesStore = mockk<NetworksStatusesStoreV2>()
    private val userWalletsStore = mockk<UserWalletsStore>()
    private val excludedBlockchains = mockk<ExcludedBlockchains>()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val producer = DefaultMultiNetworkStatusProducer(
        params = params,
        networksStatusesStore = networksStatusesStore,
        userWalletsStore = userWalletsStore,
        excludedBlockchains = excludedBlockchains,
        dispatchers = dispatchers,
    )

    @Before
    fun setup() {
        every { excludedBlockchains.contains(any()) } returns false
    }

    @Test
    fun `test that flow is mapped for user wallet id from params`() = runTest {
        val statuses = setOf(
            MockNetworkStatusFactory.createVerified(ethNetwork),
            MockNetworkStatusFactory.createVerified(cardanoNetwork),
        )

        val networksStatusesFlow = flowOf(statuses.map(NetworkStatus::toSimple).toSet())

        every { networksStatusesStore.get(params.userWalletId) } returns networksStatusesFlow
        every { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet

        val actual = producer.produce()

        // check after producer.produce()
        verify { networksStatusesStore.get(params.userWalletId) }

        val values = getEmittedValues(flow = actual)

        // Check after flow was observed by subscriber (getEmittedValues).
        // Otherwise, userWalletsStore.getSyncOrNull is not called.
        verify { userWalletsStore.getSyncOrNull(params.userWalletId) }

        Truth.assertThat(values.size).isEqualTo(1)
        Truth.assertThat(values.first()).isEqualTo(statuses)
    }

    @Test
    fun `test that flow is updated if statuses are updated`() = runTest {
        val networksStatusesFlow = MutableSharedFlow<Set<SimpleNetworkStatus>>(replay = 2)

        every { networksStatusesStore.get(params.userWalletId) } returns networksStatusesFlow
        every { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet

        val actual = producer.produce()

        // check after producer.produce()
        verify { networksStatusesStore.get(params.userWalletId) }

        // first emit
        val statuses = setOf(
            MockNetworkStatusFactory.createUnreachable(ethNetwork),
            MockNetworkStatusFactory.createUnreachable(cardanoNetwork),
        )

        networksStatusesFlow.emit(statuses.map(NetworkStatus::toSimple).toSet())

        val values1 = getEmittedValues(flow = actual)

        // Check after flow was observed by subscriber (getEmittedValues).
        // Otherwise, userWalletsStore.getSyncOrNull is not called.
        verify { userWalletsStore.getSyncOrNull(params.userWalletId) }

        Truth.assertThat(values1.size).isEqualTo(1)
        Truth.assertThat(values1.first()).isEqualTo(statuses)

        // second emit
        val updatedStatuses = setOf(
            MockNetworkStatusFactory.createVerified(ethNetwork),
            MockNetworkStatusFactory.createVerified(cardanoNetwork),
        )

        networksStatusesFlow.emit(updatedStatuses.map(NetworkStatus::toSimple).toSet())

        val values2 = getEmittedValues(flow = actual)

        // Check after flow was observed by subscriber (getEmittedValues).
        // Otherwise, userWalletsStore.getSyncOrNull is not called.
        verify { userWalletsStore.getSyncOrNull(params.userWalletId) }

        val expected = listOf(statuses, updatedStatuses)
        Truth.assertThat(values2.size).isEqualTo(2)
        Truth.assertThat(values2).isEqualTo(expected)
    }

    @Test
    fun `test that flow is filtered the same status`() = runTest {
        val networksStatusesFlow = MutableSharedFlow<Set<SimpleNetworkStatus>>(replay = 2)

        every { networksStatusesStore.get(params.userWalletId) } returns networksStatusesFlow
        every { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet

        val actual = producer.produce()

        // check after producer.produce()
        verify { networksStatusesStore.get(params.userWalletId) }

        // first emit
        val statuses = setOf(
            MockNetworkStatusFactory.createUnreachable(ethNetwork),
            MockNetworkStatusFactory.createUnreachable(cardanoNetwork),
        )

        networksStatusesFlow.emit(statuses.map(NetworkStatus::toSimple).toSet())

        val values1 = getEmittedValues(flow = actual)

        // Check after flow was observed by subscriber (getEmittedValues).
        // Otherwise, userWalletsStore.getSyncOrNull is not called.
        verify { userWalletsStore.getSyncOrNull(params.userWalletId) }

        Truth.assertThat(values1.size).isEqualTo(1)
        Truth.assertThat(values1.first()).isEqualTo(statuses)

        // second emit
        networksStatusesFlow.emit(statuses.map(NetworkStatus::toSimple).toSet())

        val values2 = getEmittedValues(flow = actual)

        // Check after flow was observed by subscriber (getEmittedValues).
        // Otherwise, userWalletsStore.getSyncOrNull is not called.
        verify { userWalletsStore.getSyncOrNull(params.userWalletId) }

        Truth.assertThat(values2.size).isEqualTo(1)
        Truth.assertThat(values2.first()).isEqualTo(statuses)
    }

    @Test
    fun `test if flow throws exception`() = runTest {
        val exception = IllegalStateException()
        val statuses = setOf(
            MockNetworkStatusFactory.createUnreachable(ethNetwork),
            MockNetworkStatusFactory.createUnreachable(cardanoNetwork),
        )

        val innerFlow = MutableStateFlow(value = false)
        val networksStatusesFlow = flow {
            if (innerFlow.value) {
                emit(statuses.map(NetworkStatus::toSimple).toSet())
            } else {
                throw exception
            }
        }
            .buffer(capacity = 5)

        every { networksStatusesStore.get(params.userWalletId) } returns networksStatusesFlow
        every { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet

        val actual = producer.produceWithFallback()

        // check after producer.produce()
        verify { networksStatusesStore.get(params.userWalletId) }

        val values1 = getEmittedValues(flow = actual)

        // Check after flow was observed by subscriber (getEmittedValues).
        // Otherwise, userWalletsStore.getSyncOrNull is not called.
        verify(inverse = true) { userWalletsStore.getSyncOrNull(params.userWalletId) }

        Truth.assertThat(values1.size).isEqualTo(1)
        Truth.assertThat(values1).isEqualTo(listOf(emptySet<NetworkStatus>()))

        innerFlow.emit(value = true)

        val values2 = getEmittedValues(flow = actual)

        // Check after flow was observed by subscriber (getEmittedValues).
        // Otherwise, userWalletsStore.getSyncOrNull is not called.
        verify { userWalletsStore.getSyncOrNull(params.userWalletId) }

        Truth.assertThat(values2.size).isEqualTo(1)
        Truth.assertThat(values2).isEqualTo(listOf(statuses))
    }

    @Test
    fun `test that flow is empty`() = runTest {
        every { networksStatusesStore.get(params.userWalletId) } returns emptyFlow()
        every { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet

        val actual = producer.produce()

        // check after producer.produce()
        verify { networksStatusesStore.get(params.userWalletId) }

        val values = getEmittedValues(flow = actual)

        verify(inverse = true) { userWalletsStore.getSyncOrNull(params.userWalletId) }

        Truth.assertThat(values.size).isEqualTo(1)
        Truth.assertThat(values).isEqualTo(listOf(emptySet<NetworkStatus>()))
    }

    @Test
    fun `test if network is null`() = runTest {
        val statuses = setOf(
            MockNetworkStatusFactory.createVerified(ethNetwork),
            MockNetworkStatusFactory.createVerified(cardanoNetwork),
        )

        val networksStatusesFlow = flowOf(statuses.map(NetworkStatus::toSimple).toSet())

        every { networksStatusesStore.get(params.userWalletId) } returns networksStatusesFlow
        every { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet
        every { excludedBlockchains.contains(any()) } returns true // cause network is null

        val actual = producer.produce()

        // check after producer.produce()
        verify { networksStatusesStore.get(params.userWalletId) }

        val values = getEmittedValues(flow = actual)

        // Check after flow was observed by subscriber (getEmittedValues).
        // Otherwise, userWalletsStore.getSyncOrNull is not called.
        verify { userWalletsStore.getSyncOrNull(params.userWalletId) }

        Truth.assertThat(values.size).isEqualTo(1)
        Truth.assertThat(values).isEqualTo(listOf(emptySet<NetworkStatus>()))
    }

    private companion object {

        val scanResponse = MockScanResponseFactory.create(
            cardConfig = GenericCardConfig(2),
            derivedKeys = emptyMap(),
        )

        val userWallet = MockUserWalletFactory.create(scanResponse = scanResponse)

        val ethNetwork = MockCryptoCurrencyFactory(scanResponse = scanResponse).ethereum.network.copy(
            canHandleTokens = true,
        )

        val cardanoNetwork = MockCryptoCurrencyFactory(scanResponse = scanResponse).cardano.network
    }
}