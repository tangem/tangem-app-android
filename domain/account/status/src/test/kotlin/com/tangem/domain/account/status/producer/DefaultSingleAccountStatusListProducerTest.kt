package com.tangem.domain.account.status.producer

import arrow.core.nonEmptyListOf
import com.google.common.truth.Truth
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.quote.PriceChange
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.repository.NetworksRepository
import com.tangem.test.core.getEmittedValues
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
@Suppress("UnusedFlow")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultSingleAccountStatusListProducerTest {

    private val accountsCRUDRepository: AccountsCRUDRepository = mockk()
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()
    private val networksRepository: NetworksRepository = mockk()
    private val flowProducerTools: FlowProducerTools = mockk()

    private val userWalletId = UserWalletId("011")
    private val userWallet = mockk<UserWallet> {
        every { this@mockk.walletId } returns userWalletId
    }

    // todo accounts status producer tests
    /*private val producer = DefaultSingleAccountStatusListProducer(
        params = SingleAccountStatusListProducer.Params(userWalletId),
        accountsCRUDRepository = accountsCRUDRepository,
        singleAccountListSupplier = singleAccountListSupplier,
        networksRepository = networksRepository,
        cryptoCurrencyStatusesFlowFactory = cryptoCurrencyStatusesFlowFactory,
        dispatchers = TestingCoroutineDispatcherProvider(),
        flowProducerTools = flowProducerTools,
    )

    @AfterEach
    fun tearDown() {
        clearMocks(
            accountsCRUDRepository,
            singleAccountListSupplier,
            networksRepository,
            cryptoCurrencyStatusesFlowFactory,
        )
    }

    @Test
    fun `flow is mapped for user wallet id from params`() = runTest {
        // Arrange
        val accountList = AccountList.empty(userWalletId = userWalletId)

        every { singleAccountListSupplier(userWalletId) } returns flowOf(accountList)
        coEvery { networksRepository.hasCachedStatuses(userWalletId) } returns true

        // Act
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        val expected = AccountStatusList(
            userWalletId = userWalletId,
            accountStatuses = listOf(
                AccountStatus.Crypto.Portfolio(
                    account = accountList.mainAccount,
                    tokenList = TokenList.Empty,
                    priceChangeLce = PriceChange(value = BigDecimal("0.00"), source = StatusSource.ACTUAL).lceContent(),
                ),
            ),
            totalAccounts = 1,
            totalArchivedAccounts = accountList.totalArchivedAccounts,
            totalFiatBalance = TotalFiatBalance.Loaded(amount = BigDecimal.ZERO, source = StatusSource.ACTUAL),
            sortType = accountList.sortType,
            groupType = accountList.groupType,
        )
        Truth.assertThat(actual).containsExactly(expected)

        coVerifySequence {
            singleAccountListSupplier(userWalletId)
            networksRepository.hasCachedStatuses(userWalletId)
        }

        coVerify(inverse = true) {
            accountsCRUDRepository.getUserWallet(userWalletId = any())
            cryptoCurrencyStatusesFlowFactory.create(userWallet = any(), currency = any())
        }
    }

    @Test
    fun `flow will updated if balances are updated`() = runTest {
        // Arrange
        val accountList = AccountList.empty(userWalletId)
        val updatedAccountList = AccountList.empty(userWalletId = userWalletId, sortType = TokensSortType.BALANCE)

        val accountListFlow = MutableStateFlow(value = accountList)

        every { singleAccountListSupplier(userWalletId) } returns accountListFlow
        coEvery { networksRepository.hasCachedStatuses(userWalletId) } returns true

        // Act (first emission)
        val actual1 = producer.produce().let(::getEmittedValues)

        // Assert (first emission)
        val expected = AccountStatusList(
            userWalletId = userWalletId,
            accountStatuses = listOf(
                AccountStatus.Crypto.Portfolio(
                    account = accountList.mainAccount,
                    tokenList = TokenList.Empty,
                    priceChangeLce = PriceChange(value = BigDecimal("0.00"), source = StatusSource.ACTUAL).lceContent(),
                ),
            ),
            totalAccounts = 1,
            totalArchivedAccounts = accountList.totalArchivedAccounts,
            totalFiatBalance = TotalFiatBalance.Loaded(amount = BigDecimal.ZERO, source = StatusSource.ACTUAL),
            sortType = accountList.sortType,
            groupType = accountList.groupType,
        )
        Truth.assertThat(actual1).containsExactly(expected)

        // Act (second emission)
        accountListFlow.value = updatedAccountList
        val actual2 = producer.produce().let(::getEmittedValues)

        // Assert (second emission)
        val expected2 = AccountStatusList(
            userWalletId = userWalletId,
            accountStatuses = listOf(
                AccountStatus.Crypto.Portfolio(
                    account = updatedAccountList.mainAccount,
                    tokenList = TokenList.Empty,
                    priceChangeLce = PriceChange(value = BigDecimal("0.00"), source = StatusSource.ACTUAL).lceContent(),
                ),
            ),
            totalAccounts = 1,
            totalArchivedAccounts = accountList.totalArchivedAccounts,
            totalFiatBalance = TotalFiatBalance.Loaded(amount = BigDecimal.ZERO, source = StatusSource.ACTUAL),
            sortType = updatedAccountList.sortType,
            groupType = updatedAccountList.groupType,
        )
        Truth.assertThat(actual2).containsExactly(expected2)

        coVerifySequence {
            singleAccountListSupplier(userWalletId)
            networksRepository.hasCachedStatuses(userWalletId)
            networksRepository.hasCachedStatuses(userWalletId)
            singleAccountListSupplier(userWalletId)
            networksRepository.hasCachedStatuses(userWalletId)
        }

        coVerify(inverse = true) {
            accountsCRUDRepository.getUserWallet(userWalletId = any())
            cryptoCurrencyStatusesFlowFactory.create(userWallet = any(), currency = any())
        }
    }

    @Test
    fun `flow is filtered the same balance`() = runTest {
        // Arrange
        val accountList = AccountList.empty(userWalletId)
        val accountListFlow = MutableStateFlow(value = accountList)

        every { singleAccountListSupplier(userWalletId) } returns accountListFlow
        coEvery { networksRepository.hasCachedStatuses(userWalletId) } returns true

        val expected = AccountStatusList(
            userWalletId = userWalletId,
            accountStatuses = listOf(
                AccountStatus.Crypto.Portfolio(
                    account = accountList.mainAccount,
                    tokenList = TokenList.Empty,
                    priceChangeLce = PriceChange(value = BigDecimal("0.00"), source = StatusSource.ACTUAL).lceContent(),
                ),
            ),
            totalAccounts = 1,
            totalArchivedAccounts = accountList.totalArchivedAccounts,
            totalFiatBalance = TotalFiatBalance.Loaded(amount = BigDecimal.ZERO, source = StatusSource.ACTUAL),
            sortType = accountList.sortType,
            groupType = accountList.groupType,
        )

        // Act (first emission)
        val actual1 = producer.produce().let(::getEmittedValues)

        // Assert (first emission)
        Truth.assertThat(actual1).containsExactly(expected)

        // Act (second emission)
        accountListFlow.value = accountList
        val actual2 = producer.produce().let(::getEmittedValues)

        // Assert (second emission)
        Truth.assertThat(actual2).containsExactly(expected)

        coVerifySequence {
            singleAccountListSupplier(userWalletId)
            networksRepository.hasCachedStatuses(userWalletId)
            singleAccountListSupplier(userWalletId)
            networksRepository.hasCachedStatuses(userWalletId)
        }

        coVerify(inverse = true) {
            accountsCRUDRepository.getUserWallet(userWalletId = any())
            cryptoCurrencyStatusesFlowFactory.create(userWallet = any(), currency = any())
        }
    }

    @Test
    fun `flow is produced for account with non empty crypto currencies`() = runTest {
        // Arrange
        val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
        val accountList = AccountList.empty(
            userWalletId = userWalletId,
            cryptoCurrencies = cryptoCurrencyFactory.ethereumAndStellar.toSet(),
        )

        coEvery { accountsCRUDRepository.getUserWallet(userWalletId = userWalletId) } returns userWallet

        every { singleAccountListSupplier(userWalletId) } returns flowOf(accountList)
        coEvery { networksRepository.hasCachedStatuses(userWalletId) } returns true

        val ethereumStatus = CryptoCurrencyStatus(
            currency = cryptoCurrencyFactory.ethereum,
            value = CryptoCurrencyStatus.Loading,
        )
        every {
            cryptoCurrencyStatusesFlowFactory.create(userWallet = userWallet, currency = cryptoCurrencyFactory.ethereum)
        } returns flowOf(ethereumStatus)

        val stellarStatus = CryptoCurrencyStatus(
            currency = cryptoCurrencyFactory.stellar,
            value = CryptoCurrencyStatus.MissedDerivation(priceChange = null, fiatRate = null),
        )
        every {
            cryptoCurrencyStatusesFlowFactory.create(userWallet = userWallet, currency = cryptoCurrencyFactory.stellar)
        } returns flowOf(stellarStatus)

        // Act
        val flow = producer.produce()
        delay(1000)
        val actual = flow.let(::getEmittedValues)

        // Assert
        val expected = AccountStatusList(
            userWalletId = userWalletId,
            accountStatuses = listOf(
                AccountStatus.Crypto.Portfolio(
                    account = accountList.mainAccount,
                    tokenList = TokenList.Ungrouped(
                        totalFiatBalance = TotalFiatBalance.Loading,
                        sortedBy = TokensSortType.NONE,
                        currencies = nonEmptyListOf(ethereumStatus, stellarStatus),
                    ),
                    priceChangeLce = lceLoading(),
                ),
            ),
            totalAccounts = 1,
            totalArchivedAccounts = accountList.totalArchivedAccounts,
            totalFiatBalance = TotalFiatBalance.Loading,
            sortType = accountList.sortType,
            groupType = accountList.groupType,
        )
        Truth.assertThat(actual).containsExactly(expected)

        coVerifySequence {
            singleAccountListSupplier(userWalletId)
            accountsCRUDRepository.getUserWallet(userWalletId = userWalletId)
            cryptoCurrencyStatusesFlowFactory.create(userWallet = userWallet, currency = cryptoCurrencyFactory.ethereum)
            cryptoCurrencyStatusesFlowFactory.create(userWallet = userWallet, currency = cryptoCurrencyFactory.stellar)
            networksRepository.hasCachedStatuses(userWalletId)
        }
    }*/
}