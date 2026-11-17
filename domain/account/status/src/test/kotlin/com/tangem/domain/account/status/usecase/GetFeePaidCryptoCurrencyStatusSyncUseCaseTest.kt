package com.tangem.domain.account.status.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.provider.NonPortfolioCurrenciesProvider
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.test.mock.yield.MockYieldSupply
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class GetFeePaidCryptoCurrencyStatusSyncUseCaseTest {

    private val currenciesRepository: CurrenciesRepository = mockk()
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val nonPortfolioCurrenciesProvider: NonPortfolioCurrenciesProvider = mockk()

    private val useCase = GetFeePaidCryptoCurrencyStatusSyncUseCase(
        currenciesRepository = currenciesRepository,
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
        nonPortfolioCurrenciesProvider = nonPortfolioCurrenciesProvider,
    )

    private val userWalletId = UserWalletId("011")
    private val network = MockYieldSupply.network()
    private val token = MockYieldSupply.token(network = network)
    private val tokenStatus = MockYieldSupply.loadedStatus(currency = token)
    private val coinStatus = MockYieldSupply.loadedStatus(currency = MockYieldSupply.coin(network = network))

    @BeforeEach
    fun resetMocks() {
        clearMocks(currenciesRepository, singleAccountStatusListSupplier, nonPortfolioCurrenciesProvider)
        coEvery { currenciesRepository.getFeePaidCurrency(userWalletId, network) } returns FeePaidCurrency.Coin
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(userWalletId) } returns null
        coEvery { nonPortfolioCurrenciesProvider.getStatuses(any(), any()) } returns emptyList()
    }

    @Test
    fun `GIVEN the coin in the portfolio WHEN invoke THEN its status without looking outside`() = runTest {
        // Arrange
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(userWalletId) } returns accountStatusListOf(coinStatus)

        // Act
        val actual = useCase(userWalletId, tokenStatus)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(coinStatus)
        coVerify(exactly = 0) { nonPortfolioCurrenciesProvider.getStatuses(any(), any()) }
    }

    @Test
    fun `GIVEN a portfolio currency and the coin only outside WHEN invoke THEN nothing is found`() = runTest {
        // Arrange
        coEvery { nonPortfolioCurrenciesProvider.getStatuses(userWalletId, network) } returns
            listOf(tokenStatus, coinStatus)

        // Act
        val actual = useCase(userWalletId, tokenStatus)

        // Assert
        assertThat(actual.getOrNull()).isNull()
        coVerify(exactly = 0) { nonPortfolioCurrenciesProvider.getStatuses(any(), any()) }
    }

    @Test
    fun `GIVEN a currency held outside the portfolio WHEN invoke THEN the outside coin status only`() = runTest {
        // Arrange
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(userWalletId) } returns
            accountStatusListOf(MockYieldSupply.loadedStatus(currency = MockYieldSupply.coin(network = network)))
        coEvery { nonPortfolioCurrenciesProvider.getStatuses(userWalletId, network) } returns
            listOf(tokenStatus, coinStatus)

        // Act
        val actual = useCase(userWalletId, tokenStatus, isHeldOutsidePortfolio = true)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(coinStatus)
        coVerify(exactly = 0) { singleAccountStatusListSupplier.getSyncOrNull(userWalletId = any()) }
    }

    @Test
    fun `GIVEN a fee token of a currency held outside the portfolio WHEN invoke THEN the outside token status`() =
        runTest {
            // Arrange
            coEvery { currenciesRepository.getFeePaidCurrency(userWalletId, network) } returns FeePaidCurrency.Token(
                tokenId = token.id,
                name = token.name,
                symbol = token.symbol,
                contractAddress = token.contractAddress,
                balance = BigDecimal.ONE,
            )
            coEvery { nonPortfolioCurrenciesProvider.getStatuses(userWalletId, network) } returns
                listOf(coinStatus, tokenStatus)

            // Act
            val actual = useCase(userWalletId, coinStatus, isHeldOutsidePortfolio = true)

            // Assert
            assertThat(actual.getOrNull()).isEqualTo(tokenStatus)
            coVerify(exactly = 0) { singleAccountStatusListSupplier.getSyncOrNull(userWalletId = any()) }
        }

    @Test
    fun `GIVEN a currency held outside the portfolio and no outside coin WHEN invoke THEN nothing is found`() =
        runTest {
            // Arrange
            coEvery { singleAccountStatusListSupplier.getSyncOrNull(userWalletId) } returns
                accountStatusListOf(coinStatus)

            // Act
            val actual = useCase(userWalletId, tokenStatus, isHeldOutsidePortfolio = true)

            // Assert
            assertThat(actual.getOrNull()).isNull()
        }

    @Test
    fun `GIVEN the fee currency is the same currency WHEN invoke THEN the given status`() = runTest {
        // Arrange
        coEvery { currenciesRepository.getFeePaidCurrency(userWalletId, network) } returns FeePaidCurrency.SameCurrency

        // Act
        val actual = useCase(userWalletId, tokenStatus)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(tokenStatus)
    }

    private fun accountStatusListOf(vararg statuses: CryptoCurrencyStatus): AccountStatusList {
        val account = Account.Personal.createMainAccount(
            userWalletId = userWalletId,
            cryptoCurrencies = statuses.map(CryptoCurrencyStatus::currency),
        )
        return AccountStatusList(
            userWalletId = userWalletId,
            accountStatuses = listOf(
                AccountStatus.Personal(
                    account = account,
                    tokenList = TokenList.Ungrouped(
                        totalFiatBalance = TotalFiatBalance.Loading,
                        sortedBy = TokensSortType.NONE,
                        currencies = statuses.toList(),
                    ),
                    priceChangeLce = lceLoading(),
                ),
            ),
            totalAccounts = 1,
            totalArchivedAccounts = 0,
            totalFiatBalance = TotalFiatBalance.Loading,
            sortType = TokensSortType.NONE,
            groupType = TokensGroupType.NONE,
        )
    }
}