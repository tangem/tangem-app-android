package com.tangem.data.staking.single

import com.google.common.truth.Truth
import com.tangem.common.test.data.staking.MockYieldBalanceWrapperDTOFactory
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.data.staking.toDomain
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.multi.MultiStakingBalanceProducer
import com.tangem.domain.staking.multi.MultiStakingBalanceSupplier
import com.tangem.domain.staking.single.SingleStakingBalanceProducer
import app.cash.turbine.test
import com.tangem.test.core.TestFlowProducerTools
import com.tangem.test.core.getEmittedValues
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultSingleStakingBalanceProducerTest {

    private val params = SingleStakingBalanceProducer.Params(
        userWalletId = UserWalletId(stringValue = "011"),
        stakingId = tonId,
    )

    private val multiNetworkStatusSupplier = mockk<MultiStakingBalanceSupplier>()
    private val analyticsExceptionHandler = mockk<AnalyticsExceptionHandler>(relaxUnitFun = true)
    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val flowProducerTools: FlowProducerTools = mockk()

    private val producer = DefaultSingleStakingBalanceProducer(
        params = params,
        multiStakingBalanceSupplier = multiNetworkStatusSupplier,
        analyticsExceptionHandler = analyticsExceptionHandler,
        dispatchers = dispatchers,
        flowProducerTools = flowProducerTools,
    )

    private fun TestScope.createProducer(): DefaultSingleStakingBalanceProducer {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        return DefaultSingleStakingBalanceProducer(
            params = params,
            multiStakingBalanceSupplier = multiNetworkStatusSupplier,
            analyticsExceptionHandler = analyticsExceptionHandler,
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

    @BeforeEach
    fun resetMocks() {
        clearMocks(multiNetworkStatusSupplier, analyticsExceptionHandler)
    }

    @Test
    fun `flow is mapped for data from params`() = runTest {
        // Arrange
        val balance = MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId).toDomain()

        val multiFlow = flowOf(
            setOf(
                balance,
                MockYieldBalanceWrapperDTOFactory.createWithBalance(solanaId).toDomain(),
            ),
        )

        val multiParams = MultiStakingBalanceProducer.Params(userWalletId = params.userWalletId)
        every { multiNetworkStatusSupplier(multiParams) } returns multiFlow

        // Act
        val actual = getEmittedValues(flow = producer.produce())

        Truth.assertThat(actual).hasSize(1)
        Truth.assertThat(actual).containsExactly(balance)

        verify(exactly = 1) { multiNetworkStatusSupplier(multiParams) }
    }

    @Test
    fun `flow is updated if staking balance is updated`() = runTest {
        // Arrange
        val multiFlow = MutableSharedFlow<Set<StakingBalance>>(replay = 2, extraBufferCapacity = 1)

        val multiParams = MultiStakingBalanceProducer.Params(userWalletId = params.userWalletId)
        every { multiNetworkStatusSupplier(multiParams) } returns multiFlow

        val producerFlow = createProducer().produceWithFallback()

        producerFlow.test {
            val balance = MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId).toDomain()
            multiFlow.emit(value = setOf(balance))
            Truth.assertThat(awaitItem()).isEqualTo(balance)

            val updatedBalance = StakingBalance.Error(stakingId = tonId)
            multiFlow.emit(value = setOf(updatedBalance))
            Truth.assertThat(awaitItem()).isEqualTo(updatedBalance)

            cancelAndIgnoreRemainingEvents()
        }

        verify(exactly = 1) { multiNetworkStatusSupplier(multiParams) }
    }

    @Test
    fun `flow is filtered the same status`() = runTest {
        // Arrange
        val multiFlow = MutableSharedFlow<Set<StakingBalance>>(replay = 2, extraBufferCapacity = 1)

        val multiParams = MultiStakingBalanceProducer.Params(userWalletId = params.userWalletId)
        every { multiNetworkStatusSupplier(multiParams) } returns multiFlow

        val producerFlow = createProducer().produceWithFallback()

        producerFlow.test {
            val balance = MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId).toDomain()
            multiFlow.emit(value = setOf(balance))
            Truth.assertThat(awaitItem()).isEqualTo(balance)

            // same balance again -> filtered out by distinctUntilChanged
            multiFlow.emit(value = setOf(balance))
            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }

        verify(exactly = 1) { multiNetworkStatusSupplier(multiParams) }
    }

    @Test
    fun `flow throws exception`() = runTest {
        // Arrange
        val exception = IllegalStateException()

        val balance = MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId).toDomain()

        val innerFlow = MutableStateFlow(value = false)
        val multiFlow = flow {
            if (innerFlow.value) {
                emit(setOf(balance))
            } else {
                throw exception
            }
        }
            .buffer(capacity = 5)

        val multiParams = MultiStakingBalanceProducer.Params(userWalletId = params.userWalletId)
        every { multiNetworkStatusSupplier(multiParams) } returns multiFlow

        val producerFlow = createProducer().produceWithFallback()

        producerFlow.test {
            // first collection throws -> retryWhen emits the fallback, then waits 2s
            val fallbackStatus = StakingBalance.Error(stakingId = tonId.copy(address = "0x1"))
            Truth.assertThat(awaitItem()).isEqualTo(fallbackStatus)

            // recover the upstream and let the retry fire
            innerFlow.value = true
            advanceTimeBy(delayTimeMillis = 2001)
            runCurrent()

            Truth.assertThat(awaitItem()).isEqualTo(balance)

            cancelAndIgnoreRemainingEvents()
        }

        verify(exactly = 1) { multiNetworkStatusSupplier(multiParams) }
    }

    @Test
    fun `flow doesn't contain network from params`() = runTest {
        // Arrange
        val balance = MockYieldBalanceWrapperDTOFactory.createWithBalance(solanaId).toDomain()

        val multiFlow = flowOf(setOf(balance))

        val multiParams = MultiStakingBalanceProducer.Params(userWalletId = params.userWalletId)
        every { multiNetworkStatusSupplier(multiParams) } returns multiFlow

        val producerFlow = producer.produce()

        // Act
        val actual = getEmittedValues(flow = producerFlow)

        // Assert
        Truth.assertThat(actual).containsExactly(StakingBalance.Error(stakingId = tonId))

        verify(exactly = 1) { multiNetworkStatusSupplier(multiParams) }
    }

    private companion object {

        val tonId = MockYieldBalanceWrapperDTOFactory.defaultStakingId
        val solanaId = StakingID(
            integrationId = "solana-sol-native-multivalidator-staking",
            address = "0x1",
        )
    }
}