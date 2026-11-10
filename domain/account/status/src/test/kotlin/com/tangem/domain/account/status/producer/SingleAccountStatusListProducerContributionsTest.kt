package com.tangem.domain.account.status.producer

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.status.contribution.YieldSupplyContributionProvider
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.verifier.JointAccountCompositionVerifier
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.balance.BalanceContributionProvider
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.yield.supply.YieldSupplyContribution
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.models.yield.supply.yieldSupply
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.networks.repository.NetworksRepository
import com.tangem.domain.tangempay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.polymarket.flow.PredictionAccountStatusSupplier
import com.tangem.domain.quotes.multi.MultiQuoteStatusSupplier
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.contribution.StakingContributionProvider
import com.tangem.domain.staking.model.*
import com.tangem.domain.staking.model.stakingBalanceData
import com.tangem.domain.staking.multi.MultiStakingBalanceSupplier
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.yield.supply.models.status.YieldModuleStatus
import com.tangem.domain.yield.supply.status.YieldModuleStatusContributionProvider
import com.tangem.domain.yield.supply.status.YieldModuleStatusSupplier
import com.tangem.test.core.getEmittedValues
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
 * Covers the **wiring** of the balance-contributions seam inside the status producer — the part the factory and
 * provider unit tests cannot reach: which flow feeds the status.
 *
 * The registered [BalanceContributionProvider]s are the only source of extra balances, and readers reach the
 * balance through the accessor that resolves it out of the contributions.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SingleAccountStatusListProducerContributionsTest {

    private val jointAccountCompositionVerifier: JointAccountCompositionVerifier = mockk {
        coEvery { isVerified(account = any(), jointAccount = any()) } returns true
        coEvery { ownerAddress(userWalletId = any(), ownerKeyIndex = any()) } returns null
    }

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val currency: CryptoCurrency = cryptoCurrencyFactory.cardano
    private val userWallet: UserWallet = MockUserWalletFactory.create()
    private val walletId = userWallet.walletId

    @Test
    fun `GIVEN registered providers WHEN produced THEN contributions come from the providers`() = runTest {
        // Act
        val status = produceCurrencyStatus()

        // Assert
        assertThat(status.value.contributions).hasSize(1)
        assertThat(status.value.contributions.single().totalDeltaCryptoAmount()).isEqualTo(REWARDS)
        // Axis-2 readers reach the balance through the accessor, which resolves it out of the contributions
        assertThat(status.value.stakingBalanceData).isEqualTo(status.value.contributions.single())
    }

    @Test
    fun `GIVEN yield positions source disabled WHEN produced THEN yield status comes from the network status`() =
        runTest {
            // Act
            val status = produceCurrencyStatus(currency = yieldToken, isYieldPositionsSourceEnabled = false)

            // Assert
            val expectedStatus = YieldSupplyStatus(
                isActive = true,
                isInitialized = true,
                isAllowedToSpend = true,
                effectiveProtocolBalance = BigDecimal("4"),
            )
            assertThat(status.value.yieldSupply).isEqualTo(expectedStatus)
            assertThat(status.value.yieldSupplyStatus).isEqualTo(expectedStatus)
            assertThat(status.value.contributions).containsExactly(
                YieldSupplyContribution(status = networkYieldStatus, source = StatusSource.ACTUAL),
            )
        }

    @Test
    fun `GIVEN yield positions source enabled WHEN produced THEN yield status comes from the position`() = runTest {
        // Act
        val status = produceCurrencyStatus(currency = yieldToken, isYieldPositionsSourceEnabled = true)

        // Assert
        val expectedStatus = YieldSupplyStatus(
            isActive = true,
            isInitialized = true,
            isAllowedToSpend = false,
            effectiveProtocolBalance = BigDecimal("7"),
        )
        assertThat(status.value.yieldSupply).isEqualTo(expectedStatus)
        assertThat(status.value.yieldSupplyStatus).isNull()
        assertThat(status.value.contributions).containsExactly(
            YieldSupplyContribution(status = expectedStatus, source = StatusSource.ONLY_CACHE),
        )
        assertThat(status.value.sources.contributionSources).containsExactly(StatusSource.ONLY_CACHE)
    }

    @Test
    fun `GIVEN supplied funds inside the network amount WHEN produced with positions THEN they are not added again`() =
        runTest {
            // Act — today's SDK: amount 10 already holds the supplied funds
            val status = produceCurrencyStatus(currency = yieldToken, isYieldPositionsSourceEnabled = true)

            // Assert
            assertThat(status.value.amount).isEqualTo(BigDecimal("10"))
            assertThat(status.value.fiatAmount).isEqualTo(BigDecimal("10"))
        }

    @Test
    fun `GIVEN yield positions source enabled and no position WHEN produced THEN network yield status is ignored`() =
        runTest {
            // Act
            val status = produceCurrencyStatus(
                currency = yieldToken,
                isYieldPositionsSourceEnabled = true,
                positions = emptySet(),
            )

            // Assert
            assertThat(status.value.yieldSupply).isNull()
            assertThat(status.value.yieldSupplyStatus).isNull()
            assertThat(status.value.contributions).isEmpty()
        }

    @Test
    fun `GIVEN no contribution providers WHEN produced THEN nothing is contributed`() = runTest {
        // Act — an empty set is a legitimate graph state; the `@Multibinds` declaration exists to allow it
        val status = produceCurrencyStatus(contributionProviders = emptySet())

        // Assert — the guard short-circuits to an empty frame instead of combining zero flows, which would emit
        // nothing at all and stall the whole status.
        assertThat(status.value.contributions).isEmpty()
    }

    /** Drives the producer end to end and returns the single currency status it built. */
    private fun TestScope.produceCurrencyStatus(
        wallet: UserWallet = userWallet,
        currency: CryptoCurrency = this@SingleAccountStatusListProducerContributionsTest.currency,
        contributionProviders: Set<BalanceContributionProvider>? = null,
        isYieldPositionsSourceEnabled: Boolean = false,
        positions: Set<YieldModuleStatus> = setOf(yieldPosition),
    ): CryptoCurrencyStatus {
        val producer = createProducer(
            wallet = wallet,
            currency = currency,
            contributionProviders = contributionProviders,
            isYieldPositionsSourceEnabled = isYieldPositionsSourceEnabled,
            positions = positions,
        )
        val emissions = getEmittedValues(producer.produce())
        val accountStatus = emissions.last().accountStatuses
            .filterIsInstance<AccountStatus.CryptoPortfolio>()
            .single()

        return accountStatus.tokenList.flattenCurrencies().single()
    }

    @Suppress("LongMethod")
    private fun createProducer(
        wallet: UserWallet,
        currency: CryptoCurrency,
        contributionProviders: Set<BalanceContributionProvider>?,
        isYieldPositionsSourceEnabled: Boolean,
        positions: Set<YieldModuleStatus>,
    ): DefaultSingleAccountStatusListProducer {
        val id = wallet.walletId
        val tokensFeatureToggles: TokensFeatureToggles = mockk {
            every { this@mockk.isYieldContributionEnabled } returns isYieldPositionsSourceEnabled
        }
        val yieldModuleStatusSupplier: YieldModuleStatusSupplier = mockk {
            every { this@mockk.invoke(any()) } returns flowOf(positions)
        }
        val stakingBalanceSupplier: MultiStakingBalanceSupplier = mockk {
            every { this@mockk.invoke(any()) } returns flowOf(setOf(stakingBalance))
        }
        val stakingIdFactory: StakingIdFactory = mockk {
            every { create(currencyId = any(), defaultAddress = any()) } returns STAKING_ID.right()
        }
        val analyticsExceptionHandler: AnalyticsExceptionHandler = mockk(relaxed = true)

        return DefaultSingleAccountStatusListProducer(
            params = SingleAccountStatusListProducer.Params(userWalletId = id),
            flowProducerTools = mockk(),
            // getSyncStrict is an extension over this StateFlow, so the flow is what has to be stubbed
            userWalletsListRepository = mockk<UserWalletsListRepository> {
                every { userWallets } returns MutableStateFlow(listOf(wallet))
            },
            jointAccountCompositionVerifier = jointAccountCompositionVerifier,
            singleJointAccountListSupplier = mockk<SingleJointAccountListSupplier> {
                every { this@mockk.invoke(userWalletId = any()) } returns flowOf(emptyList())
            },
            singleAccountListSupplier = mockk<SingleAccountListSupplier> {
                every { this@mockk.invoke(id) } returns flowOf(accountList(id, currency))
            },
            paymentAccountStatusSupplier = mockk<PaymentAccountStatusSupplier> {
                every { this@mockk.invoke(userWalletId = id) } returns flowOf(mockk(relaxed = true))
            },
            moneyAccountStatusSupplier = mockk<MoneyAccountStatusSupplier> {
                every { this@mockk.invoke(userWalletId = id) } returns flowOf(mockk(relaxed = true))
            },
            predictionAccountStatusSupplier = mockk<PredictionAccountStatusSupplier> {
                every { this@mockk.invoke(userWalletId = id) } returns flowOf(PredictionAccountStatusValue.Loading)
            },
            // the toggle is off, so the supplier is never invoked
            collectiblesAccountStatusSupplier = mockk(),
            collectiblesFeatureToggles = mockk { every { isCollectiblesEnabled } returns false },
            networksRepository = mockk<NetworksRepository> {
                coEvery { hasCachedStatuses(id) } returns true
            },
            dispatchers = TestingCoroutineDispatcherProvider(),
            networkStatusSupplier = mockk<MultiNetworkStatusSupplier> {
                every { this@mockk.invoke(any()) } returns flowOf(setOf(networkStatus(currency)))
            },
            quoteStatusSupplier = mockk<MultiQuoteStatusSupplier> {
                every { this@mockk.invoke(Unit) } returns flowOf(
                    mapOf(currency.id.rawCurrencyId!! to quoteStatus(currency)),
                )
            },
            // the real providers: this is what makes it a wiring test rather than a mock dance
            balanceContributionProviders = contributionProviders ?: setOf(
                StakingContributionProvider(
                    stakingBalanceSupplier = stakingBalanceSupplier,
                    stakingIdFactory = stakingIdFactory,
                    analyticsExceptionHandler = analyticsExceptionHandler,
                ),
                YieldSupplyContributionProvider(tokensFeatureToggles = tokensFeatureToggles),
                YieldModuleStatusContributionProvider(
                    yieldModuleStatusSupplier = yieldModuleStatusSupplier,
                    tokensFeatureToggles = tokensFeatureToggles,
                ),
            ),
            tokensFeatureToggles = tokensFeatureToggles,
        )
    }

    // region Fixtures
    private fun accountList(id: UserWalletId, currency: CryptoCurrency): AccountList {
        val account = Account.Personal.createMainAccount(id).copy(cryptoCurrencies = listOf(currency))

        return AccountList(
            userWalletId = id,
            accounts = listOf(account),
            totalAccounts = 1,
            totalArchivedAccounts = 0,
        ).getOrNull()!!
    }

    private val yieldToken: CryptoCurrency.Token = cryptoCurrencyFactory.createToken(
        blockchain = Blockchain.Ethereum,
        id = "usd-coin",
        contractAddress = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48",
    )

    private val networkYieldStatus = YieldSupplyStatus(
        isActive = true,
        isInitialized = true,
        isAllowedToSpend = true,
        effectiveProtocolBalance = BigDecimal("4"),
    )

    private val yieldPosition = YieldModuleStatus(
        networkId = yieldToken.network.id,
        tokenAddress = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48",
        isActive = true,
        isInitialized = true,
        isAllowedToSpend = false,
        effectiveProtocolBalance = BigDecimal("7"),
        source = StatusSource.ONLY_CACHE,
    )

    private val stakingBalance = StakingBalance.Data.StakeKit(
        stakingId = STAKING_ID,
        source = StatusSource.ACTUAL,
        balance = YieldBalanceItem(
            items = listOf(
                balanceItem(BigDecimal(9), BalanceType.STAKED),
                balanceItem(REWARDS, BalanceType.REWARDS),
            ),
            integrationId = INTEGRATION_ID,
        ),
    )

    private fun balanceItem(amount: BigDecimal, type: BalanceType): BalanceItem = mockk(relaxed = true) {
        every { this@mockk.token.coinGeckoId } returns currency.id.rawCurrencyId?.value
        every { this@mockk.amount } returns amount
        every { this@mockk.type } returns type
    }

    private fun networkStatus(currency: CryptoCurrency) = NetworkStatus(
        network = currency.network,
        value = NetworkStatus.Verified(
            address = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = ADDRESS, type = NetworkAddress.Address.Type.Primary),
            ),
            amounts = mapOf(currency.id to NetworkStatus.Amount.Loaded(value = BigDecimal.TEN)),
            pendingTransactions = emptyMap(),
            yieldSupplyStatuses = mapOf(yieldToken.id to networkYieldStatus),
            source = StatusSource.ACTUAL,
        ),
    )

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
        val REWARDS: BigDecimal = BigDecimal.ONE
        val STAKING_ID = StakingID(integrationId = INTEGRATION_ID, address = ADDRESS)
    }
}