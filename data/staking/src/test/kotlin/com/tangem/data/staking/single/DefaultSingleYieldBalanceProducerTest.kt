package com.tangem.data.staking.single

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.data.staking.MockYieldBalanceWrapperDTOFactory
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.utils.getEmittedValues
import com.tangem.data.staking.toDomain
import com.tangem.data.staking.utils.StakingIdFactory
import com.tangem.domain.staking.model.StakingID
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.multi.MultiYieldBalanceProducer
import com.tangem.domain.staking.multi.MultiYieldBalanceSupplier
import com.tangem.domain.staking.single.SingleYieldBalanceProducer
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
internal class DefaultSingleYieldBalanceProducerTest {

    private val params = SingleYieldBalanceProducer.Params(
        userWalletId = UserWalletId(stringValue = "011"),
        currencyId = ton.id,
        network = ton.network,
    )

    private val multiNetworkStatusSupplier = mockk<MultiYieldBalanceSupplier>()
    private val stakingIdFactory = mockk<StakingIdFactory>()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val producer = DefaultSingleYieldBalanceProducer(
        params = params,
        stakingIdFactory = stakingIdFactory,
        multiYieldBalanceSupplier = multiNetworkStatusSupplier,
        dispatchers = dispatchers,
    )

    @Test
    fun `test that flow is mapped for data from params`() = runTest {
        val balance = MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId).toDomain()

        val expected = flowOf(
            setOf(
                balance,
                MockYieldBalanceWrapperDTOFactory.createWithBalance(solanaId).toDomain(),
            ),
        )

        val multiParams = MultiYieldBalanceProducer.Params(userWalletId = params.userWalletId)
        every { multiNetworkStatusSupplier(multiParams) } returns expected
        coEvery { stakingIdFactory.create(params.userWalletId, params.currencyId, params.network) } returns stakingIds

        val actual = producer.produce()

        verify { multiNetworkStatusSupplier(multiParams) }

        val values = getEmittedValues(flow = actual)

        coVerify { stakingIdFactory.create(params.userWalletId, params.currencyId, params.network) }

