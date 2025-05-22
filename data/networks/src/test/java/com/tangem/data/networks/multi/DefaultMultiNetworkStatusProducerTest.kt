package com.tangem.data.networks.multi

import com.google.common.truth.Truth
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.common.test.domain.network.MockNetworkStatusFactory
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.test.utils.getEmittedValues
import com.tangem.data.common.network.NetworkFactory
import com.tangem.data.networks.models.SimpleNetworkStatus
import com.tangem.data.networks.store.NetworksStatusesStore
import com.tangem.data.networks.toSimple
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.configs.GenericCardConfig
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.networks.multi.MultiNetworkStatusProducer
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultMultiNetworkStatusProducerTest {

    private val params = MultiNetworkStatusProducer.Params(userWalletId = userWallet.walletId)

    private val networksStatusesStore = mockk<NetworksStatusesStore>()
    private val userWalletsStore = mockk<UserWalletsStore>()
    private val networkFactory = mockk<NetworkFactory>()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val producer = DefaultMultiNetworkStatusProducer(
        params = params,
        networksStatusesStore = networksStatusesStore,
        userWalletsStore = userWalletsStore,
        networkFactory = networkFactory,
        dispatchers = dispatchers,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(networksStatusesStore, userWalletsStore, networkFactory)
    }

    @Test
    fun `flow is mapped for user wallet id from params`() = runTest {
        // Assert
        val statuses = setOf(
            MockNetworkStatusFactory.createVerified(ethNetwork),
            MockNetworkStatusFactory.createVerified(cardanoNetwork),
        )

        val simpleStatuses = statuses.map(NetworkStatus::toSimple).toSet()

        val networksStatusesFlow = flowOf(simpleStatuses)

        every { networksStatusesStore.get(params.userWalletId) } returns networksStatusesFlow
        every { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet

        every {
            networkFactory.create(
                networkId = simpleStatuses.first().id,
                derivationPath = simpleStatuses.first().id.derivationPath,
                scanResponse = userWallet.scanResponse,
            )
        } returns statuses.first().network

        every {
            networkFactory.create(
                networkId = simpleStatuses.last().id,
                derivationPath = simpleStatuses.last().id.derivationPath,
                scanResponse = userWallet.scanResponse,
            )
        } returns statuses.last().network

        // Act
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        val expected = statuses

        Truth.assertThat(actual.size).isEqualTo(1)
        Truth.assertThat(actual.first()).isEqualTo(expected)

        verifyOrder {
            networksStatusesStore.get(params.userWalletId)
            userWalletsStore.getSyncOrNull(params.userWalletId)
            networkFactory.create(
                networkId = simpleStatuses.first().id,
                derivationPath = simpleStatuses.first().id.derivationPath,
                scanResponse = userWallet.scanResponse,
            )
            networkFactory.create(
                networkId = simpleStatuses.last().id,
                derivationPath = simpleStatuses.last().id.derivationPath,
                scanResponse = userWallet.scanResponse,
            )
        }
    }

    @Test
    fun `flow will updated if statuses are updated`() = runTest {
        // Arrange
        val networksStatusesFlow = MutableSharedFlow<Set<SimpleNetworkStatus>>(replay = 2)

        val statuses = setOf(
            MockNetworkStatusFactory.createUnreachable(ethNetwork),
            MockNetworkStatusFactory.createUnreachable(cardanoNetwork),
        )

        val simpleStatuses = statuses.map(NetworkStatus::toSimple).toSet()

        val updatedStatuses = setOf(
            MockNetworkStatusFactory.createVerified(ethNetwork),
            MockNetworkStatusFactory.createVerified(cardanoNetwork),
        )

        val updatedSimpleStatuses = updatedStatuses.map(NetworkStatus::toSimple).toSet()

        // region every
        every { networksStatusesStore.get(params.userWalletId) } returns networksStatusesFlow
        every { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet
        every {
            networkFactory.create(
                networkId = simpleStatuses.first().id,
                derivationPath = simpleStatuses.first().id.derivationPath,
                scanResponse = userWallet.scanResponse,
            )
        } returns statuses.first().network

        every {
            networkFactory.create(
                networkId = simpleStatuses.last().id,
                derivationPath = simpleStatuses.last().id.derivationPath,
                scanResponse = userWallet.scanResponse,
            )
        } returns statuses.last().network

        every {
            networkFactory.create(
                networkId = updatedSimpleStatuses.first().id,
                derivationPath = updatedSimpleStatuses.first().id.derivationPath,
                scanResponse = userWallet.scanResponse,
            )
        } returns updatedStatuses.first().network

        every {
            networkFactory.create(
                networkId = updatedSimpleStatuses.last().id,
                derivationPath = updatedSimpleStatuses.last().id.derivationPath,
                scanResponse = userWallet.scanResponse,
            )
        } returns updatedStatuses.last().network
        // endregion

        val producerFlow = producer.produce()

        // Act 1 (first emit)
        networksStatusesFlow.emit(simpleStatuses)

        val actual1 = getEmittedValues(flow = producerFlow)

        // Assert
        val expected1 = statuses

        Truth.assertThat(actual1.size).isEqualTo(1)
        Truth.assertThat(actual1.first()).isEqualTo(expected1)

        verifyOrder {
            userWalletsStore.getSyncOrNull(params.userWalletId)
            networkFactory.create(
                networkId = simpleStatuses.first().id,
                derivationPath = simpleStatuses.first().id.derivationPath,
                scanResponse = userWallet.scanResponse,
            )
            networkFactory.create(
                networkId = simpleStatuses.last().id,
                derivationPath = simpleStatuses.last().id.derivationPath,
                scanResponse = userWallet.scanResponse,
            )
        }

        // Act 2 (second emit)
        networksStatusesFlow.emit(updatedSimpleStatuses)

        val actual2 = getEmittedValues(flow = producerFlow)

        // Assert
        val expected2 = listOf(statuses, updatedStatuses)

        Truth.assertThat(actual2.size).isEqualTo(2)
        Truth.assertThat(actual2).isEqualTo(expected2)

        verifyOrder {
            userWalletsStore.getSyncOrNull(params.userWalletId)
            networkFactory.create(
                networkId = updatedSimpleStatuses.first().id,
                derivationPath = updatedSimpleStatuses.first().id.derivationPath,
                scanResponse = userWallet.scanResponse,
            )
            networkFactory.create(
                networkId = updatedSimpleStatuses.last().id,
                derivationPath = updatedSimpleStatuses.last().id.derivationPath,
                scanResponse = userWallet.scanResponse,
            )
        }
    }

    @Test
    fun `flow is filtered the same status`() = runTest {
        // Arrange
        val networksStatusesFlow = MutableSharedFlow<Set<SimpleNetworkStatus>>(replay = 2)

        val statuses = setOf(
            MockNetworkStatusFactory.createUnreachable(ethNetwork),
            MockNetworkStatusFactory.createUnreachable(cardanoNetwork),
        )

        val simpleStatuses = statuses.map(NetworkStatus::toSimple).toSet()

        // region every
        every { networksStatusesStore.get(params.userWalletId) } returns networksStatusesFlow
        every { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet

        every {
            networkFactory.create(
                networkId = simpleStatuses.first().id,
                derivationPath = simpleStatuses.first().id.derivationPath,
                scanResponse = userWallet.scanResponse,
            )
        } returns statuses.first().network

        every {
            networkFactory.create(
                networkId = simpleStatuses.last().id,
                derivationPath = simpleStatuses.last().id.derivationPath,
                scanResponse = userWallet.scanResponse,
            )
        } returns statuses.last().network
        // endregion

        val producerFlow = producer.produce()

        // Act 1 (first emit)
        networksStatusesFlow.emit(simpleStatuses)

        val actual1 = getEmittedValues(flow = producerFlow)

        // Assert
        val expected1 = statuses

        Truth.assertThat(actual1.size).isEqualTo(1)
        Truth.assertThat(actual1.first()).isEqualTo(expected1)

        verifyOrder {
            userWalletsStore.getSyncOrNull(params.userWalletId)
            networkFactory.create(
                networkId = simpleStatuses.first().id,
                derivationPath = simpleStatuses.first().id.derivationPath,
                scanResponse = userWallet.scanResponse,
            )
            networkFactory.create(
                networkId = simpleStatuses.last().id,
                derivationPath = simpleStatuses.last().id.derivationPath,
                scanResponse = userWallet.scanResponse,
            )
        }

        // Act 2 (second emit)
        networksStatusesFlow.emit(simpleStatuses)

        val actual2 = getEmittedValues(flow = producerFlow)

        // Asset
        val expected2 = expected1
        Truth.assertThat(actual2.size).isEqualTo(1)
        Truth.assertThat(actual2.first()).isEqualTo(expected2)
    }

    @Test
    fun `flow throws exception`() = runTest {
        // Arrange
        val exception = IllegalStateException()

        val statuses = setOf(
            MockNetworkStatusFactory.createUnreachable(ethNetwork),
            MockNetworkStatusFactory.createUnreachable(cardanoNetwork),
        )

        val simpleStatuses = statuses.map(NetworkStatus::toSimple).toSet()

        val innerFlow = MutableStateFlow(value = false)
        val networksStatusesFlow = flow {
            if (innerFlow.value) {
                emit(simpleStatuses)
            } else {
                throw exception
            }
        }
            .buffer(capacity = 5)

        // region every
        every { networksStatusesStore.get(params.userWalletId) } returns networksStatusesFlow
        every { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet
        every {
            networkFactory.create(
                networkId = simpleStatuses.first().id,
                derivationPath = simpleStatuses.first().id.derivationPath,
                scanResponse = userWallet.scanResponse,
            )
        } returns statuses.first().network
        every {
            networkFactory.create(
                networkId = simpleStatuses.last().id,
                derivationPath = simpleStatuses.last().id.derivationPath,
                scanResponse = userWallet.scanResponse,
            )
        } returns statuses.last().network
        // endregion

        val producerFlow = producer.produceWithFallback()

        // Act 1 (fallback)
        val actual1 = getEmittedValues(flow = producerFlow)

        // Assert
        val expected1 = emptySet<NetworkStatus>()
        Truth.assertThat(actual1.size).isEqualTo(1)
        Truth.assertThat(actual1.first()).isEqualTo(expected1)

        verifyOrder(inverse = true) {
            userWalletsStore.getSyncOrNull(any())
            networkFactory.create(networkId = any(), derivationPath = any(), scanResponse = any())
        }

        // Act 2 (emit)
        innerFlow.emit(value = true)
        val actual2 = getEmittedValues(flow = producerFlow)

        // Assert
        val expected2 = statuses
        Truth.assertThat(actual2.size).isEqualTo(1)
        Truth.assertThat(actual2.first()).isEqualTo(expected2)

        verifyOrder {
            userWalletsStore.getSyncOrNull(params.userWalletId)
            networkFactory.create(
                networkId = simpleStatuses.first().id,
                derivationPath = simpleStatuses.first().id.derivationPath,
                scanResponse = userWallet.scanResponse,
            )
            networkFactory.create(
                networkId = simpleStatuses.last().id,
                derivationPath = simpleStatuses.last().id.derivationPath,
                scanResponse = userWallet.scanResponse,
            )
        }
    }

    @Test
    fun `flow is empty if store returns empty flow`() = runTest {
        // Arrange
        every { networksStatusesStore.get(params.userWalletId) } returns emptyFlow()

        // Act
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        val expected = emptySet<NetworkStatus>()
        Truth.assertThat(actual.size).isEqualTo(1)
        Truth.assertThat(actual.first()).isEqualTo(expected)

        verify { networksStatusesStore.get(params.userWalletId) }
        verify(inverse = true) { userWalletsStore.getSyncOrNull(params.userWalletId) }
    }

    @Test
    fun `flow returns empty list if networkFactory returns null`() = runTest {
        // Arrange
        val statuses = setOf(
            MockNetworkStatusFactory.createVerified(ethNetwork),
            MockNetworkStatusFactory.createVerified(cardanoNetwork),
        )

        val simpleStatuses = statuses.map(NetworkStatus::toSimple).toSet()

        val networksStatusesFlow = flowOf(simpleStatuses)

        every { networksStatusesStore.get(params.userWalletId) } returns networksStatusesFlow
        every { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet
        coEvery { networkFactory.create(networkId = any(), any(), any()) } returns null

        // Act
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        val expected = emptySet<NetworkStatus>()
        Truth.assertThat(actual.size).isEqualTo(1)
        Truth.assertThat(actual.first()).isEqualTo(expected)

        verifyOrder {
            networksStatusesStore.get(params.userWalletId)
            userWalletsStore.getSyncOrNull(params.userWalletId)
            networkFactory.create(
                networkId = simpleStatuses.first().id,
                derivationPath = simpleStatuses.first().id.derivationPath,
                scanResponse = userWallet.scanResponse,
            )
            networkFactory.create(
                networkId = simpleStatuses.last().id,
                derivationPath = simpleStatuses.last().id.derivationPath,
                scanResponse = userWallet.scanResponse,
            )
        }
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