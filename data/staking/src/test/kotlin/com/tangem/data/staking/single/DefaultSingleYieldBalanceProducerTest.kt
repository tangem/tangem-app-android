package com.tangem.data.staking.single

import com.google.common.truth.Truth
import com.tangem.common.test.data.staking.MockYieldBalanceWrapperDTOFactory
import com.tangem.common.test.utils.getEmittedValues
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.data.staking.toDomain
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingID
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.multi.MultiYieldBalanceProducer
import com.tangem.domain.staking.multi.MultiYieldBalanceSupplier
import com.tangem.domain.staking.single.SingleYieldBalanceProducer
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultSingleYieldBalanceProducerTest {

    private val params = SingleYieldBalanceProducer.Params(
        userWalletId = UserWalletId(stringValue = "011"),
        stakingId = tonId,
    )

    private val multiNetworkStatusSupplier = mockk<MultiYieldBalanceSupplier>()
    private val analyticsExceptionHandler = mockk<AnalyticsExceptionHandler>(relaxUnitFun = true)
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val producer = DefaultSingleYieldBalanceProducer(
        params = params,
        multiYieldBalanceSupplier = multiNetworkStatusSupplier,
        analyticsExceptionHandler = analyticsExceptionHandler,
        dispatchers = dispatchers,
    )

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

        val multiParams = MultiYieldBalanceProducer.Params(userWalletId = params.userWalletId)
        every { multiNetworkStatusSupplier(multiParams) } returns multiFlow

        // Act
        val actual = getEmittedValues(flow = producer.produce())

        Truth.assertThat(actual).hasSize(1)
        Truth.assertThat(actual).containsExactly(balance)

        verify(exactly = 1) { multiNetworkStatusSupplier(multiParams) }
    }

    @Test
    fun `flow is updated if yield balance is updated`() = runTest {
        // Arrange
        val multiFlow = MutableSharedFlow<Set<YieldBalance>>(replay = 2, extraBufferCapacity = 1)

        val multiParams = MultiYieldBalanceProducer.Params(userWalletId = params.userWalletId)
        every { multiNetworkStatusSupplier(multiParams) } returns multiFlow

        val producerFlow = producer.produceWithFallback()

        val balance = MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId).toDomain()
        val updatedBalance = YieldBalance.Error(stakingId = tonId)

        // Act (first emit)
        multiFlow.emit(value = setOf(balance))
        val actual1 = getEmittedValues(flow = producerFlow)

        // Assert (first emit)
        Truth.assertThat(actual1).hasSize(1)
        Truth.assertThat(actual1).containsExactly(balance)

        // Act (second emit)
        multiFlow.emit(value = setOf(updatedBalance))
        val actual2 = getEmittedValues(flow = producerFlow)

        // Assert (second emit)
        Truth.assertThat(actual2).hasSize(2)
        Truth.assertThat(actual2).containsExactly(balance, updatedBalance)

        verify(exactly = 1) { multiNetworkStatusSupplier(multiParams) }
    }

    @Test
    fun `flow is filtered the same status`() = runTest {
        // Arrange
        val multiFlow = MutableSharedFlow<Set<YieldBalance>>(replay = 2, extraBufferCapacity = 1)

        val multiParams = MultiYieldBalanceProducer.Params(userWalletId = params.userWalletId)
        every { multiNetworkStatusSupplier(multiParams) } returns multiFlow

        val producerFlow = producer.produceWithFallback()

        val balance = MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId).toDomain()

        // Act (first emit)
        multiFlow.emit(value = setOf(balance))
        val actual1 = getEmittedValues(flow = producerFlow)

        // Assert (first emit)
        Truth.assertThat(actual1).hasSize(1)
        Truth.assertThat(actual1).containsExactly(balance)

        // Act (second emit)
        multiFlow.emit(value = setOf(balance))
        val actual2 = getEmittedValues(flow = producerFlow)

        // Assert (second emit)
        Truth.assertThat(actual2).hasSize(1)
        Truth.assertThat(actual2).containsExactly(balance)

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

        val multiParams = MultiYieldBalanceProducer.Params(userWalletId = params.userWalletId)
        every { multiNetworkStatusSupplier(multiParams) } returns multiFlow

        val producerFlow = producer.produceWithFallback()

        // Act (first emit)
        val actual1 = getEmittedValues(flow = producerFlow)

        // Assert (first emit)
        val fallbackStatus = YieldBalance.Error(stakingId = tonId.copy(address = "0x1"))

        Truth.assertThat(actual1).hasSize(1)
        Truth.assertThat(actual1).containsExactly(fallbackStatus)

        // Act (second emit)
        innerFlow.emit(value = true)
        val actual2 = getEmittedValues(flow = producerFlow)

        Truth.assertThat(actual2).hasSize(1)
        Truth.assertThat(actual2).containsExactly(balance)

        verify(exactly = 1) { multiNetworkStatusSupplier(multiParams) }
    }

    @Test
    fun `flow doesn't contain network from params`() = runTest {
        // Arrange
        val balance = MockYieldBalanceWrapperDTOFactory.createWithBalance(solanaId).toDomain()

        val multiFlow = flowOf(setOf(balance))

        val multiParams = MultiYieldBalanceProducer.Params(userWalletId = params.userWalletId)
        every { multiNetworkStatusSupplier(multiParams) } returns multiFlow

        val producerFlow = producer.produce()

        // Act
        val actual = getEmittedValues(flow = producerFlow)

        // Assert
        Truth.assertThat(actual).isEmpty()

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