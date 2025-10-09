package com.tangem.data.staking.utils

import com.tangem.data.staking.store.YieldsBalancesStore
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

    private val yieldsBalancesStore = mockk<YieldsBalancesStore>(relaxed = true)
    private val cleaner = DefaultStakingCleaner(
        yieldsBalancesStore = yieldsBalancesStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )
    private val userWalletId = UserWalletId("011")
    private val stakingIds = setOf(
        StakingID(integrationId = StakingIntegrationID.Coin.Cardano.value, address = "0x1"),
    )

    @BeforeEach
    fun setUp() {
        clearMocks(yieldsBalancesStore)
    }

    @Test
    fun `should clear yields balances when called`() = runTest {
        // Act
        cleaner(userWalletId = userWalletId, stakingIds = stakingIds)

        // Assert
        coVerifyOrder {
            yieldsBalancesStore.clear(userWalletId = userWalletId, stakingIds = stakingIds)
        }
    }

    @Test
    fun `should handle empty stakingIds`() = runTest {
        // Act
        cleaner(userWalletId = userWalletId, stakingIds = emptySet())

        // Assert
        coVerifyOrder(inverse = true) {
            yieldsBalancesStore.clear(userWalletId = any(), stakingIds = any())
        }
    }
}