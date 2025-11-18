package com.tangem.domain.account.status.usecase

import com.google.common.truth.Truth
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.utils.assertNone
import com.tangem.common.test.utils.assertSome
import com.tangem.common.test.utils.getEmittedValues
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.*
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetAccountCurrencyStatusUseCaseTest {

    private val supplier = mockk<SingleAccountStatusListSupplier>()
    private val useCase = GetAccountCurrencyStatusUseCase(singleAccountStatusListSupplier = supplier)

    private val userWalletId = UserWalletId("011")
    private val supplierParams = SingleAccountStatusListProducer.Params(userWalletId)
    private val currency = MockCryptoCurrencyFactory().ethereum.let {
        val derivationPath = Network.DerivationPath.Card("m/44'/60'/0'/0/1")

        it.copy(
            network = it.network.copy(
                id = Network.ID(value = "ethereum", derivationPath = derivationPath),
                derivationPath = derivationPath,
            ),
        )
    }

    @BeforeEach
    fun setUp() {
        clearMocks(supplier)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InvokeSync {

        @Test
        fun `invokeSync returns None when supplier returns null`() = runTest {
            // Arrange
            coEvery { supplier.getSyncOrNull(supplierParams) } returns null

            // Act
            val actual = useCase.invokeSync(userWalletId = userWalletId, currencyId = currency.id, network = null)

            // Assert
            assertNone(actual)
            coVerifyOrder { supplier.getSyncOrNull(supplierParams) }
        }

        @Test
        fun `invokeSync returns None when AccountList does not contain required currency id`() = runTest {
            // Arrange
            val accountStatus = AccountStatus.CryptoPortfolio(
                account = Account.CryptoPortfolio.createMainAccount(userWalletId),
                tokenList = TokenList.Empty,
                priceChangeLce = lceLoading(),
            )

            val accountStatusList = mockk<AccountStatusList>(relaxed = true) {
                every { this@mockk.accountStatuses } returns listOf(accountStatus)
            }

            coEvery { supplier.getSyncOrNull(supplierParams) } returns accountStatusList

            // Act
            val actual = useCase.invokeSync(userWalletId = userWalletId, currencyId = currency.id, network = null)

            // Assert
            assertNone(actual)
            coVerifyOrder { supplier.getSyncOrNull(supplierParams) }
        }

        @Test
        fun `invokeSync returns Some if network is not null`() = runTest {
            // Arrange
            val mainAccountStatus = AccountStatus.CryptoPortfolio(
                account = Account.CryptoPortfolio.createMainAccount(userWalletId),
                tokenList = TokenList.Empty,
                priceChangeLce = lceLoading(),
            )

            val account = mockk<Account.CryptoPortfolio>(relaxed = true) {
                every { this@mockk.derivationIndex } returns DerivationIndex(1).getOrNull()!!
                every { this@mockk.cryptoCurrencies } returns setOf(currency)
            }
            val currencyStatus = CryptoCurrencyStatus(currency = currency, value = CryptoCurrencyStatus.Loading)
            val accountStatus = AccountStatus.CryptoPortfolio(
                account = account,
                tokenList = TokenList.Ungrouped(
                    totalFiatBalance = TotalFiatBalance.Loading,
                    sortedBy = TokensSortType.NONE,
                    currencies = listOf(currencyStatus),
                ),
                priceChangeLce = lceLoading(),
            )

            val accountStatusList = mockk<AccountStatusList>(relaxed = true) {
                every { this@mockk.accountStatuses } returns listOf(mainAccountStatus, accountStatus, mockk())
            }

            coEvery { supplier.getSyncOrNull(supplierParams) } returns accountStatusList

            // Act
            val actual = useCase.invokeSync(
                userWalletId = userWalletId,
                currencyId = currency.id,
                network = currency.network,
            )

            // Assert
            val expected = AccountCryptoCurrencyStatus(account = accountStatus.account, status = currencyStatus)
            assertSome(actual, expected)

            coVerifyOrder { supplier.getSyncOrNull(supplierParams) }
        }

        @Test
        fun `invokeSync returns Some if network is null`() = runTest {
            // Arrange
            val account = mockk<Account.CryptoPortfolio>(relaxed = true) {
                every { this@mockk.cryptoCurrencies } returns setOf(currency)
            }
            val currencyStatus = CryptoCurrencyStatus(currency = currency, value = CryptoCurrencyStatus.Loading)
            val accountStatus = AccountStatus.CryptoPortfolio(
                account = account,
                tokenList = TokenList.Ungrouped(
                    totalFiatBalance = TotalFiatBalance.Loading,
                    sortedBy = TokensSortType.NONE,
                    currencies = listOf(currencyStatus),
                ),
                priceChangeLce = lceLoading(),
            )

            val accountStatusList = mockk<AccountStatusList>(relaxed = true) {
                every { this@mockk.accountStatuses } returns listOf(accountStatus)
            }

            coEvery { supplier.getSyncOrNull(supplierParams) } returns accountStatusList

            // Act
            val actual = useCase.invokeSync(userWalletId = userWalletId, currencyId = currency.id, network = null)

            // Assert
            val expected = AccountCryptoCurrencyStatus(account = accountStatus.account, status = currencyStatus)
            assertSome(actual, expected)
            coVerifyOrder { supplier.getSyncOrNull(supplierParams) }
        }
    }

    @Suppress("UnusedFlow")
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Invoke {

        @Test
        fun `invoke returns empty flow when supplier returns empty flow`() = runTest {
            // Arrange
            coEvery { supplier(supplierParams) } returns emptyFlow()

            // Act
            val actual = useCase(userWalletId = userWalletId, currencyId = currency.id, network = null)
                .let(::getEmittedValues)

            // Assert
            Truth.assertThat(actual).isEmpty()
            coVerifyOrder { supplier(supplierParams) }
        }

        @Test
        fun `invoke returns empty flow when AccountList does not contain required currency id`() = runTest {
            // Arrange
            val accountStatus = AccountStatus.CryptoPortfolio(
                account = Account.CryptoPortfolio.createMainAccount(userWalletId),
                tokenList = TokenList.Empty,
                priceChangeLce = lceLoading(),
            )

            val accountStatusList = mockk<AccountStatusList>(relaxed = true) {
                every { this@mockk.accountStatuses } returns listOf(accountStatus)
            }

            coEvery { supplier(supplierParams) } returns flowOf(accountStatusList)

            // Act
            val actual = useCase(userWalletId = userWalletId, currencyId = currency.id, network = null)
                .let(::getEmittedValues)

            // Assert
            Truth.assertThat(actual).isEmpty()
            coVerifyOrder { supplier(supplierParams) }
        }

        @Test
        fun `invoke returns data if network is not null`() = runTest {
            // Arrange
            val mainAccountStatus = AccountStatus.CryptoPortfolio(
                account = Account.CryptoPortfolio.createMainAccount(userWalletId),
                tokenList = TokenList.Empty,
                priceChangeLce = lceLoading(),
            )

            val account = mockk<Account.CryptoPortfolio>(relaxed = true) {
                every { this@mockk.derivationIndex } returns DerivationIndex(1).getOrNull()!!
                every { this@mockk.cryptoCurrencies } returns setOf(currency)
            }
            val currencyStatus = CryptoCurrencyStatus(currency = currency, value = CryptoCurrencyStatus.Loading)
            val accountStatus = AccountStatus.CryptoPortfolio(
                account = account,
                tokenList = TokenList.Ungrouped(
                    totalFiatBalance = TotalFiatBalance.Loading,
                    sortedBy = TokensSortType.NONE,
                    currencies = listOf(currencyStatus),
                ),
                priceChangeLce = lceLoading(),
            )

            val accountStatusList = mockk<AccountStatusList>(relaxed = true) {
                every { this@mockk.accountStatuses } returns listOf(mainAccountStatus, accountStatus, mockk())
            }

            coEvery { supplier(supplierParams) } returns flowOf(accountStatusList)

            // Act
            val actual = useCase(userWalletId = userWalletId, currencyId = currency.id, network = null)
                .let(::getEmittedValues)

            // Assert
            val expected = AccountCryptoCurrencyStatus(account = accountStatus.account, status = currencyStatus)
            Truth.assertThat(actual).containsExactly(expected)

            coVerifyOrder { supplier(supplierParams) }
        }

        @Test
        fun `invoke returns data if network is null`() = runTest {
            // Arrange
            val account = mockk<Account.CryptoPortfolio>(relaxed = true) {
                every { this@mockk.cryptoCurrencies } returns setOf(currency)
            }
            val currencyStatus = CryptoCurrencyStatus(currency = currency, value = CryptoCurrencyStatus.Loading)
            val accountStatus = AccountStatus.CryptoPortfolio(
                account = account,
                tokenList = TokenList.Ungrouped(
                    totalFiatBalance = TotalFiatBalance.Loading,
                    sortedBy = TokensSortType.NONE,
                    currencies = listOf(currencyStatus),
                ),
                priceChangeLce = lceLoading(),
            )

            val accountStatusList = mockk<AccountStatusList>(relaxed = true) {
                every { this@mockk.accountStatuses } returns listOf(accountStatus)
            }

            coEvery { supplier(supplierParams) } returns flowOf(accountStatusList)

            // Act
            val actual = useCase(userWalletId = userWalletId, currencyId = currency.id, network = null)
                .let(::getEmittedValues)

            // Assert
            val expected = AccountCryptoCurrencyStatus(account = accountStatus.account, status = currencyStatus)
            Truth.assertThat(actual).containsExactly(expected)
            coVerifyOrder { supplier(supplierParams) }
        }
    }
}