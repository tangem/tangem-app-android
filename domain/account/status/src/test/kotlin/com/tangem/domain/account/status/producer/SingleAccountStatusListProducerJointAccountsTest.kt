package com.tangem.domain.account.status.producer

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.networks.repository.NetworksRepository
import com.tangem.domain.quotes.multi.MultiQuoteStatusSupplier
import com.tangem.test.core.getEmittedValues
import com.tangem.test.mock.MockAccounts
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import com.tangem.domain.polymarket.flow.PredictionAccountStatusSupplier
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier

/**
 * A joint account on the wallet screen: its lifecycle state comes from the joint accounts the wallet participates
 * in, its money — read by the Safe address — from the same network statuses as everyone else's.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SingleAccountStatusListProducerJointAccountsTest {

    private val userWallet: UserWallet = MockUserWalletFactory.create()
    private val walletId = userWallet.walletId
    private val ethereum: CryptoCurrency = MockCryptoCurrencyFactory().ethereum
    private val jointEthereum: CryptoCurrency = ethereum.onJointNetwork()
    private val jointAccount: Account.Joint = MockAccounts.createJointAccount(
        ownerKeyIndex = 0,
        cryptoCurrencies = listOf(jointEthereum),
        userWalletId = walletId,
    )

    @Test
    fun `GIVEN an active account with a network status WHEN produced THEN the row carries the balance by the Safe`() =
        runTest {
            // Arrange
            val producer = createProducer(jointAccounts = flowOf(listOf(jointRecord(JointAccount.Status.ACTIVE))))

            // Act
            val status = getEmittedValues(producer.produce()).last().jointStatus()

            // Assert
            assertThat(status.value).isEqualTo(
                JointAccountStatusValue.Active(source = StatusSource.ACTUAL, totalMembers = 3, safeAddress = SAFE),
            )
            val currencyStatus = status.tokenList.flattenCurrencies().single()
            assertThat(currencyStatus.currency).isEqualTo(jointEthereum)
            assertThat(currencyStatus.value).isInstanceOf(CryptoCurrencyStatus.Loaded::class.java)
            assertThat(currencyStatus.value.amount).isEqualTo(BigDecimal.TEN)
            assertThat(status.tokenList.totalFiatBalance).isEqualTo(
                TotalFiatBalance.Loaded(amount = BigDecimal.TEN, source = StatusSource.ACTUAL),
            )
        }

    @Test
    fun `GIVEN a pending account WHEN produced THEN no tokens and the wallet total is not stalled`() = runTest {
        // Arrange
        val producer = createProducer(jointAccounts = flowOf(listOf(jointRecord(JointAccount.Status.PENDING))))

        // Act
        val list = getEmittedValues(producer.produce()).last()

        // Assert
        val status = list.jointStatus()
        assertThat(status.value).isEqualTo(
            JointAccountStatusValue.Pending(source = StatusSource.ACTUAL, joinedMembers = 2, totalMembers = 3),
        )
        assertThat(status.tokenList).isEqualTo(TokenList.Empty)
        assertThat(list.totalFiatBalance).isInstanceOf(TotalFiatBalance.Loaded::class.java)
    }

    @Test
    fun `GIVEN the loaded list has no record of the account WHEN produced THEN the row is unavailable`() = runTest {
        // Arrange
        val producer = createProducer(jointAccounts = flowOf(emptyList()))

        // Act
        val status = getEmittedValues(producer.produce()).last().jointStatus()

        // Assert
        assertThat(status.value).isEqualTo(JointAccountStatusValue.Error.Unavailable)
        assertThat(status.tokenList).isEqualTo(TokenList.Empty)
    }

    @Test
    fun `GIVEN the joint accounts supplier never emits WHEN produced THEN the screen shows a loading row`() = runTest {
        // Arrange
        val producer = createProducer(jointAccounts = emptyFlow())

        // Act
        val lists = getEmittedValues(producer.produce())

        // Assert
        assertThat(lists).isNotEmpty()
        assertThat(lists.last().jointStatus().value).isEqualTo(JointAccountStatusValue.Loading)
        assertThat(lists.last().totalFiatBalance).isInstanceOf(TotalFiatBalance.Loaded::class.java)
    }

    private fun AccountStatusList.jointStatus(): AccountStatus.Joint {
        return accountStatuses.filterIsInstance<AccountStatus.Joint>().single()
    }

    private fun jointRecord(status: JointAccount.Status): JointAccount = JointAccount(
        cryptoAccountId = jointAccount.accountId.value,
        membersCount = 3,
        threshold = 2,
        safeAddress = SAFE.takeIf { status != JointAccount.Status.PENDING },
        status = status,
        members = listOf(
            JointAccount.Member(name = "Alice", address = "0x1", role = JointAccount.Role.CREATOR),
            JointAccount.Member(name = "Bob", address = "0x2", role = JointAccount.Role.MEMBER),
        ),
        source = StatusSource.ACTUAL,
    )

    private fun createProducer(jointAccounts: Flow<List<JointAccount>>): DefaultSingleAccountStatusListProducer {
        return DefaultSingleAccountStatusListProducer(
            params = SingleAccountStatusListProducer.Params(userWalletId = walletId),
            flowProducerTools = mockk(),
            userWalletsListRepository = mockk<UserWalletsListRepository> {
                every { userWallets } returns MutableStateFlow(listOf(userWallet))
            },
            singleAccountListSupplier = mockk<SingleAccountListSupplier> {
                every { this@mockk.invoke(walletId) } returns flowOf(accountList())
            },
            singleJointAccountListSupplier = mockk<SingleJointAccountListSupplier> {
                every { this@mockk.invoke(userWalletId = walletId) } returns jointAccounts
            },
            paymentAccountStatusSupplier = mockk<PaymentAccountStatusSupplier> {
                every { this@mockk.invoke(userWalletId = walletId) } returns flowOf(
                    AccountStatus.Payment(Account.Payment(walletId), PaymentAccountStatusValue.NotCreated),
                )
            },
            moneyAccountStatusSupplier = mockk<MoneyAccountStatusSupplier> {
                every { this@mockk.invoke(userWalletId = walletId) } returns flowOf(
                    AccountStatus.Money(Account.Money(walletId), MoneyAccountStatusValue.NotCreated),
                )
            },
            predictionAccountStatusSupplier = mockk<PredictionAccountStatusSupplier> {
                every { this@mockk.invoke(userWalletId = walletId) } returns flowOf(
                    PredictionAccountStatusValue.NotOnboarded,
                )
            },
            polymarketFeatureToggles = mockk { every { this@mockk.isPolymarketEnabled } returns false },
            // the toggle is off, so the supplier is never invoked
            collectiblesAccountStatusSupplier = mockk(),
            collectiblesFeatureToggles = mockk { every { isCollectiblesEnabled } returns false },
            networksRepository = mockk<NetworksRepository> { coEvery { hasCachedStatuses(walletId) } returns true },
            dispatchers = TestingCoroutineDispatcherProvider(),
            networkStatusSupplier = mockk<MultiNetworkStatusSupplier> {
                every { this@mockk.invoke(any()) } returns flowOf(setOf(jointNetworkStatus()))
            },
            quoteStatusSupplier = mockk<MultiQuoteStatusSupplier> {
                every { this@mockk.invoke(Unit) } returns flowOf(
                    mapOf(jointEthereum.id.rawCurrencyId!! to quoteStatus()),
                )
            },
            balanceContributionProviders = emptySet(),
        )
    }

    private fun accountList(): AccountList {
        // The main account stays empty on purpose: only the joint account's currencies are under test
        val accounts = listOf(Account.Personal.createMainAccount(walletId), jointAccount)

        return AccountList(
            userWalletId = walletId,
            accounts = accounts,
            totalAccounts = accounts.size,
            totalArchivedAccounts = 0,
            totalJointAccounts = 1,
        ).getOrNull()!!
    }

    private fun quoteStatus() = QuoteStatus(
        rawCurrencyId = jointEthereum.id.rawCurrencyId!!,
        value = QuoteStatus.Data(
            fiatRate = BigDecimal.ONE,
            fiatRateUSD = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
            source = StatusSource.ACTUAL,
        ),
    )

    private fun jointNetworkStatus() = NetworkStatus(
        network = jointEthereum.network,
        value = NetworkStatus.Verified(
            address = NetworkAddress.Single(NetworkAddress.Address(value = SAFE, type = NetworkAddress.Address.Type.Primary)),
            amounts = mapOf(jointEthereum.id to NetworkStatus.Amount.Loaded(value = BigDecimal.TEN)),
            pendingTransactions = emptyMap(),
            yieldSupplyStatuses = emptyMap(),
            source = StatusSource.ACTUAL,
        ),
    )

    private fun CryptoCurrency.onJointNetwork(): CryptoCurrency {
        val path = Network.DerivationPath.Joint(value = "m/44'/60'/888888'/0/0")
        val jointNetwork = network.copy(id = Network.ID(rawId = network.id.rawId, derivationPath = path), derivationPath = path)

        return when (this) {
            is CryptoCurrency.Coin -> copy(network = jointNetwork)
            is CryptoCurrency.Token -> copy(network = jointNetwork)
        }
    }

    private companion object {
        const val SAFE = "0x5afe000000000000000000000000000000000001"
    }
}