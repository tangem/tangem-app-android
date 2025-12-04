package com.tangem.data.staking.utils

import arrow.core.right
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.data.staking.store.StakingBalancesStore
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultStakingCleanerTest {

    private val stakingIdFactory = mockk<StakingIdFactory>(relaxed = true)
    private val stakingBalancesStore = mockk<StakingBalancesStore>(relaxed = true)
    private val cleaner = DefaultStakingCleaner(
        stakingIdFactory = stakingIdFactory,
        stakingBalancesStore = stakingBalancesStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val userWalletId = UserWalletId("011")

    @BeforeEach
    fun setUp() {
        clearMocks(stakingIdFactory, stakingBalancesStore)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ClearByStakingIds {

        private val stakingIds = setOf(
            StakingID(integrationId = StakingIntegrationID.StakeKit.Coin.Cardano.value, address = "0x1"),
        )

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

        @Test
        fun `should catch exception from stakingBalancesStore and not throw`() = runTest {
            // Arrange
            coEvery { stakingBalancesStore.clear(userWalletId, stakingIds) } throws Exception()

            // Act
            cleaner(userWalletId = userWalletId, stakingIds = stakingIds)

            // Assert
            coVerifyOrder {
                stakingBalancesStore.clear(userWalletId = userWalletId, stakingIds = stakingIds)
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ClearByCurrencies {

        private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
        private val coin = cryptoCurrencyFactory.ethereum

        @Test
        fun `should clear yields balances when called with single currency`() = runTest {
            // Arrange
            val stakingId = StakingID(
                integrationId = "stake_kit_coin_eth",
                address = "0xabc",
            )

            coEvery { stakingIdFactory.create(userWalletId, coin) } returns stakingId.right()

            // Act
            cleaner(userWalletId = userWalletId, currency = coin)

            // Assert
            coVerifyOrder {
                stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = coin)
                stakingBalancesStore.clear(userWalletId = userWalletId, stakingIds = setOf(stakingId))
            }
        }

        @Test
        fun `should handle empty list of currencies`() = runTest {
            // Act
            cleaner(userWalletId = userWalletId, currencies = emptyList())

            // Assert
            coVerifyOrder(inverse = true) {
                stakingIdFactory.create(userWalletId = any(), cryptoCurrency = any())
                stakingBalancesStore.clear(userWalletId = any(), stakingIds = any())
            }
        }

        @Test
        fun `should catch exception from stakingBalancesStore and not throw`() = runTest {
            // Arrange
            val stakingId = StakingID(
                integrationId = "stake_kit_coin_eth",
                address = "0xabc",
            )

            coEvery { stakingIdFactory.create(userWalletId, coin) } returns stakingId.right()
            coEvery {
                stakingBalancesStore.clear(userWalletId = userWalletId, stakingIds = setOf(stakingId))
            } throws Exception()

            // Act
            cleaner(userWalletId = userWalletId, currency = coin)

            // Assert
            coVerifyOrder {
                stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = coin)
                stakingBalancesStore.clear(userWalletId = userWalletId, stakingIds = setOf(stakingId))
            }
        }
    }
}