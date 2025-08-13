package com.tangem.data.staking.store

import com.google.common.truth.Truth
import com.tangem.common.test.data.staking.MockYieldBalanceWrapperDTOFactory
import com.tangem.common.test.datastore.MockStateDataStore
import com.tangem.data.staking.toDomain
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingID
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
internal class YieldsBalancesStoreUpdateMethodsTest {

    private val runtimeStore = RuntimeSharedStore<WalletIdWithBalances>()
    private val persistenceStore = MockStateDataStore<WalletIdWithWrappers>(default = emptyMap())

    private val store = DefaultYieldsBalancesStore(
        runtimeStore = runtimeStore,
        persistenceStore = persistenceStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @Test
    fun `refresh the single id if runtime store is empty`() = runTest {
        store.refresh(userWalletId = userWalletId, stakingId = stakingId)

        val runtimeExpected = mapOf(userWalletId to emptySet<YieldBalance>())

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<YieldBalanceWrapperDTO>>())
    }

    @Test
    fun `refresh the single id if runtime store contains balance with this id`() = runTest {
        val balance = MockYieldBalanceWrapperDTOFactory.createWithBalance().toDomain(source = StatusSource.ACTUAL)

        runtimeStore.store(
            value = mapOf(userWalletId to setOf(balance)),
        )

        store.refresh(userWalletId = userWalletId, stakingId = stakingId)

        val runtimeExpected = mapOf(
            userWalletId to setOf(
                balance.copySealed(source = StatusSource.CACHE),
            ),
        )

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<YieldBalanceWrapperDTO>>())
    }

    @Test
    fun `refresh the multi ids if runtime store is empty`() = runTest {
        store.refresh(userWalletId = userWalletId, stakingIds = stakingIds)

        val runtimeExpected = mapOf(userWalletId to emptySet<YieldBalance>())

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<YieldBalanceWrapperDTO>>())
    }

    @Test
    fun `refresh the multi ids if runtime store contains balance with this id`() = runTest {
        val firstBalance = MockYieldBalanceWrapperDTOFactory.createWithBalance(stakingId = stakingIds.first())
            .toDomain(source = StatusSource.ACTUAL)

        val secondBalance = MockYieldBalanceWrapperDTOFactory.createWithBalance(stakingId = stakingIds.last())
            .toDomain(source = StatusSource.ACTUAL)

        runtimeStore.store(
            value = mapOf(userWalletId to setOf(firstBalance, secondBalance)),
        )

        store.refresh(userWalletId = userWalletId, stakingIds = stakingIds)

        val runtimeExpected = mapOf(
            userWalletId to setOf(
                firstBalance.copySealed(source = StatusSource.CACHE),
                secondBalance.copySealed(source = StatusSource.CACHE),
            ),
        )

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<YieldBalanceWrapperDTO>>())
    }

    @Test
    fun `store actual if runtime and cache stores contain balance with this id`() = runTest {
        val prevWrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance()

        runtimeStore.store(
            value = mapOf(userWalletId to setOf(prevWrapper.toDomain(source = StatusSource.CACHE))),
        )

        persistenceStore.updateData {
            it.toMutableMap().apply {
                put(userWalletId.stringValue, setOf(prevWrapper))
            }
        }

        val newWrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance()

        store.storeActual(userWalletId, setOf(newWrapper))

        val runtimeExpected = mapOf(
            userWalletId to setOf(newWrapper.toDomain(source = StatusSource.ACTUAL)),
        )

        val persistenceExpected = mapOf(
            userWalletId.stringValue to setOf(newWrapper),
        )

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(persistenceExpected)
    }

    @Test
    fun `store error if runtime store is empty`() = runTest {
        store.storeError(userWalletId = userWalletId, stakingIds = setOf(stakingId))

        val runtimeExpected = mapOf(
            userWalletId to setOf(
                YieldBalance.Error(
                    integrationId = stakingId.integrationId,
                    address = stakingId.address,
                ),
            ),
        )

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<YieldBalanceWrapperDTO>>())
    }

    @Test
    fun `store error if runtime store contains balance with this id`() = runTest {
        val wrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance(stakingId)

        runtimeStore.store(
            value = mapOf(
                userWalletId to setOf(wrapper.toDomain(source = StatusSource.CACHE)),
            ),
        )

        store.storeError(userWalletId = userWalletId, stakingIds = setOf(stakingId))

        val runtimeExpected = mapOf(
            userWalletId to setOf(wrapper.toDomain(source = StatusSource.ONLY_CACHE)),
        )

        Truth.assertThat(runtimeStore.getSyncOrNull()).isEqualTo(runtimeExpected)
        Truth.assertThat(persistenceStore.data.firstOrNull()).isEqualTo(emptyMap<String, Set<YieldBalanceWrapperDTO>>())
    }

    private companion object {

        val userWalletId = UserWalletId(stringValue = "011")

        val stakingId = MockYieldBalanceWrapperDTOFactory.defaultStakingId

        val stakingIds = setOf(
            stakingId,
            StakingID(
                integrationId = "solana-sol-native-multivalidator-staking",
                address = "0x1",
            ),
        )
    }
}