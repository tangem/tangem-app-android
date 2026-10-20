package com.tangem.data.staking.multi

import com.google.common.truth.Truth
import com.tangem.common.test.data.staking.MockYieldBalanceWrapperDTOFactory
import com.tangem.common.test.data.staking.MockP2PEthPoolAccountResponseFactory
import com.tangem.data.staking.store.P2PEthPoolBalancesStore
import com.tangem.data.staking.store.StakeKitBalancesStore
import com.tangem.data.staking.toDomain
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.staking.*
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.multi.MultiStakingBalanceProducer
import app.cash.turbine.test
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
internal class DefaultMultiStakingBalanceProducerTest {

    private val params = MultiStakingBalanceProducer.Params(userWalletId = UserWalletId("011"))

    private val stakeKitBalancesStore = mockk<StakeKitBalancesStore>()
    private val p2PEthPoolBalancesStore = mockk<P2PEthPoolBalancesStore>()
    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val flowProducerTools: FlowProducerTools = mockk()

    private val producer = DefaultMultiStakingBalanceProducer(
        params = params,
        stakeKitBalancesStore = stakeKitBalancesStore,
        p2PEthPoolBalancesStore = p2PEthPoolBalancesStore,
        flowProducerTools = flowProducerTools,
        dispatchers = dispatchers,
    )

    // Producer wired with a real test FlowProducerTools (shareIn + retry + distinctUntilChanged)
    // for produceWithFallback() cases.
    private fun TestScope.createProducer(): DefaultMultiStakingBalanceProducer {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        return DefaultMultiStakingBalanceProducer(
            params = params,
            stakeKitBalancesStore = stakeKitBalancesStore,
            p2PEthPoolBalancesStore = p2PEthPoolBalancesStore,
            flowProducerTools = TestFlowProducerTools(scope = backgroundScope, dispatcher = testDispatcher),
            dispatchers = TestingCoroutineDispatcherProvider(
                main = testDispatcher,
                mainImmediate = testDispatcher,
                io = testDispatcher,
                default = testDispatcher,
                single = testDispatcher,
            ),
        )
    }

    @Test
    fun `GIVEN stakeKit balances WHEN produce THEN emits them for params wallet`() = runTest {
        // Arrange
        val balances = setOf(
            MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId).toDomain(),
            MockYieldBalanceWrapperDTOFactory.createWithBalance(solanaId).toDomain(),
        )

        val networksStatusesFlow = flowOf(balances)

        every { stakeKitBalancesStore.get(params.userWalletId) } returns networksStatusesFlow
        every { p2PEthPoolBalancesStore.get(params.userWalletId) } returns flowOf(emptySet())

        // Act
        val actual = producer.produce()
        val values = getEmittedValues(flow = actual)

        // Assert
        verify { stakeKitBalancesStore.get(params.userWalletId) }
        verify { p2PEthPoolBalancesStore.get(params.userWalletId) }

