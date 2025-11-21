package com.tangem.domain.account.status.producer

import arrow.core.nonEmptyListOf
import com.google.common.truth.Truth
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.producer.SingleAccountListProducer
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusesFlowFactory
import com.tangem.domain.account.supplier.SingleAccountListSupplier
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
import com.tangem.test.core.getEmittedValues
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
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
    private val cryptoCurrencyStatusesFlowFactory: CryptoCurrencyStatusesFlowFactory = mockk()

    private val userWalletId = UserWalletId("011")
    private val userWallet = mockk<UserWallet> {
        every { this@mockk.walletId } returns userWalletId
    }

    private val producer = DefaultSingleAccountStatusListProducer(
        params = SingleAccountStatusListProducer.Params(userWalletId),
        accountsCRUDRepository = accountsCRUDRepository,
        singleAccountListSupplier = singleAccountListSupplier,
        cryptoCurrencyStatusesFlowFactory = cryptoCurrencyStatusesFlowFactory,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @AfterEach
    fun tearDown() {
        clearMocks(accountsCRUDRepository, singleAccountListSupplier, cryptoCurrencyStatusesFlowFactory)
    }

    @Test
    fun `flow is mapped for user wallet id from params`() = runTest {
        // Arrange
        val accountList = AccountList.empty(userWalletId = userWalletId)

        every {
            singleAccountListSupplier(params = SingleAccountListProducer.Params(userWalletId))
        } returns flowOf(accountList)

        // Act
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        val expected = AccountStatusList(
            userWalletId = userWalletId,
            accountStatuses = listOf(
                AccountStatus.CryptoPortfolio(
                    account = accountList.mainAccount,
                    tokenList = TokenList.Empty,
                    priceChangeLce = PriceChange(value = BigDecimal("0.00"), source = StatusSource.ACTUAL).lceContent(),
                ),
            ),
            totalAccounts = 1,
            totalFiatBalance = TotalFiatBalance.Loaded(amount = BigDecimal.ZERO, source = StatusSource.ACTUAL),
            sortType = accountList.sortType,
            groupType = accountList.groupType,
        )
        Truth.assertThat(actual).containsExactly(expected)

        coVerify(ordering = Ordering.SEQUENCE) {
            singleAccountListSupplier(params = SingleAccountListProducer.Params(userWalletId))
        }
    }

    @Test
    fun `flow will updated if balances are updated`() = runTest {
        // Arrange
        val accountList = AccountList.empty(userWalletId)
        val updatedAccountList = AccountList.empty(userWalletId = userWalletId, sortType = TokensSortType.BALANCE)

        val accountListFlow = MutableStateFlow(value = accountList)

        every {
            singleAccountListSupplier(params = SingleAccountListProducer.Params(userWalletId))
        } returns accountListFlow

        // Act (first emission)
        val actual1 = producer.produce().let(::getEmittedValues)

        // Assert (first emission)
        val expected = AccountStatusList(
            userWalletId = userWalletId,
            accountStatuses = listOf(
                AccountStatus.CryptoPortfolio(
                    account = accountList.mainAccount,
                    tokenList = TokenList.Empty,
                    priceChangeLce = PriceChange(value = BigDecimal("0.00"), source = StatusSource.ACTUAL).lceContent(),
                ),
            ),
            totalAccounts = 1,
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
                AccountStatus.CryptoPortfolio(
                    account = updatedAccountList.mainAccount,
                    tokenList = TokenList.Empty,
                    priceChangeLce = PriceChange(value = BigDecimal("0.00"), source = StatusSource.ACTUAL).lceContent(),
                ),
            ),
            totalAccounts = 1,
            totalFiatBalance = TotalFiatBalance.Loaded(amount = BigDecimal.ZERO, source = StatusSource.ACTUAL),
            sortType = updatedAccountList.sortType,
            groupType = updatedAccountList.groupType,
        )
        Truth.assertThat(actual2).containsExactly(expected2)

        coVerify(ordering = Ordering.SEQUENCE) {
            singleAccountListSupplier(params = SingleAccountListProducer.Params(userWalletId))
            singleAccountListSupplier(params = SingleAccountListProducer.Params(userWalletId))
        }
    }

    @Test
    fun `flow is filtered the same balance`() = runTest {
        // Arrange
        val accountList = AccountList.empty(userWalletId)
        val accountListFlow = MutableStateFlow(value = accountList)

        every {
            singleAccountListSupplier(params = SingleAccountListProducer.Params(userWalletId))
        } returns accountListFlow

        val expected = AccountStatusList(
            userWalletId = userWalletId,
            accountStatuses = listOf(
                AccountStatus.CryptoPortfolio(
                    account = accountList.mainAccount,
                    tokenList = TokenList.Empty,
                    priceChangeLce = PriceChange(value = BigDecimal("0.00"), source = StatusSource.ACTUAL).lceContent(),
                ),
            ),
            totalAccounts = 1,
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

        coVerify(ordering = Ordering.SEQUENCE) {
            singleAccountListSupplier(params = SingleAccountListProducer.Params(userWalletId))
            singleAccountListSupplier(params = SingleAccountListProducer.Params(userWalletId))
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

        every {
            singleAccountListSupplier(params = SingleAccountListProducer.Params(userWalletId))
        } returns flowOf(accountList)

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
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        val expected = AccountStatusList(
            userWalletId = userWalletId,
            accountStatuses = listOf(
                AccountStatus.CryptoPortfolio(
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
            totalFiatBalance = TotalFiatBalance.Loading,
            sortType = accountList.sortType,
            groupType = accountList.groupType,
        )
        Truth.assertThat(actual).containsExactly(expected)

        coVerify(ordering = Ordering.SEQUENCE) {
            singleAccountListSupplier(params = SingleAccountListProducer.Params(userWalletId))
        }
    }
}