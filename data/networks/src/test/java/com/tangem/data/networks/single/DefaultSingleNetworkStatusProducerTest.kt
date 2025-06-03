package com.tangem.data.networks.single

import com.google.common.truth.Truth
import com.tangem.common.test.domain.network.MockNetworkStatusFactory
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.utils.getEmittedValues
import com.tangem.domain.networks.multi.MultiNetworkStatusProducer
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
internal class DefaultSingleNetworkStatusProducerTest {

    private val params = SingleNetworkStatusProducer.Params(
        userWalletId = UserWalletId(stringValue = "011"),
        network = ethereum,
    )

    private val multiNetworkStatusSupplier = mockk<MultiNetworkStatusSupplier>()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val producer = DefaultSingleNetworkStatusProducer(
        params = params,
        multiNetworkStatusSupplier = multiNetworkStatusSupplier,
        dispatchers = dispatchers,
    )

    @Test
    fun `test that flow is mapped for network from params`() = runTest {
        val status = MockNetworkStatusFactory.createMissedDerivation(params.network)
        val expected = flowOf(
            setOf(
                status,
                MockNetworkStatusFactory.createMissedDerivation(cardano),
            ),
        )

        val multiParams = MultiNetworkStatusProducer.Params(userWalletId = params.userWalletId)
        every { multiNetworkStatusSupplier(multiParams) } returns expected

        val actual = producer.produce()

        verify { multiNetworkStatusSupplier(multiParams) }

        val values = getEmittedValues(flow = actual)

        Truth.assertThat(values.size).isEqualTo(1)
        Truth.assertThat(values).isEqualTo(listOf(status))
    }

    @Test
    fun `test that flow is updated if network status is updated`() = runTest {
        val expected = MutableSharedFlow<Set<NetworkStatus>>(replay = 2, extraBufferCapacity = 1)

        val multiParams = MultiNetworkStatusProducer.Params(userWalletId = params.userWalletId)
        every { multiNetworkStatusSupplier(multiParams) } returns expected

        val actual = producer.produceWithFallback()

        verify { multiNetworkStatusSupplier(multiParams) }

        // first emit
        val status = MockNetworkStatusFactory.createMissedDerivation(params.network)
        expected.emit(value = setOf(status))

        val values1 = getEmittedValues(flow = actual)

        Truth.assertThat(values1.size).isEqualTo(1)
        Truth.assertThat(values1).isEqualTo(listOf(status))

        // second emit
        val updatedStatus = status.copy(value = NetworkStatus.Unreachable(null))
        expected.emit(value = setOf(updatedStatus))

        val values2 = getEmittedValues(flow = actual)

        Truth.assertThat(values2.size).isEqualTo(2)
        Truth.assertThat(values2).isEqualTo(listOf(status, updatedStatus))
    }

    @Test
    fun `test that flow is filtered the same status`() = runTest {
        val expected = MutableSharedFlow<Set<NetworkStatus>>(replay = 2, extraBufferCapacity = 1)

        val multiParams = MultiNetworkStatusProducer.Params(userWalletId = params.userWalletId)
        every { multiNetworkStatusSupplier(multiParams) } returns expected

        val actual = producer.produceWithFallback()

        verify { multiNetworkStatusSupplier(multiParams) }

        // first emit
        val status = MockNetworkStatusFactory.createMissedDerivation(params.network)
        expected.emit(value = setOf(status))

        val values1 = getEmittedValues(flow = actual)

        Truth.assertThat(values1.size).isEqualTo(1)
        Truth.assertThat(values1).isEqualTo(listOf(status))

        // second emit
        expected.emit(value = setOf(status))

        val values2 = getEmittedValues(flow = actual)

        Truth.assertThat(values2.size).isEqualTo(1)
        Truth.assertThat(values2).isEqualTo(listOf(status))
    }

    @Test
    fun `test if flow throws exception`() = runTest {
        val exception = IllegalStateException()
        val status = MockNetworkStatusFactory.createMissedDerivation(params.network)

        val innerFlow = MutableStateFlow(value = false)
        val expected = flow {
            if (innerFlow.value) {
                emit(setOf(status))
            } else {
                throw exception
            }
        }
            .buffer(capacity = 5)

        val multiParams = MultiNetworkStatusProducer.Params(userWalletId = params.userWalletId)
        every { multiNetworkStatusSupplier(multiParams) } returns expected

        val actual = producer.produceWithFallback()

        verify { multiNetworkStatusSupplier(multiParams) }

        val values1 = getEmittedValues(flow = actual)

        Truth.assertThat(values1.size).isEqualTo(1)
        val fallbackStatus = MockNetworkStatusFactory.createUnreachable(params.network)
        Truth.assertThat(values1).isEqualTo(listOf(fallbackStatus))

        innerFlow.emit(value = true)

        val values2 = getEmittedValues(flow = actual)
        Truth.assertThat(values2.size).isEqualTo(1)
        Truth.assertThat(values2).isEqualTo(listOf(status))
    }

    @Test
    fun `test if flow doesn't contain network from params`() = runTest {
        val expected = flowOf(
            setOf(MockNetworkStatusFactory.createMissedDerivation(cardano)),
        )

        val multiParams = MultiNetworkStatusProducer.Params(userWalletId = params.userWalletId)
        every { multiNetworkStatusSupplier(multiParams) } returns expected

        val actual = producer.produce()

        verify { multiNetworkStatusSupplier(multiParams) }

        val values = getEmittedValues(flow = actual)

        Truth.assertThat(values.size).isEqualTo(0)
    }

    private companion object {

        val mocks = MockCryptoCurrencyFactory()

        val ethereum = mocks.ethereum.network
        val cardano = mocks.cardano.network
    }
}