        Truth.assertThat(values.size).isEqualTo(1)
        Truth.assertThat(values.first()).isEqualTo(balances)
    }

    @Test
    fun `GIVEN stakeKit balances updated WHEN produce THEN emits each update`() = runTest {
        // Arrange
        val networksStatusesFlow = MutableSharedFlow<Set<StakingBalance>>(replay = 2)

        every { stakeKitBalancesStore.get(params.userWalletId) } returns networksStatusesFlow
        every { p2PEthPoolBalancesStore.get(params.userWalletId) } returns flowOf(emptySet())

        // Act
        val actual = producer.produce()

        // Assert
        verify { stakeKitBalancesStore.get(params.userWalletId) }
        verify { p2PEthPoolBalancesStore.get(params.userWalletId) }

        // Act & Assert
        val balances = setOf(
            MockYieldBalanceWrapperDTOFactory.createWithEmptyBalance(tonId).toDomain(),
            MockYieldBalanceWrapperDTOFactory.createWithEmptyBalance(solanaId).toDomain(),
        )

        networksStatusesFlow.emit(balances)

        val values1 = getEmittedValues(flow = actual)

        Truth.assertThat(values1.size).isEqualTo(1)
        Truth.assertThat(values1.first()).isEqualTo(balances)

        // Act & Assert
        val updatedWrappers = setOf(
            MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId).toDomain(),
            MockYieldBalanceWrapperDTOFactory.createWithBalance(solanaId).toDomain(),
        )

        networksStatusesFlow.emit(updatedWrappers)

        val values2 = getEmittedValues(flow = actual)

        val expected = listOf(balances, updatedWrappers)
        Truth.assertThat(values2.size).isEqualTo(2)
        Truth.assertThat(values2).isEqualTo(expected)
    }

    @Test
    fun `GIVEN same balances emitted twice WHEN produceWithFallback THEN second emission filtered`() = runTest {
        // Arrange
        val networksStatusesFlow = MutableSharedFlow<Set<StakingBalance>>(replay = 2)

        every { stakeKitBalancesStore.get(params.userWalletId) } returns networksStatusesFlow
        every { p2PEthPoolBalancesStore.get(params.userWalletId) } returns flowOf(emptySet())

        // Act
        val actual = createProducer().produceWithFallback()

        // Assert
        verify { stakeKitBalancesStore.get(params.userWalletId) }
        verify { p2PEthPoolBalancesStore.get(params.userWalletId) }

        actual.test {
            val wrappers = setOf(
                MockYieldBalanceWrapperDTOFactory.createWithEmptyBalance(tonId).toDomain(),
                MockYieldBalanceWrapperDTOFactory.createWithEmptyBalance(solanaId).toDomain(),
            )

            networksStatusesFlow.emit(wrappers)
            Truth.assertThat(awaitItem()).isEqualTo(wrappers)

            // same balances again -> filtered out by distinctUntilChanged
            networksStatusesFlow.emit(wrappers)
            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN upstream throws WHEN produceWithFallback THEN emits empty fallback and recovers after retry`() = runTest {
        // Arrange
        val exception = IllegalStateException()
        val balances = setOf(
            MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId).toDomain(),
            MockYieldBalanceWrapperDTOFactory.createWithBalance(solanaId).toDomain(),
        )

        val innerFlow = MutableStateFlow(value = false)
        val networksStatusesFlow = flow {
            if (innerFlow.value) {
                emit(balances)
            } else {
                throw exception
            }
        }
            .buffer(capacity = 5)

        every { stakeKitBalancesStore.get(params.userWalletId) } returns networksStatusesFlow
        every { p2PEthPoolBalancesStore.get(params.userWalletId) } returns flowOf(emptySet())

        // Act
        val actual = createProducer().produceWithFallback()

        // Assert
        verify { stakeKitBalancesStore.get(params.userWalletId) }
        verify { p2PEthPoolBalancesStore.get(params.userWalletId) }

        actual.test {
            // first collection throws -> retryWhen emits the empty fallback, then waits 2s
            Truth.assertThat(awaitItem()).isEqualTo(emptySet<StakingBalance>())

            // recover the upstream and let the retry fire
            innerFlow.value = true
            advanceTimeBy(delayTimeMillis = 2001)
            runCurrent()

            Truth.assertThat(awaitItem()).isEqualTo(balances)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN both stores emit nothing WHEN produce THEN emits empty set`() = runTest {
        // Arrange
        every { stakeKitBalancesStore.get(params.userWalletId) } returns emptyFlow()
        every { p2PEthPoolBalancesStore.get(params.userWalletId) } returns emptyFlow()

        // Act
        val actual = producer.produce()
        val values = getEmittedValues(flow = actual)

        // Assert
        verify { stakeKitBalancesStore.get(params.userWalletId) }
        verify { p2PEthPoolBalancesStore.get(params.userWalletId) }

        Truth.assertThat(values.size).isEqualTo(1)
        Truth.assertThat(values).isEqualTo(listOf(emptySet<StakingBalance>()))
    }

    @Test
    fun `GIVEN stakeKit and p2p balances WHEN produce THEN emits their union`() = runTest {
        // Arrange
        val stakeKitBalances = createStakeKitBalances()
        val p2pEthPoolBalances = createP2PEthPoolBalances()

        every { stakeKitBalancesStore.get(params.userWalletId) } returns flowOf(stakeKitBalances)
        every { p2PEthPoolBalancesStore.get(params.userWalletId) } returns flowOf(p2pEthPoolBalances)

        // Act
        val actual = producer.produce()
        val values = getEmittedValues(flow = actual)

        // Assert
        verify { stakeKitBalancesStore.get(params.userWalletId) }
        verify { p2PEthPoolBalancesStore.get(params.userWalletId) }

        Truth.assertThat(values.size).isEqualTo(1)
        Truth.assertThat(values.first()).isEqualTo(stakeKitBalances + p2pEthPoolBalances)
    }

    @Test
    fun `GIVEN p2p flow updates WHEN produce THEN combined set re-emitted`() = runTest {
        // Arrange
        val stakeKitBalances = createStakeKitBalancesWithTonOnly()
        val p2pEthPoolFlow = MutableSharedFlow<Set<StakingBalance>>(replay = 2)

        every { stakeKitBalancesStore.get(params.userWalletId) } returns flowOf(stakeKitBalances)
        every { p2PEthPoolBalancesStore.get(params.userWalletId) } returns p2pEthPoolFlow

        // Act
        val actual = producer.produce()

        // Assert
        verify { stakeKitBalancesStore.get(params.userWalletId) }
        verify { p2PEthPoolBalancesStore.get(params.userWalletId) }

        // Act & Assert
        p2pEthPoolFlow.emit(emptySet())

        val values1 = getEmittedValues(flow = actual)

        Truth.assertThat(values1.size).isEqualTo(1)
        Truth.assertThat(values1.first()).isEqualTo(stakeKitBalances)

        // Act & Assert
        val p2pEthPoolBalances = createP2PEthPoolBalances()
        p2pEthPoolFlow.emit(p2pEthPoolBalances)

        val values2 = getEmittedValues(flow = actual)

        Truth.assertThat(values2.size).isEqualTo(2)
        Truth.assertThat(values2.last()).isEqualTo(stakeKitBalances + p2pEthPoolBalances)
    }

    private companion object {

        val tonId = MockYieldBalanceWrapperDTOFactory.defaultStakingId
        val solanaId = StakingID(
            integrationId = "solana-sol-native-multivalidator-staking",
            address = "0x1",
        )
        val p2pEthereumId = StakingID(
            integrationId = StakingIntegrationID.P2PEthPool.value,
            address = "0x5aa711F440Eb6d4361148bBD89d03464628ace84",
        )

        fun createStakeKitBalances(): Set<StakingBalance> {
            return setOf(
                MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId).toDomain(),
                MockYieldBalanceWrapperDTOFactory.createWithBalance(solanaId).toDomain(),
            )
        }

        fun createStakeKitBalancesWithTonOnly(): Set<StakingBalance> {
            return setOf(
                MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId).toDomain(),
            )
        }

        fun createP2PEthPoolBalances(): Set<StakingBalance> {
            return setOf(
                MockP2PEthPoolAccountResponseFactory.createWithBalance(stakingId = p2pEthereumId).toDomain(
                    source = StatusSource.ACTUAL,
                ),
            )
        }
    }
}