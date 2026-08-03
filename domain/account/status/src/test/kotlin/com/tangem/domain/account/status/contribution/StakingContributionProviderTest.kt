package com.tangem.domain.account.status.contribution

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.staking.BalanceItem
import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.P2PEthPoolExitQueue
import com.tangem.domain.models.staking.P2PEthPoolExitRequest
import com.tangem.domain.models.staking.P2PEthPoolStake
import com.tangem.domain.models.staking.P2PEthPoolStakingAccount
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.staking.YieldBalanceItem
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.multi.MultiStakingBalanceProducer
import com.tangem.domain.staking.multi.MultiStakingBalanceSupplier
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
 * The staking half of the contributions seam: it owns matching a balance to a currency (staking id + address),
 * narrowing a StakeKit balance to this currency's items, and resolving the per-network rule the contribution then
 * answers with.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StakingContributionProviderTest {

    private val stakingBalanceSupplier: MultiStakingBalanceSupplier = mockk()
    private val stakingIdFactory: StakingIdFactory = mockk()
    private val analyticsExceptionHandler: AnalyticsExceptionHandler = mockk(relaxed = true)

    private val provider = StakingContributionProvider(
        stakingBalanceSupplier = stakingBalanceSupplier,
        stakingIdFactory = stakingIdFactory,
        analyticsExceptionHandler = analyticsExceptionHandler,
    )

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val multiCurrencyWallet = MockUserWalletFactory.create()

    @BeforeEach
    fun resetMocks() {
        clearMocks(stakingBalanceSupplier, stakingIdFactory, analyticsExceptionHandler)
    }

    @Test
    fun `GIVEN single-currency wallet WHEN contributions THEN nothing is contributed`() = runTest {
        // Arrange
        val wallet = MockUserWalletFactory.createSingleWalletWithToken()

        // Act
        val resolver = provider.contributions(wallet).first()

        // Assert — the staking supplier is never even subscribed
        assertThat(resolver).isEqualTo(ContributionResolver.Empty)
        assertThat(resolver.resolve(cryptoCurrencyFactory.ethereum, verifiedStatus(cryptoCurrencyFactory.ethereum)))
            .isNull()
    }

    @Test
    fun `GIVEN balance shared with another token WHEN resolve THEN only this currency's items contribute`() =
        runTest {
            // Arrange — Solana keeps its stake outside the network balance, so everything of ITS items counts.
            // The balance also carries an item of a different token, which narrowing must drop before summing.
            val currency = cryptoCurrencyFactory.createCoin(Blockchain.Solana)
            val shared = stakeKitBalance(currency).let { balance ->
                balance.copy(
                    balance = balance.balance.copy(
                        items = balance.balance.items + balanceItem(
                            coinGeckoId = "another-token",
                            amount = BigDecimal(100),
                            type = BalanceType.STAKED,
                        ),
                    ),
                )
            }
            givenBalances(currency = currency, balance = shared)

            // Act
            val actual = provider.contributions(multiCurrencyWallet).first()
                .resolve(currency, verifiedStatus(currency))

            // Assert
            val staking = actual as StakingBalance.Data.StakeKit
            assertThat(staking.isStakedIncludedInNetworkBalance).isFalse()
            assertThat(staking.balance.items).hasSize(2)
            assertThat(staking.totalDeltaCryptoAmount()).isEqualTo(BigDecimal(10))
        }

    @Test
    fun `GIVEN Cardano currency WHEN resolve THEN the contribution counts rewards only`() = runTest {
        // Arrange — Cardano's stake is already inside the network balance
        val currency = cryptoCurrencyFactory.cardano
        givenBalances(currency = currency, balance = stakeKitBalance(currency))

        // Act
        val actual = provider.contributions(multiCurrencyWallet).first().resolve(currency, verifiedStatus(currency))

        // Assert
        assertThat(actual?.totalDeltaCryptoAmount()).isEqualTo(BigDecimal.ONE)
    }

    @Test
    fun `GIVEN balance staked from another address WHEN resolve THEN nothing is contributed`() = runTest {
        // Arrange
        val currency = cryptoCurrencyFactory.ethereum
        val stakingId = StakingID(integrationId = INTEGRATION_ID, address = "0xanother")
        givenBalances(currency = currency, balance = stakeKitBalance(currency, stakingId), stakingId = stakingId)

        // Act
        val actual = provider.contributions(multiCurrencyWallet).first().resolve(currency, verifiedStatus(currency))

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN balance with no items of this currency WHEN resolve THEN nothing is contributed`() = runTest {
        // Arrange — the balance belongs to another token of the same integration
        val currency = cryptoCurrencyFactory.ethereum
        val foreignItem = balanceItem(coinGeckoId = "another-token", amount = BigDecimal.TEN, type = BalanceType.STAKED)
        givenBalances(
            currency = currency,
            balance = stakeKitBalance(currency).copy(balance = YieldBalanceItem(listOf(foreignItem), INTEGRATION_ID)),
        )

        // Act
        val actual = provider.contributions(multiCurrencyWallet).first().resolve(currency, verifiedStatus(currency))

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN no staking id for the currency WHEN resolve THEN nothing is contributed`() = runTest {
        // Arrange
        val currency = cryptoCurrencyFactory.ethereum
        every { stakingBalanceSupplier(any()) } returns flowOf(emptySet())
        every { stakingIdFactory.create(currencyId = any(), defaultAddress = any()) } returns
            StakingIdFactory.Error.UnsupportedCurrency.left()

        // Act
        val actual = provider.contributions(multiCurrencyWallet).first().resolve(currency, verifiedStatus(currency))

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN P2PEthPool balance WHEN resolve THEN it is stamped for the network and every bucket counts`() =
        runTest {
            // Arrange — Ethereum keeps the staked principal outside the network balance
            val currency = cryptoCurrencyFactory.ethereum
            givenBalances(
                currency = currency,
                balance = p2pEthPoolBalance(staked = 5, unstaking = 2, withdrawable = 1, rewards = 3),
            )

            // Act
            val actual = provider.contributions(multiCurrencyWallet).first()
                .resolve(currency, verifiedStatus(currency))

            // Assert
            val staking = actual as StakingBalance.Data.P2PEthPool
            assertThat(staking.isStakedIncludedInNetworkBalance).isFalse()
            assertThat(staking.totalDeltaCryptoAmount()).isEqualTo(BigDecimal(11))
        }

    @Test
    fun `GIVEN P2PEthPool balance on a chain that already counts the stake WHEN resolve THEN rewards only`() =
        runTest {
            // Arrange — Cardano stands in for "stake already inside the network balance". The point is that the
            // P2PEthPool arm reads the rule from the currency's own network instead of keeping the model default.
            val currency = cryptoCurrencyFactory.cardano
            givenBalances(
                currency = currency,
                balance = p2pEthPoolBalance(staked = 5, unstaking = 2, withdrawable = 1, rewards = 3),
            )

            // Act
            val actual = provider.contributions(multiCurrencyWallet).first()
                .resolve(currency, verifiedStatus(currency))

            // Assert
            val staking = actual as StakingBalance.Data.P2PEthPool
            assertThat(staking.isStakedIncludedInNetworkBalance).isTrue()
            assertThat(staking.totalDeltaCryptoAmount()).isEqualTo(BigDecimal(3))
        }

    @Test
    fun `GIVEN empty staking balance WHEN resolve THEN nothing is contributed`() = runTest {
        // Arrange
        val currency = cryptoCurrencyFactory.ethereum
        givenBalances(
            currency = currency,
            balance = StakingBalance.Empty(stakingId = defaultStakingId(), source = StatusSource.ACTUAL),
        )

        // Act
        val actual = provider.contributions(multiCurrencyWallet).first().resolve(currency, verifiedStatus(currency))

        // Assert — only a StakingBalance.Data can move a total
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN failed staking balance WHEN resolve THEN nothing is contributed`() = runTest {
        // Arrange
        val currency = cryptoCurrencyFactory.ethereum
        givenBalances(currency = currency, balance = StakingBalance.Error(stakingId = defaultStakingId()))

        // Act
        val actual = provider.contributions(multiCurrencyWallet).first().resolve(currency, verifiedStatus(currency))

        // Assert — a failed fetch must not silently contribute zero either
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN several balances for one staking id WHEN resolve THEN the data one wins and the anomaly is reported`() =
        runTest {
            // Arrange
            val currency = cryptoCurrencyFactory.ethereum
            every { stakingBalanceSupplier(any()) } returns flowOf(
                setOf(
                    StakingBalance.Empty(stakingId = defaultStakingId(), source = StatusSource.ACTUAL),
                    stakeKitBalance(currency),
                ),
            )
            every {
                stakingIdFactory.create(currencyId = currency.id, defaultAddress = any())
            } returns defaultStakingId().right()

            // Act
            val actual = provider.contributions(multiCurrencyWallet).first()
                .resolve(currency, verifiedStatus(currency))

            // Assert
            assertThat(actual?.totalDeltaCryptoAmount()).isEqualTo(BigDecimal(10))
            verify(exactly = 1) { analyticsExceptionHandler.sendException(any()) }
        }

    @Test
    fun `GIVEN no network status WHEN resolve THEN nothing is contributed`() = runTest {
        // Arrange
        val currency = cryptoCurrencyFactory.ethereum
        givenBalances(currency = currency, balance = stakeKitBalance(currency))

        // Act — with no status there is no address, so the balance cannot be proven to belong to this currency
        val actual = provider.contributions(multiCurrencyWallet).first().resolve(currency, networkStatus = null)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN multi-currency wallet WHEN contributions THEN the staking supplier is subscribed for that wallet`() =
        runTest {
            // Arrange
            every { stakingBalanceSupplier(any()) } returns flowOf(emptySet())

            // Act
            provider.contributions(multiCurrencyWallet).first()

            // Assert
            verify(exactly = 1) {
                stakingBalanceSupplier(MultiStakingBalanceProducer.Params(multiCurrencyWallet.walletId))
            }
        }

    @Test
    fun `GIVEN an unchanged balance snapshot WHEN contributions THEN the duplicate frame is not re-emitted`() =
        runTest {
            // Arrange — the same snapshot twice, then a genuinely different one
            val currency = cryptoCurrencyFactory.ethereum
            val balance = stakeKitBalance(currency)
            val another = stakeKitBalance(
                currency = currency,
                stakingId = StakingID(integrationId = "another-integration", address = ADDRESS),
            )
            every { stakingBalanceSupplier(any()) } returns flowOf(
                setOf(balance),
                setOf(balance),
                setOf(balance, another),
            )

            // Act
            val frames = provider.contributions(multiCurrencyWallet).toList()

            // Assert — the status producer must not be woken up for a staking snapshot that did not change
            assertThat(frames).hasSize(2)
        }

    // region Fixtures
    private fun defaultStakingId() = StakingID(integrationId = INTEGRATION_ID, address = ADDRESS)

    private fun givenBalances(
        currency: CryptoCurrency,
        balance: StakingBalance,
        stakingId: StakingID = defaultStakingId(),
    ) {
        every { stakingBalanceSupplier(any()) } returns flowOf(setOf(balance))
        every {
            stakingIdFactory.create(currencyId = currency.id, defaultAddress = any())
        } returns stakingId.right()
    }

    private fun stakeKitBalance(
        currency: CryptoCurrency,
        stakingId: StakingID = defaultStakingId(),
    ): StakingBalance.Data.StakeKit {
        return StakingBalance.Data.StakeKit(
            stakingId = stakingId,
            source = StatusSource.ACTUAL,
            balance = YieldBalanceItem(
                items = listOf(
                    balanceItem(
                        coinGeckoId = currency.id.rawCurrencyId?.value,
                        amount = BigDecimal(9),
                        type = BalanceType.STAKED,
                    ),
                    balanceItem(
                        coinGeckoId = currency.id.rawCurrencyId?.value,
                        amount = BigDecimal.ONE,
                        type = BalanceType.REWARDS,
                    ),
                ),
                integrationId = INTEGRATION_ID,
            ),
        )
    }

    /**
     * A real balance — not a mock — because the assertions call [StakingBalance.Data.totalDeltaCryptoAmount] on
     * whatever the provider returns, and a mocked balance would intercept it.
     *
     * Unstaking / withdrawable are derived from the exit queue by claimability, so they go in as requests.
     */
    private fun p2pEthPoolBalance(
        staked: Int,
        unstaking: Int,
        withdrawable: Int,
        rewards: Int,
    ): StakingBalance.Data.P2PEthPool {
        return StakingBalance.Data.P2PEthPool(
            stakingId = defaultStakingId(),
            source = StatusSource.ACTUAL,
            accounts = listOf(
                P2PEthPoolStakingAccount(
                    delegatorAddress = ADDRESS,
                    vaultAddress = "0xvault",
                    stake = P2PEthPoolStake(
                        assets = BigDecimal(staked),
                        totalEarnedAssets = BigDecimal(rewards),
                    ),
                    availableToUnstake = BigDecimal.ZERO,
                    availableToWithdraw = BigDecimal.ZERO,
                    exitQueue = P2PEthPoolExitQueue(
                        total = BigDecimal(unstaking + withdrawable),
                        requests = listOf(
                            exitRequest(totalAssets = unstaking, isClaimable = false),
                            exitRequest(totalAssets = withdrawable, isClaimable = true),
                        ),
                    ),
                ),
            ),
        )
    }

    private fun exitRequest(totalAssets: Int, isClaimable: Boolean): P2PEthPoolExitRequest {
        return P2PEthPoolExitRequest(
            ticket = "ticket-$totalAssets-$isClaimable",
            totalAssets = BigDecimal(totalAssets),
            timestamp = Instant.fromEpochSeconds(epochSeconds = 0),
            withdrawalTimestamp = null,
            isClaimable = isClaimable,
        )
    }

    private fun balanceItem(coinGeckoId: String?, amount: BigDecimal, type: BalanceType): BalanceItem {
        return mockk(relaxed = true) {
            every { this@mockk.token.coinGeckoId } returns coinGeckoId
            every { this@mockk.amount } returns amount
            every { this@mockk.type } returns type
        }
    }

    private fun verifiedStatus(currency: CryptoCurrency): NetworkStatus {
        return NetworkStatus(
            network = currency.network,
            value = NetworkStatus.Verified(
                address = NetworkAddress.Single(
                    defaultAddress = NetworkAddress.Address(
                        value = ADDRESS,
                        type = NetworkAddress.Address.Type.Primary,
                    ),
                ),
                amounts = emptyMap(),
                pendingTransactions = emptyMap(),
                yieldSupplyStatuses = emptyMap(),
                source = StatusSource.ACTUAL,
            ),
        )
    }
    // endregion

    private companion object {

        const val INTEGRATION_ID = "integration"
        const val ADDRESS = "0x1"
    }
}