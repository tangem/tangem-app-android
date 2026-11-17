package com.tangem.features.yield.supply.impl.common

import arrow.core.none
import arrow.core.some
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
import com.tangem.test.mock.yield.MockYieldSupply
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

internal class YieldSupplyCurrencyStatusResolverTest {

    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val nonPortfolioCurrenciesProvider: NonPortfolioCurrenciesProvider = mockk()

    private val resolver = YieldSupplyCurrencyStatusResolver(
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
        clearMocks(singleAccountStatusListSupplier, nonPortfolioCurrenciesProvider)
        every { singleAccountStatusListSupplier(userWalletId) } returns flowOf(accountStatusListOf())
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(userWalletId) } returns accountStatusListOf()
        coEvery { nonPortfolioCurrenciesProvider.findCurrency(userWalletId, token.id) } returns null
        coEvery { nonPortfolioCurrenciesProvider.getStatuses(userWalletId, network) } returns emptyList()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Observe {

        @Test
        fun `GIVEN a portfolio currency WHEN observe THEN its portfolio status`() = runTest {
            // Arrange
            every { singleAccountStatusListSupplier(userWalletId) } returns flowOf(accountStatusListOf(tokenStatus))

            // Act
            val actual = resolver.observe(userWalletId, token).first()

            // Assert
            assertThat(actual).isEqualTo(tokenStatus.some())
        }

        @Test
        fun `GIVEN a currency held outside the portfolio WHEN observe THEN its holder status, not the portfolio`() =
            runTest {
                // Arrange
                coEvery { nonPortfolioCurrenciesProvider.findCurrency(userWalletId, token.id) } returns token
                every { nonPortfolioCurrenciesProvider.observe(userWalletId, token) } returns
                    flowOf(Account.Money(userWalletId = userWalletId) to tokenStatus)

                // Act
                val actual = resolver.observe(userWalletId, token).first()

                // Assert
                assertThat(actual).isEqualTo(tokenStatus.some())
                verify(exactly = 0) { singleAccountStatusListSupplier(any<UserWalletId>()) }
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetSync {

        @Test
        fun `GIVEN a portfolio currency WHEN getSync THEN its portfolio status without looking outside`() = runTest {
            // Arrange
            coEvery { singleAccountStatusListSupplier.getSyncOrNull(userWalletId) } returns
                accountStatusListOf(tokenStatus)

            // Act
            val actual = resolver.getSync(userWalletId, token)

            // Assert
            assertThat(actual).isEqualTo(tokenStatus.some())
            coVerify(exactly = 0) { nonPortfolioCurrenciesProvider.getStatuses(any(), any()) }
        }

        @Test
        fun `GIVEN a currency only outside the portfolio WHEN getSync THEN the holder status`() = runTest {
            // Arrange
            coEvery { nonPortfolioCurrenciesProvider.getStatuses(userWalletId, network) } returns
                listOf(coinStatus, tokenStatus)

            // Act
            val actual = resolver.getSync(userWalletId, token)

            // Assert
            assertThat(actual).isEqualTo(tokenStatus.some())
        }

        @Test
        fun `GIVEN a fee token only outside the portfolio WHEN getSync by id THEN the holder status`() = runTest {
            // Arrange
            coEvery { nonPortfolioCurrenciesProvider.getStatuses(userWalletId, network) } returns
                listOf(coinStatus, tokenStatus)

            // Act
            val actual = resolver.getSync(userWalletId = userWalletId, currencyId = token.id, network = network)

            // Assert
            assertThat(actual).isEqualTo(tokenStatus)
        }

        @Test
        fun `GIVEN the currency nowhere WHEN getSync THEN none`() = runTest {
            // Act
            val actual = resolver.getSync(userWalletId, token)

            // Assert
            assertThat(actual).isEqualTo(none<CryptoCurrencyStatus>())
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsInPortfolio {

        @Test
        fun `GIVEN a portfolio currency WHEN isInPortfolio THEN true`() = runTest {
            // Arrange
            coEvery { singleAccountStatusListSupplier.getSyncOrNull(userWalletId) } returns
                accountStatusListOf(tokenStatus)

            // Act
            val actual = resolver.isInPortfolio(userWalletId, token)

            // Assert
            assertThat(actual).isTrue()
        }

        @Test
        fun `GIVEN a currency held outside the portfolio WHEN isInPortfolio THEN false`() = runTest {
            // Arrange
            coEvery { nonPortfolioCurrenciesProvider.getStatuses(userWalletId, network) } returns listOf(tokenStatus)

            // Act
            val actual = resolver.isInPortfolio(userWalletId, token)

            // Assert
            assertThat(actual).isFalse()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsHeldOutsidePortfolio {

        @Test
        fun `GIVEN portfolio not loaded and currency held outside WHEN isHeldOutsidePortfolio THEN true`() = runTest {
            // Arrange
            coEvery { singleAccountStatusListSupplier.getSyncOrNull(userWalletId) } returns null
            coEvery { nonPortfolioCurrenciesProvider.findCurrency(userWalletId, token.id) } returns token

            // Act
            val actual = resolver.isHeldOutsidePortfolio(userWalletId, token)

            // Assert
            assertThat(actual).isTrue()
        }

        @Test
        fun `GIVEN a portfolio currency WHEN isHeldOutsidePortfolio THEN false`() = runTest {
            // Arrange
            coEvery { singleAccountStatusListSupplier.getSyncOrNull(userWalletId) } returns
                accountStatusListOf(tokenStatus)

            // Act
            val actual = resolver.isHeldOutsidePortfolio(userWalletId, token)

            // Assert
            assertThat(actual).isFalse()
        }
    }

    private fun accountStatusListOf(vararg statuses: CryptoCurrencyStatus): AccountStatusList {
        val account = Account.Personal.createMainAccount(
            userWalletId = userWalletId,
            cryptoCurrencies = statuses.map(CryptoCurrencyStatus::currency),
        )
        val tokenList = if (statuses.isEmpty()) {
            TokenList.Empty
        } else {
            TokenList.Ungrouped(
                totalFiatBalance = TotalFiatBalance.Loading,
                sortedBy = TokensSortType.NONE,
                currencies = statuses.toList(),
            )
        }
        return AccountStatusList(
            userWalletId = userWalletId,
            accountStatuses = listOf(
                AccountStatus.Personal(account = account, tokenList = tokenList, priceChangeLce = lceLoading()),
            ),
            totalAccounts = 1,
            totalArchivedAccounts = 0,
            totalFiatBalance = TotalFiatBalance.Loading,
            sortType = TokensSortType.NONE,
            groupType = TokensGroupType.NONE,
        )
    }
}