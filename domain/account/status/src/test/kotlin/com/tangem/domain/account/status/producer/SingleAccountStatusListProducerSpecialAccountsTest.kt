package com.tangem.domain.account.status.producer

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.account.VirtualAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.networks.repository.NetworksRepository
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.polymarket.flow.PredictionAccountStatusSupplier
import com.tangem.domain.quotes.multi.MultiQuoteStatusSupplier
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.multi.MultiStakingBalanceSupplier
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.virtualaccount.flow.VirtualAccountStatusSupplier
import com.tangem.test.core.getEmittedValues
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
 * Covers the special accounts — payment, virtual and prediction — that the producer does not build itself but
 * joins from their own suppliers.
 *
 * They are joined with `combine`, which withholds everything until each source has emitted, and that join gates
 * the account statuses of the whole wallet — so a source that goes quiet blanks the screen, not one row. The
 * prediction source is therefore both toggle-gated and seeded before the join, and both of those are pinned here
 * rather than trusted to the supplier's own module.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SingleAccountStatusListProducerSpecialAccountsTest {

    private val currency: CryptoCurrency = MockCryptoCurrencyFactory().cardano
    private val userWallet: UserWallet = MockUserWalletFactory.create()
    private val walletId = userWallet.walletId

    @Test
    fun `GIVEN prediction status is active WHEN produced THEN the list carries it as a prediction account`() =
        runTest {
            // Arrange
            val value = PredictionAccountStatusValue.Active(
                source = StatusSource.ACTUAL,
                balance = BigDecimal.TEN,
                fiatRate = BigDecimal.ONE,
                isTradingAllowed = true,
            )
            val producer = createProducer(predictionStatus = flowOf(value))

            // Act
            val statuses = getEmittedValues(producer.produce()).last().accountStatuses

            // Assert
            val prediction = statuses.filterIsInstance<AccountStatus.Prediction>().single()
            assertThat(prediction.value).isEqualTo(value)
            assertThat(prediction.account).isEqualTo(Account.Prediction(walletId))
        }

    @Test
    fun `GIVEN prediction status is unavailable WHEN produced THEN the other accounts still arrive`() = runTest {
        // Arrange — an unreachable prediction account is a value the supplier emits, not a silence
        val producer = createProducer(
            predictionStatus = flowOf(PredictionAccountStatusValue.Error.Unavailable),
        )

        // Act
        val statuses = getEmittedValues(producer.produce()).last().accountStatuses

        // Assert
        assertThat(statuses.filterIsInstance<AccountStatus.CryptoPortfolio>()).hasSize(1)
        assertThat(statuses.filterIsInstance<AccountStatus.Payment>()).hasSize(1)
        assertThat(statuses.filterIsInstance<AccountStatus.Virtual>()).hasSize(1)
    }

    @Test
    fun `GIVEN a wallet without a prediction account WHEN produced THEN no prediction status is built`() = runTest {
        // Arrange — the account list is what decides whether the account exists; the supplier is asked regardless
        val producer = createProducer(
            predictionStatus = flowOf(PredictionAccountStatusValue.NotOnboarded),
            accounts = accountList().let { list ->
                list.plus(Account.Payment(walletId)).getOrNull()!!
            },
        )

        // Act
        val statuses = getEmittedValues(producer.produce()).last().accountStatuses

        // Assert
        assertThat(statuses.filterIsInstance<AccountStatus.Prediction>()).isEmpty()
    }

    @Test
    fun `GIVEN the prediction supplier never emits WHEN produced THEN the wallet screen is not stalled`() = runTest {
        // Arrange — a silent supplier is what the joined combine cannot survive on its own
        val producer = createProducer(predictionStatus = emptyFlow())

        // Act
        val statuses = getEmittedValues(producer.produce()).last().accountStatuses

        // Assert
        assertThat(statuses.filterIsInstance<AccountStatus.CryptoPortfolio>()).hasSize(1)
        assertThat(statuses.filterIsInstance<AccountStatus.Prediction>().single().value)
            .isEqualTo(PredictionAccountStatusValue.Loading)
    }

    @Test
    fun `GIVEN polymarket is disabled WHEN produced THEN the prediction supplier is never subscribed`() = runTest {
        // Arrange
        val supplier = predictionSupplier(flowOf(PredictionAccountStatusValue.NotOnboarded))
        val producer = createProducer(isPolymarketEnabled = false, predictionAccountStatusSupplier = supplier)

        // Act
        val statuses = getEmittedValues(producer.produce()).last().accountStatuses

        // Assert — the supplier builds a producer that reads storage and subscribes a quote, so with the feature
        // off it must not be touched at all, however the account list is shaped
        verify(inverse = true) { supplier.invoke(userWalletId = walletId) }
        assertThat(statuses.filterIsInstance<AccountStatus.CryptoPortfolio>()).hasSize(1)
    }

    private fun predictionSupplier(status: Flow<PredictionAccountStatusValue>) =
        mockk<PredictionAccountStatusSupplier> {
            every { this@mockk.invoke(userWalletId = walletId) } returns status
        }

    @Suppress("LongMethod")
    private fun createProducer(
        predictionStatus: Flow<PredictionAccountStatusValue> = flowOf(PredictionAccountStatusValue.NotOnboarded),
        accounts: AccountList = accountListWithSpecialAccounts(),
        isPolymarketEnabled: Boolean = true,
        predictionAccountStatusSupplier: PredictionAccountStatusSupplier = predictionSupplier(predictionStatus),
    ): DefaultSingleAccountStatusListProducer {
        return DefaultSingleAccountStatusListProducer(
            params = SingleAccountStatusListProducer.Params(userWalletId = walletId),
            flowProducerTools = mockk(),
            userWalletsListRepository = mockk<UserWalletsListRepository> {
                every { userWallets } returns MutableStateFlow(listOf(userWallet))
            },
            singleAccountListSupplier = mockk<SingleAccountListSupplier> {
                every { this@mockk.invoke(walletId) } returns flowOf(accounts)
            },
            paymentAccountStatusSupplier = mockk<PaymentAccountStatusSupplier> {
                every { this@mockk.invoke(userWalletId = walletId) } returns flowOf(
                    AccountStatus.Payment(
                        account = Account.Payment(walletId),
                        value = PaymentAccountStatusValue.NotCreated,
                    ),
                )
            },
            virtualAccountStatusSupplier = mockk<VirtualAccountStatusSupplier> {
                every { this@mockk.invoke(userWalletId = walletId) } returns flowOf(
                    AccountStatus.Virtual(
                        account = Account.Virtual(walletId),
                        value = VirtualAccountStatusValue.NotCreated,
                    ),
                )
            },
            predictionAccountStatusSupplier = predictionAccountStatusSupplier,
            polymarketFeatureToggles = mockk {
                every { this@mockk.isPolymarketEnabled } returns isPolymarketEnabled
            },
            networksRepository = mockk<NetworksRepository> {
                coEvery { hasCachedStatuses(walletId) } returns true
            },
            dispatchers = TestingCoroutineDispatcherProvider(),
            networkStatusSupplier = mockk<MultiNetworkStatusSupplier> {
                every { this@mockk.invoke(any()) } returns flowOf(setOf(networkStatus()))
            },
            quoteStatusSupplier = mockk<MultiQuoteStatusSupplier> {
                every { this@mockk.invoke(Unit) } returns flowOf(emptyMap())
            },
            stakingBalanceSupplier = mockk<MultiStakingBalanceSupplier> {
                every { this@mockk.invoke(any()) } returns flowOf(emptySet())
            },
            stakingIdFactory = mockk {
                every { create(currencyId = any(), defaultAddress = any()) } returns
                    StakingID(integrationId = "integration", address = "0x1").right()
            },
            analyticsExceptionHandler = mockk(relaxed = true),
            tokensFeatureToggles = mockk<TokensFeatureToggles> {
                every { isBalanceContributionsEnabled } returns false
            },
            balanceContributionProviders = emptySet(),
        )
    }

    private fun accountList(): AccountList = AccountList(
        userWalletId = walletId,
        accounts = listOf(
            Account.CryptoPortfolio.createMainAccount(walletId).copy(cryptoCurrencies = listOf(currency)),
        ),
        totalAccounts = 1,
        totalArchivedAccounts = 0,
    ).getOrNull()!!

    private fun networkStatus() = NetworkStatus(
        network = currency.network,
        value = NetworkStatus.Verified(
            address = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = "0x1", type = NetworkAddress.Address.Type.Primary),
            ),
            amounts = mapOf(currency.id to NetworkStatus.Amount.Loaded(value = BigDecimal.TEN)),
            pendingTransactions = emptyMap(),
            yieldSupplyStatuses = emptyMap(),
            source = StatusSource.ACTUAL,
        ),
    )

    private fun accountListWithSpecialAccounts(): AccountList = accountList()
        .plus(Account.Payment(walletId)).getOrNull()!!
        .plus(Account.Virtual(walletId)).getOrNull()!!
        .plus(Account.Prediction(walletId)).getOrNull()!!
}