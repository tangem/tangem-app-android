package com.tangem.data.staking.multi

import com.google.common.truth.Truth
import com.tangem.common.test.data.staking.MockYieldBalanceWrapperDTOFactory
import com.tangem.common.test.utils.getEmittedValues
import com.tangem.data.staking.store.YieldsBalancesStore
import com.tangem.data.staking.toDomain
import com.tangem.domain.staking.model.StakingID
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.multi.MultiYieldBalanceProducer
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
internal class DefaultMultiYieldBalanceProducerTest {

    private val params = MultiYieldBalanceProducer.Params(userWalletId = UserWalletId("011"))

    private val yieldsBalancesStore = mockk<YieldsBalancesStore>()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val producer = DefaultMultiYieldBalanceProducer(
        params = params,
        yieldsBalancesStore = yieldsBalancesStore,
        dispatchers = dispatchers,
    )

    @Test
    fun `test that flow is mapped for user wallet id from params`() = runTest {
        val balances = setOf(
            MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId).toDomain(),
            MockYieldBalanceWrapperDTOFactory.createWithBalance(solanaId).toDomain(),
        )

        val networksStatusesFlow = flowOf(balances)

        every { yieldsBalancesStore.get(params.userWalletId) } returns networksStatusesFlow

        val actual = producer.produce()

        // check after producer.produce()
        verify { yieldsBalancesStore.get(params.userWalletId) }

        val values = getEmittedValues(flow = actual)

        Truth.assertThat(values.size).isEqualTo(1)
        Truth.assertThat(values.first()).isEqualTo(balances)
    }

    @Test
    fun `test that flow is updated if balances are updated`() = runTest {
        val networksStatusesFlow = MutableSharedFlow<Set<YieldBalance>>(replay = 2)

        every { yieldsBalancesStore.get(params.userWalletId) } returns networksStatusesFlow

        val actual = producer.produce()

        // check after producer.produce()
        verify { yieldsBalancesStore.get(params.userWalletId) }

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
        val networksStatusesFlow = MutableSharedFlow<Set<YieldBalance>>(replay = 2)

        every { yieldsBalancesStore.get(params.userWalletId) } returns networksStatusesFlow

        val actual = producer.produce()

        // check after producer.produce()
        verify { yieldsBalancesStore.get(params.userWalletId) }

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

        every { yieldsBalancesStore.get(params.userWalletId) } returns networksStatusesFlow

        val actual = producer.produceWithFallback()

        // check after producer.produce()
        verify { yieldsBalancesStore.get(params.userWalletId) }

        val values1 = getEmittedValues(flow = actual)

        Truth.assertThat(values1.size).isEqualTo(1)
        Truth.assertThat(values1).isEqualTo(listOf(emptySet<YieldBalance>()))

        innerFlow.emit(value = true)

        val values2 = getEmittedValues(flow = actual)

        Truth.assertThat(values2.size).isEqualTo(1)
        Truth.assertThat(values2).isEqualTo(listOf(balances))
    }

    @Test
    fun `test that flow is empty`() = runTest {
        every { yieldsBalancesStore.get(params.userWalletId) } returns emptyFlow()

        val actual = producer.produce()

        // check after producer.produce()
        verify { yieldsBalancesStore.get(params.userWalletId) }

        val values = getEmittedValues(flow = actual)

        Truth.assertThat(values.size).isEqualTo(1)
        Truth.assertThat(values).isEqualTo(listOf(emptySet<YieldBalance>()))
    }

    private companion object {

        val tonId = MockYieldBalanceWrapperDTOFactory.defaultStakingId
        val solanaId = StakingID(
            integrationId = "solana-sol-native-multivalidator-staking",
            address = "0x1",
        )
    }
}