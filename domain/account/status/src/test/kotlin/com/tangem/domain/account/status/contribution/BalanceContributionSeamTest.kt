package com.tangem.domain.account.status.contribution

import arrow.core.right
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.getExtraBalanceOrNull
import com.tangem.common.getTotalCryptoAmount
import com.tangem.common.getTotalFiatAmount
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.staking.BalanceItem
import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.P2PEthPoolExitQueue
import com.tangem.domain.models.staking.P2PEthPoolExitRequest
import com.tangem.domain.models.staking.P2PEthPoolStake
import com.tangem.domain.models.staking.P2PEthPoolStakingAccount
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.staking.YieldBalanceItem
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.multi.MultiStakingBalanceSupplier
import com.tangem.domain.tokens.operations.CryptoCurrencyStatusFactory
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * The balance-contributions seam, composed the way the producer composes it: the real
 * [StakingContributionProvider] / [YieldSupplyContributionProvider] resolve a currency, and
 * [CryptoCurrencyStatusFactory] folds what they return into the status.
 *
 * Balances are critical, so what a reader can actually observe is pinned here end to end — the extra balance on top
 * of the network amount, the totals every UI surface computes through `:common`, and the freshness the status
 * reports.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BalanceContributionSeamTest {

    private val stakingBalanceSupplier: MultiStakingBalanceSupplier = mockk()
    private val stakingIdFactory: StakingIdFactory = mockk()
    private val analyticsExceptionHandler: com.tangem.core.analytics.api.AnalyticsExceptionHandler = mockk(
        relaxed = true,
    )

    private val stakingProvider = StakingContributionProvider(
        stakingBalanceSupplier = stakingBalanceSupplier,
        stakingIdFactory = stakingIdFactory,
        analyticsExceptionHandler = analyticsExceptionHandler,
    )
    private val yieldSupplyProvider = YieldSupplyContributionProvider()

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val wallet = MockUserWalletFactory.create()

    @Test
    fun `GIVEN a cached staking balance WHEN built at the seam THEN the status reports only-cache`() = runTest {
        // Arrange
        val currency = cryptoCurrencyFactory.cardano

        // Act
        val status = buildStatus(
            currency = currency,
            stakingBalance = stakeKitBalance(currency).copy(source = StatusSource.ONLY_CACHE),
        )

        // Assert — a stale staking fetch degrades the whole status, not just the contribution
        assertThat(status.value.sources.contributionSources).containsExactly(StatusSource.ONLY_CACHE)
        assertThat(status.value.sources.total).isEqualTo(StatusSource.ONLY_CACHE)
    }

    @Test
    fun `GIVEN an empty staking balance WHEN built at the seam THEN nothing is contributed`() = runTest {
        // Arrange
        val currency = cryptoCurrencyFactory.cardano
        val empty = StakingBalance.Empty(stakingId = stakingId(), source = StatusSource.ONLY_CACHE)

        // Act
        val status = buildStatus(currency = currency, stakingBalance = empty)

        // Assert — only Data balances ever reach a status
        assertThat(status.value.contributions).isEmpty()
        assertThat(status.getExtraBalanceOrNull()).isNull()
        assertThat(status.value.stakingBalance).isNull()
    }

    @Test
    fun `GIVEN a staking balance of another address WHEN built at the seam THEN nothing is contributed`() = runTest {
        // Arrange — the stake belongs to a different address of the same wallet than the one the status is built for
        val currency = cryptoCurrencyFactory.cardano
        val foreignId = stakingId(address = "0xanother")

        // Act
        val status = buildStatus(
            currency = currency,
            stakingBalance = stakeKitBalance(currency, stakingId = foreignId),
            stakingId = foreignId,
        )

        // Assert
        assertThat(status.value.contributions).isEmpty()
        assertThat(status.getExtraBalanceOrNull()).isNull()
    }

    @Test
    fun `GIVEN yield supply WHEN built at the seam THEN its contribution stays numerically inert`() = runTest {
        // Arrange
        val currency = cryptoCurrencyFactory.ethereum
        val yieldStatus = YieldSupplyStatus(
            isActive = true,
            isInitialized = true,
            isAllowedToSpend = true,
            effectiveProtocolBalance = BigDecimal(4),
        )

        // Act
        val status = buildStatus(
            currency = currency,
            stakingBalance = null,
            yieldSupplyStatuses = mapOf(currency.id to yieldStatus),
        )

        // Assert — the yield-supply balance is already inside the network amount, so it may not be added on top
        assertThat(status.value.contributions).hasSize(1)
        assertThat(status.value.contributions.single().totalDeltaCryptoAmount()).isEqualTo(BigDecimal.ZERO)
        assertThat(status.getTotalCryptoAmount()).isEqualTo(NETWORK_AMOUNT)
        assertThat(status.getTotalFiatAmount()).isEqualTo(NETWORK_AMOUNT)
    }

    /**
     * One case per **live** staking integration ([StakingIntegrationID.entries]), with the item types that
     * integration really returns — TON's warm-up `PREPARING`, Tron's `LOCKED` votes, Solana's several validators,
     * BSC's `UNSTAKING`, and P2PEthPool's account-shaped balance that has no items at all.
     *
     * Cardano is the one that must differ: its stake is already inside the network balance, so only rewards may be
     * added on top.
     */
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PerIntegration {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a live integration WHEN built at the seam THEN the extra balance is pinned`(
            model: IntegrationTestModel,
        ) = runTest {
            // Act
            val status = buildStatus(
                currency = model.currency,
                stakingBalance = model.balance,
                stakingId = model.balance.stakingId,
            )

            // Assert
            assertThat(status.getExtraBalanceOrNull()).isEqualTo(model.expectedExtraBalance)
            assertThat(status.getTotalCryptoAmount()).isEqualTo(NETWORK_AMOUNT + model.expectedExtraBalance)
        }

        @Suppress("LongMethod")
        private fun provideTestModels(): List<IntegrationTestModel> {
            val cosmos = cryptoCurrencyFactory.createCoin(Blockchain.Cosmos)
            val solana = cryptoCurrencyFactory.createCoin(Blockchain.Solana)
            val ton = cryptoCurrencyFactory.createCoin(Blockchain.TON)
            val tron = cryptoCurrencyFactory.createCoin(Blockchain.Tron)
            val bsc = cryptoCurrencyFactory.createCoin(Blockchain.BSC)
            val cardano = cryptoCurrencyFactory.cardano
            val matic = cryptoCurrencyFactory.createToken(blockchain = Blockchain.Ethereum, id = "matic-network")
            val ethereum = cryptoCurrencyFactory.ethereum

            return listOf(
                IntegrationTestModel(
                    integration = StakingIntegrationID.StakeKit.Coin.Cosmos,
                    currency = cosmos,
                    balance = stakeKitFor(
                        StakingIntegrationID.StakeKit.Coin.Cosmos,
                        cosmos,
                        BalanceType.STAKED to 9,
                        BalanceType.REWARDS to 1,
                    ),
                    expectedExtraBalance = BigDecimal(10),
                ),
                IntegrationTestModel(
                    description = "several validators, each its own item",
                    integration = StakingIntegrationID.StakeKit.Coin.Solana,
                    currency = solana,
                    balance = stakeKitFor(
                        StakingIntegrationID.StakeKit.Coin.Solana,
                        solana,
                        BalanceType.STAKED to 4,
                        BalanceType.STAKED to 3,
                        BalanceType.REWARDS to 1,
                    ),
                    expectedExtraBalance = BigDecimal(8),
                ),
                IntegrationTestModel(
                    description = "pool warm-up: PREPARING counts toward the total",
                    integration = StakingIntegrationID.StakeKit.Coin.Ton,
                    currency = ton,
                    balance = stakeKitFor(
                        StakingIntegrationID.StakeKit.Coin.Ton,
                        ton,
                        BalanceType.PREPARING to 2,
                        BalanceType.STAKED to 8,
                    ),
                    expectedExtraBalance = BigDecimal(10),
                ),
                IntegrationTestModel(
                    description = "frozen votes arrive as LOCKED",
                    integration = StakingIntegrationID.StakeKit.Coin.Tron,
                    currency = tron,
                    balance = stakeKitFor(
                        StakingIntegrationID.StakeKit.Coin.Tron,
                        tron,
                        BalanceType.LOCKED to 3,
                        BalanceType.STAKED to 5,
                    ),
                    expectedExtraBalance = BigDecimal(8),
                ),
                IntegrationTestModel(
                    description = "an unbonding position still belongs to the user",
                    integration = StakingIntegrationID.StakeKit.Coin.BSC,
                    currency = bsc,
                    balance = stakeKitFor(
                        StakingIntegrationID.StakeKit.Coin.BSC,
                        bsc,
                        BalanceType.STAKED to 6,
                        BalanceType.UNSTAKING to 2,
                    ),
                    expectedExtraBalance = BigDecimal(8),
                ),
                IntegrationTestModel(
                    description = "stake is already inside the network balance, so only rewards are added",
                    integration = StakingIntegrationID.StakeKit.Coin.Cardano,
                    currency = cardano,
                    balance = stakeKitFor(
                        StakingIntegrationID.StakeKit.Coin.Cardano,
                        cardano,
                        BalanceType.STAKED to 9,
                        BalanceType.REWARDS to 1,
                    ),
                    expectedExtraBalance = BigDecimal(1),
                ),
                IntegrationTestModel(
                    description = "a token, not a coin — narrowing is by coinGeckoId",
                    integration = StakingIntegrationID.StakeKit.EthereumToken.Polygon,
                    currency = matic,
                    balance = stakeKitFor(
                        StakingIntegrationID.StakeKit.EthereumToken.Polygon,
                        matic,
                        BalanceType.STAKED to 7,
                        BalanceType.REWARDS to 1,
                    ),
                    expectedExtraBalance = BigDecimal(8),
                ),
                IntegrationTestModel(
                    description = "account-shaped balance with no items at all",
                    integration = StakingIntegrationID.P2PEthPool,
                    currency = ethereum,
                    balance = p2pEthPoolFor(staked = 5, unstaking = 2, withdrawable = 1, rewards = 3),
                    expectedExtraBalance = BigDecimal(11),
                ),
            )
        }
    }

    internal data class IntegrationTestModel(
        val integration: StakingIntegrationID,
        val currency: CryptoCurrency,
        val balance: StakingBalance.Data,
        val expectedExtraBalance: BigDecimal,
        val description: String = "",
    ) {

        override fun toString(): String = buildString {
            append(integration.value)
            if (description.isNotEmpty()) append(" — ").append(description)
        }
    }

    /** Runs the registered providers over [currency] and folds what they resolve into a status, as the producer does. */
    private suspend fun buildStatus(
        currency: CryptoCurrency,
        stakingBalance: StakingBalance?,
        stakingId: StakingID = stakingId(),
        yieldSupplyStatuses: Map<CryptoCurrency.ID, YieldSupplyStatus?> = emptyMap(),
    ): CryptoCurrencyStatus {
        every { stakingBalanceSupplier(any()) } returns flowOf(setOfNotNull(stakingBalance))
        every { stakingIdFactory.create(currencyId = any(), defaultAddress = any()) } returns stakingId.right()

        val networkStatus = verifiedStatus(currency = currency, yieldSupplyStatuses = yieldSupplyStatuses)
        val resolvers = listOf(
            stakingProvider.contributions(wallet).first(),
            yieldSupplyProvider.contributions(wallet).first(),
        )

        return CryptoCurrencyStatusFactory.create(
            currency = currency,
            maybeNetworkStatus = networkStatus.some(),
            maybeQuoteStatus = quoteStatus(currency).some(),
            contributions = resolvers.mapNotNull { it.resolve(currency = currency, networkStatus = networkStatus) },
        )
    }

    // region Fixtures
    private fun stakingId(address: String = ADDRESS) = StakingID(
        integrationId = INTEGRATION_ID,
        address = address,
    )

    private fun stakeKitBalance(
        currency: CryptoCurrency,
        stakingId: StakingID = stakingId(),
    ): StakingBalance.Data.StakeKit {
        return StakingBalance.Data.StakeKit(
            stakingId = stakingId,
            source = StatusSource.ACTUAL,
            balance = YieldBalanceItem(
                items = listOf(
                    balanceItem(currency, BigDecimal(9), BalanceType.STAKED),
                    balanceItem(currency, BigDecimal.ONE, BalanceType.REWARDS),
                ),
                integrationId = INTEGRATION_ID,
            ),
        )
    }

    /** A StakeKit balance stamped with the integration's real id, carrying the item types it really returns. */
    private fun stakeKitFor(
        integration: StakingIntegrationID,
        currency: CryptoCurrency,
        vararg items: Pair<BalanceType, Int>,
    ): StakingBalance.Data.StakeKit {
        return StakingBalance.Data.StakeKit(
            stakingId = StakingID(integrationId = integration.value, address = ADDRESS),
            source = StatusSource.ACTUAL,
            balance = YieldBalanceItem(
                items = items.map { (type, amount) -> balanceItem(currency, BigDecimal(amount), type) },
                integrationId = integration.value,
            ),
        )
    }

    /**
     * P2PEthPool reports accounts, not items: unstaking and withdrawable are derived from the exit queue by
     * claimability, so they go in as requests rather than as amounts.
     */
    private fun p2pEthPoolFor(
        staked: Int,
        unstaking: Int,
        withdrawable: Int,
        rewards: Int,
    ): StakingBalance.Data.P2PEthPool {
        return StakingBalance.Data.P2PEthPool(
            stakingId = StakingID(integrationId = StakingIntegrationID.P2PEthPool.value, address = ADDRESS),
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

    private fun exitRequest(totalAssets: Int, isClaimable: Boolean) = P2PEthPoolExitRequest(
        ticket = "ticket-$totalAssets-$isClaimable",
        totalAssets = BigDecimal(totalAssets),
        timestamp = Instant.fromEpochSeconds(epochSeconds = 0),
        withdrawalTimestamp = null,
        isClaimable = isClaimable,
    )

    private fun balanceItem(currency: CryptoCurrency, amount: BigDecimal, type: BalanceType): BalanceItem {
        return mockk(relaxed = true) {
            every { this@mockk.token.coinGeckoId } returns currency.id.rawCurrencyId?.value
            every { this@mockk.amount } returns amount
            every { this@mockk.type } returns type
        }
    }

    private fun verifiedStatus(
        currency: CryptoCurrency,
        yieldSupplyStatuses: Map<CryptoCurrency.ID, YieldSupplyStatus?>,
    ): NetworkStatus {
        return NetworkStatus(
            network = currency.network,
            value = NetworkStatus.Verified(
                address = NetworkAddress.Single(
                    defaultAddress = NetworkAddress.Address(
                        value = ADDRESS,
                        type = NetworkAddress.Address.Type.Primary,
                    ),
                ),
                amounts = mapOf(currency.id to NetworkStatus.Amount.Loaded(value = NETWORK_AMOUNT)),
                pendingTransactions = emptyMap(),
                yieldSupplyStatuses = yieldSupplyStatuses,
                source = StatusSource.ACTUAL,
            ),
        )
    }

    private fun quoteStatus(currency: CryptoCurrency) = QuoteStatus(
        rawCurrencyId = currency.id.rawCurrencyId!!,
        value = QuoteStatus.Data(
            fiatRate = BigDecimal.ONE,
            fiatRateUSD = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
            source = StatusSource.ACTUAL,
        ),
    )
    // endregion

    private companion object {

        const val INTEGRATION_ID = "integration"
        const val ADDRESS = "0x1"
        val NETWORK_AMOUNT: BigDecimal = BigDecimal.TEN
    }
}