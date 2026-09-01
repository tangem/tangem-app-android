package com.tangem.domain.account.status.contribution

import arrow.core.right
import arrow.core.some
import arrow.core.toOption
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
import com.tangem.domain.staking.model.stakingBalanceData
import com.tangem.domain.staking.multi.MultiStakingBalanceSupplier
import com.tangem.domain.staking.single.SingleStakingBalanceProducer.Companion.selectStakingBalance
import com.tangem.domain.tokens.operations.BalanceContributionsInput
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
 * Old-vs-new parity at the **seam**, composed the way the producer composes it.
 *
 * Balances are critical, so the contributions architecture ships alongside the legacy staking join and must be
 * provably equivalent before the toggle is released. Each case builds the same currency twice — once through the
 * legacy path (`findStakingBalance`-equivalent → factory with [BalanceContributionsInput.Disabled]) and once
 * through the new path (real [StakingContributionProvider] / [YieldSupplyContributionProvider] → factory with
 * `enabled(...)`) — then compares what a reader can actually observe: the typed staking field, the freshness the
 * status reports, and the totals every UI surface computes through `:common`.
 *
 * `Value.contributions` itself is expected to differ (it is empty on the legacy path by design); everything a user
 * sees must not.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BalanceContributionSeamParityTest {

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
    fun `GIVEN Solana staking WHEN built both ways THEN a reader observes the same balance`() = runTest {
        assertParity(
            currency = cryptoCurrencyFactory.createCoin(Blockchain.Solana),
            stakingBalance = stakeKitBalance(cryptoCurrencyFactory.createCoin(Blockchain.Solana)),
        )
    }

    @Test
    fun `GIVEN Cardano staking WHEN built both ways THEN a reader observes the same balance`() = runTest {
        // the per-network rule is resolved by the factory on one path and by the provider on the other
        assertParity(
            currency = cryptoCurrencyFactory.cardano,
            stakingBalance = stakeKitBalance(cryptoCurrencyFactory.cardano),
        )
    }

    @Test
    fun `GIVEN cached staking balance WHEN built both ways THEN both report the same freshness`() = runTest {
        // Arrange
        val currency = cryptoCurrencyFactory.cardano

        // Act
        val (legacy, contributions) = buildBoth(
            currency = currency,
            stakingBalance = stakeKitBalance(currency).copy(source = StatusSource.ONLY_CACHE),
        )

        // Assert — pinned, not just equal: a stale staking fetch must still degrade the status on both paths,
        // otherwise "both report ACTUAL" would pass a purely differential check
        assertThat(legacy.value.sources.total).isEqualTo(StatusSource.ONLY_CACHE)
        assertThat(contributions.value.sources.total).isEqualTo(StatusSource.ONLY_CACHE)
        assertParityOf(legacy, contributions)
    }

    @Test
    fun `GIVEN empty staking balance WHEN built both ways THEN neither attaches anything`() = runTest {
        val currency = cryptoCurrencyFactory.cardano
        val empty = StakingBalance.Empty(stakingId = stakingId(), source = StatusSource.ONLY_CACHE)

        val (legacy, contributions) = buildBoth(currency = currency, stakingBalance = empty)

        // Assert — Empty is not a Data balance, so the legacy factory drops it too: no divergence
        assertThat(legacy.value.stakingBalance).isNull()
        assertThat(contributions.value.stakingBalance).isNull()
        assertParityOf(legacy, contributions)
    }

    @Test
    fun `GIVEN staking balance of another address WHEN built both ways THEN neither attaches anything`() = runTest {
        val currency = cryptoCurrencyFactory.cardano
        val foreign = stakeKitBalance(currency, stakingId = stakingId(address = "0xanother"))

        val (legacy, contributions) = buildBoth(
            currency = currency,
            stakingBalance = foreign,
            stakingId = stakingId(address = "0xanother"),
        )

        // Assert
        assertThat(legacy.value.stakingBalance).isNull()
        assertThat(contributions.value.stakingBalance).isNull()
        assertParityOf(legacy, contributions)
    }

    @Test
    fun `GIVEN yield supply WHEN built both ways THEN the extra contribution stays numerically inert`() = runTest {
        // Arrange — the new path gains a zero-delta yield contribution the legacy path has no equivalent for
        val currency = cryptoCurrencyFactory.ethereum
        val yieldStatus = YieldSupplyStatus(
            isActive = true,
            isInitialized = true,
            isAllowedToSpend = true,
            effectiveProtocolBalance = BigDecimal(4),
        )

        val (legacy, contributions) = buildBoth(
            currency = currency,
            stakingBalance = null,
            yieldSupplyStatuses = mapOf(currency.id to yieldStatus),
        )

        // Assert — the lists differ on purpose, the numbers must not
        assertThat(legacy.value.contributions).isEmpty()
        assertThat(contributions.value.contributions).hasSize(1)
        assertThat(contributions.value.contributions.single().totalDeltaCryptoAmount()).isEqualTo(BigDecimal.ZERO)
        assertThat(contributions.getTotalCryptoAmount()).isEqualTo(legacy.getTotalCryptoAmount())
        assertThat(contributions.getTotalFiatAmount()).isEqualTo(legacy.getTotalFiatAmount())
        assertThat(contributions.value.sources.total).isEqualTo(legacy.value.sources.total)
    }

    /**
     * One case per **live** staking integration ([StakingIntegrationID.entries]).
     *
     * The generic cases above prove the two architectures agree on a representative balance; these prove it for
     * every integration actually shipped, with the item types that integration really returns — TON's warm-up
     * `PREPARING`, Tron's `LOCKED` votes, Solana's several validators, BSC's `UNSTAKING`, and P2PEthPool's
     * account-shaped balance that has no items at all.
     *
     * Each case asserts twice on purpose:
     * - a **pinned** expected extra balance, which keeps documenting the integration's rule after the legacy path
     *   and its differential assertion are deleted in the toggle-cleanup phase;
     * - **parity** with the legacy path, which is what guards the migration while both exist.
     *
     * Cardano is the one that must differ: its stake is already inside the network balance, so only rewards may be
     * added on top. The per-network rule is applied by the factory on one path and by the provider on the other,
     * which is exactly the divergence a per-integration table is here to catch.
     */
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PerIntegration {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a live integration WHEN built both ways THEN the extra balance is pinned and identical`(
            model: IntegrationTestModel,
        ) = runTest {
            // Act
            val (legacy, contributions) = buildBoth(
                currency = model.currency,
                stakingBalance = model.balance,
                stakingId = model.balance.stakingId,
            )

            // Assert
            assertThat(contributions.getExtraBalanceOrNull()).isEqualTo(model.expectedExtraBalance)
            assertParityOf(legacy, contributions)
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

    private suspend fun assertParity(currency: CryptoCurrency, stakingBalance: StakingBalance?) {
        val (legacy, contributions) = buildBoth(currency = currency, stakingBalance = stakingBalance)

        assertParityOf(legacy, contributions)
    }

    /**
     * Everything a reader can observe about extra balances, on both paths.
     *
     * The staking breakdown is compared through `stakingBalanceData`, not through the raw
     * [CryptoCurrencyStatus.Value.stakingBalance] field: the factory stopped back-filling the typed field on the
     * contributions path, so it is `null` there **by design** and every Axis-2 reader now goes through the
     * accessor. Asserting on the raw field would only re-prove that the shim is gone.
     */
    private fun assertParityOf(legacy: CryptoCurrencyStatus, contributions: CryptoCurrencyStatus) {
        assertThat(contributions.getExtraBalanceOrNull()).isEqualTo(legacy.getExtraBalanceOrNull())
        assertThat(contributions.getTotalCryptoAmount()).isEqualTo(legacy.getTotalCryptoAmount())
        assertThat(contributions.getTotalFiatAmount()).isEqualTo(legacy.getTotalFiatAmount())
        assertThat(contributions.value.stakingBalanceData).isEqualTo(legacy.value.stakingBalanceData)
        assertThat(contributions.value.sources.total).isEqualTo(legacy.value.sources.total)
    }

    /**
     * Builds the same currency through both architectures. The legacy branch mirrors the producer's
     * `findStakingBalance`; the contributions branch runs the real providers.
     */
    private suspend fun buildBoth(
        currency: CryptoCurrency,
        stakingBalance: StakingBalance?,
        stakingId: StakingID = stakingId(),
        yieldSupplyStatuses: Map<CryptoCurrency.ID, YieldSupplyStatus?> = emptyMap(),
    ): Pair<CryptoCurrencyStatus, CryptoCurrencyStatus> {
        every { stakingBalanceSupplier(any()) } returns flowOf(setOfNotNull(stakingBalance))
        every { stakingIdFactory.create(currencyId = any(), defaultAddress = any()) } returns stakingId.right()

        val networkStatus = verifiedStatus(currency = currency, yieldSupplyStatuses = yieldSupplyStatuses)

        // legacy: the producer's own join, then the factory resolves address/coinGeckoId narrowing and the stamp
        val legacySelected = stakingBalance?.let {
            selectStakingBalance(
                currentStakingId = stakingId,
                currentBalances = listOf(it),
                analyticsExceptionHandler = analyticsExceptionHandler,
            )
        }
        val legacy = CryptoCurrencyStatusFactory.create(
            currency = currency,
            maybeNetworkStatus = networkStatus.some(),
            maybeQuoteStatus = quoteStatus(currency).some(),
            maybeStakingBalance = legacySelected.toOption(),
            contributionsInput = BalanceContributionsInput.Disabled,
        )

        // new: whatever the registered providers resolve for this currency
        val resolvers = listOf(
            stakingProvider.contributions(wallet).first(),
            yieldSupplyProvider.contributions(wallet).first(),
        )
        val contributions = CryptoCurrencyStatusFactory.create(
            currency = currency,
            maybeNetworkStatus = networkStatus.some(),
            maybeQuoteStatus = quoteStatus(currency).some(),
            maybeStakingBalance = arrow.core.none(),
            contributionsInput = BalanceContributionsInput.enabled(
                resolvers.mapNotNull { it.resolve(currency = currency, networkStatus = networkStatus) },
            ),
        )

        return legacy to contributions
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
                amounts = mapOf(currency.id to NetworkStatus.Amount.Loaded(value = BigDecimal.TEN)),
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
    }
}