package com.tangem.domain.account.status.producer

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.status.contribution.BalanceContributionProvider
import com.tangem.domain.account.status.contribution.StakingContributionProvider
import com.tangem.domain.account.status.contribution.YieldSupplyContributionProvider
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.staking.*
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.networks.repository.NetworksRepository
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.polymarket.flow.PredictionAccountStatusSupplier
import com.tangem.domain.quotes.multi.MultiQuoteStatusSupplier
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.model.stakingBalanceData
import com.tangem.domain.staking.multi.MultiStakingBalanceSupplier
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.virtualaccount.flow.VirtualAccountStatusSupplier
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
 * provider unit tests cannot reach: which flow feeds the status, and what the `TWI_1717_BALANCE_CONTRIBUTIONS`
 * branch actually switches.
 *
 * With the toggle off the producer must keep using its legacy staking join and expose no contributions; with it on
 * the registered [BalanceContributionProvider]s must supply them while the legacy join is short-circuited — and
 * the balance a reader ends up seeing must be the same either way.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SingleAccountStatusListProducerContributionsTest {

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val currency: CryptoCurrency = cryptoCurrencyFactory.cardano
    private val userWallet: UserWallet = MockUserWalletFactory.create()
    private val walletId = userWallet.walletId

    @Test
    fun `GIVEN toggle off WHEN produced THEN staking comes from the legacy join and no contributions are exposed`() =
        runTest {
            // Act
            val status = produceCurrencyStatus(useContributions = false)

            // Assert
            assertThat(status.value.contributions).isEmpty()
            assertThat(status.value.stakingBalance).isNotNull()
            assertThat((status.value.stakingBalance as StakingBalance.Data).totalDeltaCryptoAmount())
                .isEqualTo(REWARDS) // Cardano: rewards only
        }

    @Test
    fun `GIVEN toggle on WHEN produced THEN contributions come from the providers`() = runTest {
        // Act
        val status = produceCurrencyStatus(useContributions = true)

        // Assert
        assertThat(status.value.contributions).hasSize(1)
        assertThat(status.value.contributions.single().totalDeltaCryptoAmount()).isEqualTo(REWARDS)
        // the typed field is no longer back-filled on this path; Axis-2 readers reach the balance through the
        // accessor, which resolves it out of the contributions
        assertThat(status.value.stakingBalance).isNull()
        assertThat(status.value.stakingBalanceData).isEqualTo(status.value.contributions.single())
    }

    @Test
    fun `GIVEN the same wallet WHEN produced with the toggle off and on THEN the observable balance matches`() =
        runTest {
            // Act
            val legacy = produceCurrencyStatus(useContributions = false)
            val contributions = produceCurrencyStatus(useContributions = true)

            // Assert — the contributions list and the typed field differ by design; nothing a reader sees may
            assertThat(contributions.value.stakingBalanceData).isEqualTo(legacy.value.stakingBalanceData)
            assertThat(contributions.value.amount).isEqualTo(legacy.value.amount)
            assertThat(contributions.value.fiatAmount).isEqualTo(legacy.value.fiatAmount)
            assertThat(contributions.value.sources.total).isEqualTo(legacy.value.sources.total)
        }

    @Test
    fun `GIVEN no contribution providers WHEN produced with the toggle on THEN nothing is contributed`() = runTest {
        // Act — an empty set is a legitimate graph state; the `@Multibinds` declaration exists to allow it
        val status = produceCurrencyStatus(useContributions = true, contributionProviders = emptySet())

        // Assert — the guard short-circuits to an empty frame instead of combining zero flows, which would emit
        // nothing at all and stall the whole status. The legacy join stays off, so nothing is attached either.
        assertThat(status.value.contributions).isEmpty()
        assertThat(status.value.stakingBalance).isNull()
    }

    @Test
    fun `GIVEN a single-currency wallet WHEN produced with the toggle off THEN staking is not joined`() = runTest {
        // Act — staking is a multi-currency-wallet feature, so the legacy join must not even subscribe
        val status = produceCurrencyStatus(
            useContributions = false,
            wallet = MockUserWalletFactory.createSingleWalletWithToken(),
        )

        // Assert — note this pins the *policy*, not either guard individually: `stakingFlow` and
        // `findStakingBalance` both check `isMultiCurrency`, so removing only one of them changes nothing
        // observable here. Both have to go before this test goes red.
        assertThat(status.value.stakingBalance).isNull()
        assertThat(status.value.contributions).isEmpty()
    }

    /** Drives the producer end to end and returns the single currency status it built. */
    private fun TestScope.produceCurrencyStatus(
        useContributions: Boolean,
        wallet: UserWallet = userWallet,
        contributionProviders: Set<BalanceContributionProvider>? = null,
    ): CryptoCurrencyStatus {
        val producer = createProducer(
            useContributions = useContributions,
            wallet = wallet,
            contributionProviders = contributionProviders,
        )
        val emissions = getEmittedValues(producer.produce())
        val accountStatus = emissions.last().accountStatuses
            .filterIsInstance<AccountStatus.CryptoPortfolio>()
            .single()

        return accountStatus.tokenList.flattenCurrencies().single()
    }

    @Suppress("LongMethod")
    private fun createProducer(
        useContributions: Boolean,
        wallet: UserWallet = userWallet,
        contributionProviders: Set<BalanceContributionProvider>? = null,
    ): DefaultSingleAccountStatusListProducer {
        val id = wallet.walletId
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
            singleAccountListSupplier = mockk<SingleAccountListSupplier> {
                every { this@mockk.invoke(id) } returns flowOf(accountList(id))
            },
            paymentAccountStatusSupplier = mockk<PaymentAccountStatusSupplier> {
                every { this@mockk.invoke(userWalletId = id) } returns flowOf(mockk(relaxed = true))
            },
            virtualAccountStatusSupplier = mockk<VirtualAccountStatusSupplier> {
                every { this@mockk.invoke(userWalletId = id) } returns flowOf(mockk(relaxed = true))
            },
            predictionAccountStatusSupplier = mockk<PredictionAccountStatusSupplier> {
                every { this@mockk.invoke(userWalletId = id) } returns flowOf(PredictionAccountStatusValue.Loading)
            },
            polymarketFeatureToggles = mockk { every { isPolymarketEnabled } returns true },
            networksRepository = mockk<NetworksRepository> {
                coEvery { hasCachedStatuses(id) } returns true
            },
            dispatchers = TestingCoroutineDispatcherProvider(),
            networkStatusSupplier = mockk<MultiNetworkStatusSupplier> {
                every { this@mockk.invoke(any()) } returns flowOf(setOf(networkStatus()))
            },
            quoteStatusSupplier = mockk<MultiQuoteStatusSupplier> {
                every { this@mockk.invoke(Unit) } returns flowOf(mapOf(currency.id.rawCurrencyId!! to quoteStatus()))
            },
            stakingBalanceSupplier = stakingBalanceSupplier,
            stakingIdFactory = stakingIdFactory,
            analyticsExceptionHandler = analyticsExceptionHandler,
            tokensFeatureToggles = mockk<TokensFeatureToggles> {
                every { isBalanceContributionsEnabled } returns useContributions
            },
            // the real providers: this is what makes it a wiring test rather than a mock dance
            balanceContributionProviders = contributionProviders ?: setOf(
                StakingContributionProvider(
                    stakingBalanceSupplier = stakingBalanceSupplier,
                    stakingIdFactory = stakingIdFactory,
                    analyticsExceptionHandler = analyticsExceptionHandler,
                ),
                YieldSupplyContributionProvider(),
            ),
        )
    }

    // region Fixtures
    private fun accountList(id: UserWalletId): AccountList {
        val account = Account.CryptoPortfolio.createMainAccount(id).copy(cryptoCurrencies = listOf(currency))

        return AccountList(
            userWalletId = id,
            accounts = listOf(account),
            totalAccounts = 1,
            totalArchivedAccounts = 0,
        ).getOrNull()!!
    }

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

    private fun networkStatus() = NetworkStatus(
        network = currency.network,
        value = NetworkStatus.Verified(
            address = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = ADDRESS, type = NetworkAddress.Address.Type.Primary),
            ),
            amounts = mapOf(currency.id to NetworkStatus.Amount.Loaded(value = BigDecimal.TEN)),
            pendingTransactions = emptyMap(),
            yieldSupplyStatuses = emptyMap(),
            source = StatusSource.ACTUAL,
        ),
    )

    private fun quoteStatus() = QuoteStatus(
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