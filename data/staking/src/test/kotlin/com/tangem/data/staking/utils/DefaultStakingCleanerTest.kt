package com.tangem.data.staking.utils

import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.data.staking.store.P2PEthPoolBalancesStore
import com.tangem.data.staking.store.StakeKitBalancesStore
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coJustRun
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultStakingCleanerTest {

    private val stakingIdFactory: StakingIdFactory = mockk()
    private val stakeKitBalancesStore: StakeKitBalancesStore = mockk()
    private val p2pEthPoolBalancesStore: P2PEthPoolBalancesStore = mockk()
    private val cleaner = DefaultStakingCleaner(
        stakingIdFactory = stakingIdFactory,
        stakeKitBalancesStore = stakeKitBalancesStore,
        p2pEthPoolBalancesStore = p2pEthPoolBalancesStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val userWalletId = UserWalletId("011")

    @BeforeEach
    fun setUp() {
        clearMocks(stakingIdFactory, stakeKitBalancesStore, p2pEthPoolBalancesStore)
        coJustRun { stakeKitBalancesStore.clear(any(), any()) }
        coJustRun { p2pEthPoolBalancesStore.clear(any(), any()) }
        coJustRun { stakeKitBalancesStore.remove(any()) }
        coJustRun { p2pEthPoolBalancesStore.remove(any()) }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ClearByStakingIds {

        private val stakingIds = setOf(
            StakingID(integrationId = StakingIntegrationID.StakeKit.Coin.Cardano.value, address = "0x1"),
        )

        @Test
        fun `GIVEN staking ids WHEN invoke THEN both balances stores cleared`() = runTest {
            // Act
            cleaner(userWalletId = userWalletId, stakingIds = stakingIds)

            // Assert
            coVerify {
                stakeKitBalancesStore.clear(userWalletId = userWalletId, stakingIds = stakingIds)
                p2pEthPoolBalancesStore.clear(userWalletId = userWalletId, stakingIds = stakingIds)
            }
        }

        @Test
        fun `GIVEN empty staking ids WHEN invoke THEN stores not touched`() = runTest {
            // Act
            cleaner(userWalletId = userWalletId, stakingIds = emptySet())

            // Assert
            coVerify(exactly = 0) { stakeKitBalancesStore.clear(userWalletId = any(), stakingIds = any()) }
            coVerify(exactly = 0) { p2pEthPoolBalancesStore.clear(userWalletId = any(), stakingIds = any()) }
        }

        @Test
        fun `GIVEN stakeKit store throws WHEN invoke THEN p2p store still cleared without exception`() = runTest {
            // Arrange
            coEvery { stakeKitBalancesStore.clear(userWalletId, stakingIds) } throws Exception()

            // Act
            cleaner(userWalletId = userWalletId, stakingIds = stakingIds)

            // Assert
            coVerify {
                stakeKitBalancesStore.clear(userWalletId = userWalletId, stakingIds = stakingIds)
                p2pEthPoolBalancesStore.clear(userWalletId = userWalletId, stakingIds = stakingIds)
            }
        }

        @Test
        fun `GIVEN p2p store throws WHEN invoke THEN no exception propagated`() = runTest {
            // Arrange
            coEvery { p2pEthPoolBalancesStore.clear(userWalletId, stakingIds) } throws Exception()

            // Act
            cleaner(userWalletId = userWalletId, stakingIds = stakingIds)

            // Assert
            coVerify {
                stakeKitBalancesStore.clear(userWalletId = userWalletId, stakingIds = stakingIds)
                p2pEthPoolBalancesStore.clear(userWalletId = userWalletId, stakingIds = stakingIds)
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ClearByCurrencies {

        private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
        private val coin = cryptoCurrencyFactory.ethereum

        @Test
        fun `GIVEN single currency WHEN invoke THEN staking id created and both stores cleared`() = runTest {
            // Arrange
            val stakingId = StakingID(
                integrationId = "stake_kit_coin_eth",
                address = "0xabc",
            )

            coEvery { stakingIdFactory.create(userWalletId, coin) } returns stakingId.right()

            // Act
            cleaner(userWalletId = userWalletId, currency = coin)

            // Assert
            coVerify {
                stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = coin)
                stakeKitBalancesStore.clear(userWalletId = userWalletId, stakingIds = setOf(stakingId))
                p2pEthPoolBalancesStore.clear(userWalletId = userWalletId, stakingIds = setOf(stakingId))
            }
        }

        @Test
        fun `GIVEN empty currencies WHEN invoke THEN nothing created or cleared`() = runTest {
            // Act
            cleaner(userWalletId = userWalletId, currencies = emptyList())

            // Assert
            coVerify(exactly = 0) { stakingIdFactory.create(userWalletId = any(), cryptoCurrency = any()) }
            coVerify(exactly = 0) { stakeKitBalancesStore.clear(userWalletId = any(), stakingIds = any()) }
            coVerify(exactly = 0) { p2pEthPoolBalancesStore.clear(userWalletId = any(), stakingIds = any()) }
        }

        @Test
        fun `GIVEN factory returns left for every currency WHEN invoke THEN stores not touched`() = runTest {
            // Arrange
            val token = cryptoCurrencyFactory.createToken(blockchain = Blockchain.Ethereum)
            coEvery { stakingIdFactory.create(userWalletId, coin) } returns
                StakingIdFactory.Error.UnsupportedCurrency.left()
            coEvery { stakingIdFactory.create(userWalletId, token) } returns
                StakingIdFactory.Error.UnableToGetAddress(StakingIntegrationID.P2PEthPool).left()

            // Act
            cleaner(userWalletId = userWalletId, currencies = listOf(coin, token))

            // Assert
            coVerify(exactly = 1) { stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = coin) }
            coVerify(exactly = 1) { stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = token) }
            coVerify(exactly = 0) { stakeKitBalancesStore.clear(userWalletId = any(), stakingIds = any()) }
            coVerify(exactly = 0) { p2pEthPoolBalancesStore.clear(userWalletId = any(), stakingIds = any()) }
        }

        @Test
        fun `GIVEN stakeKit store throws WHEN invoke with currency THEN p2p store still cleared`() = runTest {
            // Arrange
            val stakingId = StakingID(
                integrationId = "stake_kit_coin_eth",
                address = "0xabc",
            )

            coEvery { stakingIdFactory.create(userWalletId, coin) } returns stakingId.right()
            coEvery {
                stakeKitBalancesStore.clear(userWalletId = userWalletId, stakingIds = setOf(stakingId))
            } throws Exception()

            // Act
            cleaner(userWalletId = userWalletId, currency = coin)

            // Assert
            coVerify {
                stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = coin)
                stakeKitBalancesStore.clear(userWalletId = userWalletId, stakingIds = setOf(stakingId))
                p2pEthPoolBalancesStore.clear(userWalletId = userWalletId, stakingIds = setOf(stakingId))
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ClearByWalletIds {

        @Test
        fun `GIVEN wallets WHEN clear THEN both balances stores removed once with all ids`() = runTest {
            // Arrange
            val ids = listOf(UserWalletId("011"), UserWalletId("022"))

            // Act
            cleaner.clear(userWalletIds = ids)

            // Assert
            coVerify(exactly = 1) { stakeKitBalancesStore.remove(ids) }
            coVerify(exactly = 1) { p2pEthPoolBalancesStore.remove(ids) }
        }

        @Test
        fun `GIVEN empty list WHEN clear THEN stores are not touched`() = runTest {
            // Act
            cleaner.clear(userWalletIds = emptyList())

            // Assert
            coVerify(exactly = 0) { stakeKitBalancesStore.remove(any()) }
            coVerify(exactly = 0) { p2pEthPoolBalancesStore.remove(any()) }
        }
    }
}