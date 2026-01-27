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
import com.tangem.test.core.getEmittedValues
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

    @Test
    fun `test that flow is mapped for user wallet id from params`() = runTest {
        val balances = setOf(
            MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId).toDomain(),
            MockYieldBalanceWrapperDTOFactory.createWithBalance(solanaId).toDomain(),
        )

        val networksStatusesFlow = flowOf(balances)

        every { stakeKitBalancesStore.get(params.userWalletId) } returns networksStatusesFlow
        every { p2PEthPoolBalancesStore.get(params.userWalletId) } returns flowOf(emptySet())

        val actual = producer.produce()

        // check after producer.produce()
        verify { stakeKitBalancesStore.get(params.userWalletId) }
        verify { p2PEthPoolBalancesStore.get(params.userWalletId) }

        val values = getEmittedValues(flow = actual)

        Truth.assertThat(values.size).isEqualTo(1)
        Truth.assertThat(values.first()).isEqualTo(balances)
    }

    @Test
    fun `test that flow is updated if balances are updated`() = runTest {
        val networksStatusesFlow = MutableSharedFlow<Set<StakingBalance>>(replay = 2)

        every { stakeKitBalancesStore.get(params.userWalletId) } returns networksStatusesFlow
        every { p2PEthPoolBalancesStore.get(params.userWalletId) } returns flowOf(emptySet())

        val actual = producer.produce()

        // check after producer.produce()
        verify { stakeKitBalancesStore.get(params.userWalletId) }
        verify { p2PEthPoolBalancesStore.get(params.userWalletId) }

        // first emit
        val balances = setOf(
            MockYieldBalanceWrapperDTOFactory.createWithEmptyBalance(tonId).toDomain(),
            MockYieldBalanceWrapperDTOFactory.createWithEmptyBalance(solanaId).toDomain(),
        )

        networksStatusesFlow.emit(balances)

        val values1 = getEmittedValues(flow = actual)

        Truth.assertThat(values1.size).isEqualTo(1)
        Truth.assertThat(values1.first()).isEqualTo(balances)

        // second emit
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
    fun `test that flow is filtered the same balance`() = runTest {
        val networksStatusesFlow = MutableSharedFlow<Set<StakingBalance>>(replay = 2)

        every { stakeKitBalancesStore.get(params.userWalletId) } returns networksStatusesFlow
        every { p2PEthPoolBalancesStore.get(params.userWalletId) } returns flowOf(emptySet())

        val actual = producer.produce()

        // check after producer.produce()
        verify { stakeKitBalancesStore.get(params.userWalletId) }
        verify { p2PEthPoolBalancesStore.get(params.userWalletId) }

        // first emit
        val wrappers = setOf(
            MockYieldBalanceWrapperDTOFactory.createWithEmptyBalance(tonId).toDomain(),
            MockYieldBalanceWrapperDTOFactory.createWithEmptyBalance(solanaId).toDomain(),
        )

        networksStatusesFlow.emit(wrappers)

        val values1 = getEmittedValues(flow = actual)

        Truth.assertThat(values1.size).isEqualTo(1)
        Truth.assertThat(values1.first()).isEqualTo(wrappers)

        // second emit
        networksStatusesFlow.emit(wrappers)

        val values2 = getEmittedValues(flow = actual)

        Truth.assertThat(values2.size).isEqualTo(1)
        Truth.assertThat(values2.first()).isEqualTo(wrappers)
    }

    @Test
    fun `test if flow throws exception`() = runTest {
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

        val actual = producer.produceWithFallback()

        // check after producer.produce()
        verify { stakeKitBalancesStore.get(params.userWalletId) }
        verify { p2PEthPoolBalancesStore.get(params.userWalletId) }

        val values1 = getEmittedValues(flow = actual)

        Truth.assertThat(values1.size).isEqualTo(1)
        Truth.assertThat(values1).isEqualTo(listOf(emptySet<StakingBalance>()))

        innerFlow.emit(value = true)

        val values2 = getEmittedValues(flow = actual)

        Truth.assertThat(values2.size).isEqualTo(1)
        Truth.assertThat(values2).isEqualTo(listOf(balances))
    }

    @Test
    fun `test that flow is empty`() = runTest {
        every { stakeKitBalancesStore.get(params.userWalletId) } returns emptyFlow()
        every { p2PEthPoolBalancesStore.get(params.userWalletId) } returns emptyFlow()

        val actual = producer.produce()

        // check after producer.produce()
        verify { stakeKitBalancesStore.get(params.userWalletId) }
        verify { p2PEthPoolBalancesStore.get(params.userWalletId) }

        val values = getEmittedValues(flow = actual)

        Truth.assertThat(values.size).isEqualTo(1)
        Truth.assertThat(values).isEqualTo(listOf(emptySet<StakingBalance>()))
    }

    @Test
    fun `test that StakeKit and P2P balances are combined`() = runTest {
        val stakeKitBalances = createStakeKitBalances()
        val p2pEthPoolBalances = createP2PEthPoolBalances()

        every { stakeKitBalancesStore.get(params.userWalletId) } returns flowOf(stakeKitBalances)
        every { p2PEthPoolBalancesStore.get(params.userWalletId) } returns flowOf(p2pEthPoolBalances)

        val actual = producer.produce()

        // check after producer.produce()
        verify { stakeKitBalancesStore.get(params.userWalletId) }
        verify { p2PEthPoolBalancesStore.get(params.userWalletId) }

        val values = getEmittedValues(flow = actual)

        Truth.assertThat(values.size).isEqualTo(1)
        Truth.assertThat(values.first()).isEqualTo(stakeKitBalances + p2pEthPoolBalances)
    }

    @Test
    fun `test that P2P balances are updated independently from StakeKit`() = runTest {
        val stakeKitBalances = createStakeKitBalancesWithTonOnly()
        val p2pEthPoolFlow = MutableSharedFlow<Set<StakingBalance>>(replay = 2)

        every { stakeKitBalancesStore.get(params.userWalletId) } returns flowOf(stakeKitBalances)
        every { p2PEthPoolBalancesStore.get(params.userWalletId) } returns p2pEthPoolFlow

        val actual = producer.produce()

        // check after producer.produce()
        verify { stakeKitBalancesStore.get(params.userWalletId) }
        verify { p2PEthPoolBalancesStore.get(params.userWalletId) }

        // first emit - empty P2PEthPool
        p2pEthPoolFlow.emit(emptySet())

        val values1 = getEmittedValues(flow = actual)

        Truth.assertThat(values1.size).isEqualTo(1)
        Truth.assertThat(values1.first()).isEqualTo(stakeKitBalances)

        // second emit - with P2PEthPool balance
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