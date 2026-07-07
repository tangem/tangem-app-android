package com.tangem.data.networks.single

import app.cash.turbine.test
import com.google.common.truth.Truth
import com.tangem.common.test.domain.network.MockNetworkStatusFactory
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.multi.MultiNetworkStatusProducer
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.test.core.TestFlowProducerTools
import com.tangem.test.core.getEmittedValues
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
[REDACTED_AUTHOR]
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultSingleNetworkStatusProducerTest {

    private val params = SingleNetworkStatusProducer.Params(
        userWalletId = UserWalletId(stringValue = "011"),
        network = ethereum,
    )

    private val multiNetworkStatusSupplier = mockk<MultiNetworkStatusSupplier>()

    private fun TestScope.createProducer(): DefaultSingleNetworkStatusProducer {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        return DefaultSingleNetworkStatusProducer(
            params = params,
            multiNetworkStatusSupplier = multiNetworkStatusSupplier,
            dispatchers = TestingCoroutineDispatcherProvider(
                main = testDispatcher,
                mainImmediate = testDispatcher,
                io = testDispatcher,
                default = testDispatcher,
                single = testDispatcher,
            ),
            flowProducerTools = TestFlowProducerTools(scope = backgroundScope, dispatcher = testDispatcher),
        )
    }

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

        val actual = createProducer().produce()

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

        val actual = createProducer().produceWithFallback()

        verify { multiNetworkStatusSupplier(multiParams) }

        actual.test {
            val status = MockNetworkStatusFactory.createMissedDerivation(params.network)
            expected.emit(value = setOf(status))
            Truth.assertThat(awaitItem()).isEqualTo(status)

            val updatedStatus = status.copy(value = NetworkStatus.Unreachable(null))
            expected.emit(value = setOf(updatedStatus))
            Truth.assertThat(awaitItem()).isEqualTo(updatedStatus)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that flow is filtered the same status`() = runTest {
        val expected = MutableSharedFlow<Set<NetworkStatus>>(replay = 2, extraBufferCapacity = 1)

        val multiParams = MultiNetworkStatusProducer.Params(userWalletId = params.userWalletId)
        every { multiNetworkStatusSupplier(multiParams) } returns expected

        val actual = createProducer().produceWithFallback()

        verify { multiNetworkStatusSupplier(multiParams) }

        actual.test {
            val status = MockNetworkStatusFactory.createMissedDerivation(params.network)
            expected.emit(value = setOf(status))
            Truth.assertThat(awaitItem()).isEqualTo(status)

            // same status again -> filtered out by distinctUntilChanged
            expected.emit(value = setOf(status))
            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
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

        val actual = createProducer().produceWithFallback()

        verify { multiNetworkStatusSupplier(multiParams) }

        actual.test {
            // first collection throws -> retryWhen emits the fallback, then waits 2s before retrying
            val fallbackStatus = MockNetworkStatusFactory.createUnreachable(params.network)
            Truth.assertThat(awaitItem()).isEqualTo(fallbackStatus)

            // recover the upstream and let the retry fire
            innerFlow.value = true
            advanceTimeBy(delayTimeMillis = 2001)
            runCurrent()

            Truth.assertThat(awaitItem()).isEqualTo(status)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test if flow doesn't contain network from params`() = runTest {
        val expected = flowOf(
            setOf(MockNetworkStatusFactory.createMissedDerivation(cardano)),
        )

        val multiParams = MultiNetworkStatusProducer.Params(userWalletId = params.userWalletId)
        every { multiNetworkStatusSupplier(multiParams) } returns expected

        val actual = createProducer().produce()

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