        Truth.assertThat(values.size).isEqualTo(1)
        Truth.assertThat(values).isEqualTo(listOf(balance))
    }

    @Test
    fun `test that flow is updated if yield balance is updated`() = runTest {
        val expected = MutableSharedFlow<Set<YieldBalance>>(replay = 2, extraBufferCapacity = 1)

        val multiParams = MultiYieldBalanceProducer.Params(userWalletId = params.userWalletId)
        every { multiNetworkStatusSupplier(multiParams) } returns expected
        coEvery { stakingIdFactory.create(params.userWalletId, params.currencyId, params.network) } returns stakingIds

        val actual = producer.produceWithFallback()

        verify { multiNetworkStatusSupplier(multiParams) }

        // first emit
        val balance = MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId).toDomain()
        expected.emit(value = setOf(balance))

        val values1 = getEmittedValues(flow = actual)

        coVerify { stakingIdFactory.create(params.userWalletId, params.currencyId, params.network) }

        Truth.assertThat(values1.size).isEqualTo(1)
        Truth.assertThat(values1).isEqualTo(listOf(balance))

        // second emit
        val updatedStatus = YieldBalance.Error(integrationId = tonId.integrationId, address = tonId.address)
        expected.emit(value = setOf(updatedStatus))

        val values2 = getEmittedValues(flow = actual)

        coVerify { stakingIdFactory.create(params.userWalletId, params.currencyId, params.network) }

        Truth.assertThat(values2.size).isEqualTo(2)
        Truth.assertThat(values2).isEqualTo(listOf(balance, updatedStatus))
    }

    @Test
    fun `test that flow is filtered the same status`() = runTest {
        val expected = MutableSharedFlow<Set<YieldBalance>>(replay = 2, extraBufferCapacity = 1)

        val multiParams = MultiYieldBalanceProducer.Params(userWalletId = params.userWalletId)
        every { multiNetworkStatusSupplier(multiParams) } returns expected
        coEvery { stakingIdFactory.create(params.userWalletId, params.currencyId, params.network) } returns stakingIds

        val actual = producer.produceWithFallback()

        verify { multiNetworkStatusSupplier(multiParams) }

        // first emit
        val balance = MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId).toDomain()
        expected.emit(value = setOf(balance))

        val values1 = getEmittedValues(flow = actual)

        coVerify { stakingIdFactory.create(params.userWalletId, params.currencyId, params.network) }

        Truth.assertThat(values1.size).isEqualTo(1)
        Truth.assertThat(values1).isEqualTo(listOf(balance))

        // second emit
        expected.emit(value = setOf(balance))

        val values2 = getEmittedValues(flow = actual)

        coVerify { stakingIdFactory.create(params.userWalletId, params.currencyId, params.network) }

        Truth.assertThat(values2.size).isEqualTo(1)
        Truth.assertThat(values2).isEqualTo(listOf(balance))
    }

    @Test
    fun `test if flow throws exception`() = runTest {
        val exception = IllegalStateException()
        val balance = MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId).toDomain()

        val innerFlow = MutableStateFlow(value = false)
        val expected = flow {
            if (innerFlow.value) {
                emit(setOf(balance))
            } else {
                throw exception
            }
        }
            .buffer(capacity = 5)

        val multiParams = MultiYieldBalanceProducer.Params(userWalletId = params.userWalletId)
        every { multiNetworkStatusSupplier(multiParams) } returns expected
        every { stakingIdFactory.createIntegrationId(currencyId = params.currencyId) } returns tonId.integrationId

        val actual = producer.produceWithFallback()

        verify { multiNetworkStatusSupplier(multiParams) }

        val values1 = getEmittedValues(flow = actual)

        coVerify(inverse = true) { stakingIdFactory.create(any(), any(), any()) }

        Truth.assertThat(values1.size).isEqualTo(1)
        val fallbackStatus = YieldBalance.Error(integrationId = tonId.integrationId, address = null)
        Truth.assertThat(values1).isEqualTo(listOf(fallbackStatus))

        coEvery { stakingIdFactory.create(params.userWalletId, params.currencyId, params.network) } returns stakingIds

        innerFlow.emit(value = true)

        val values2 = getEmittedValues(flow = actual)

        coVerify { stakingIdFactory.create(params.userWalletId, params.currencyId, params.network) }

        Truth.assertThat(values2.size).isEqualTo(1)
        Truth.assertThat(values2).isEqualTo(listOf(balance))
    }

    @Test
    fun `test if flow doesn't contain network from params`() = runTest {
        val balance = MockYieldBalanceWrapperDTOFactory.createWithBalance(solanaId).toDomain()

        val expected = flowOf(setOf(balance))

        val multiParams = MultiYieldBalanceProducer.Params(userWalletId = params.userWalletId)
        every { multiNetworkStatusSupplier(multiParams) } returns expected
        coEvery { stakingIdFactory.create(params.userWalletId, params.currencyId, params.network) } returns stakingIds

        val actual = producer.produce()

        verify { multiNetworkStatusSupplier(multiParams) }

        val values = getEmittedValues(flow = actual)

        coVerify { stakingIdFactory.create(params.userWalletId, params.currencyId, params.network) }

        Truth.assertThat(values.size).isEqualTo(0)
    }

    @Test
    fun `test if wallet manager facade returns empty set`() = runTest {
        val balance = MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId).toDomain()

        val expected = flowOf(setOf(balance))

        val multiParams = MultiYieldBalanceProducer.Params(userWalletId = params.userWalletId)
        every { multiNetworkStatusSupplier(multiParams) } returns expected
        coEvery { stakingIdFactory.create(params.userWalletId, params.currencyId, params.network) } returns emptySet()

        val actual = producer.produce()

        verify { multiNetworkStatusSupplier(multiParams) }

        val values = getEmittedValues(flow = actual)

        coVerify { stakingIdFactory.create(params.userWalletId, params.currencyId, params.network) }

        Truth.assertThat(values.size).isEqualTo(0)
    }

    private companion object {

        val mocks = MockCryptoCurrencyFactory()

        val ton = mocks.createCoin(Blockchain.TON)

        val tonId = MockYieldBalanceWrapperDTOFactory.defaultStakingId
        val solanaId = StakingID(
            integrationId = "solana-sol-native-multivalidator-staking",
            address = "0x1",
        )

        val stakingIds = setOf(tonId)
    }
}