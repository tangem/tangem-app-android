package com.tangem.data.staking.utils

import com.tangem.data.staking.store.StakingBalancesStore
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultStakingCleanerTest {

    private val stakingBalancesStore = mockk<StakingBalancesStore>(relaxed = true)
    private val cleaner = DefaultStakingCleaner(
        stakingBalancesStore = stakingBalancesStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )
    private val userWalletId = UserWalletId("011")
    private val stakingIds = setOf(
        StakingID(integrationId = StakingIntegrationID.StakeKit.Coin.Cardano.value, address = "0x1"),
    )

    @BeforeEach
    fun setUp() {
        clearMocks(stakingBalancesStore)
    }

    @Test
    fun `should clear yields balances when called`() = runTest {
        // Act
        cleaner(userWalletId = userWalletId, stakingIds = stakingIds)

        // Assert
        coVerifyOrder {
            stakingBalancesStore.clear(userWalletId = userWalletId, stakingIds = stakingIds)
        }
    }

    @Test
    fun `should handle empty stakingIds`() = runTest {
        // Act
        cleaner(userWalletId = userWalletId, stakingIds = emptySet())

        // Assert
        coVerifyOrder(inverse = true) {
            stakingBalancesStore.clear(userWalletId = any(), stakingIds = any())
        }
    }
}