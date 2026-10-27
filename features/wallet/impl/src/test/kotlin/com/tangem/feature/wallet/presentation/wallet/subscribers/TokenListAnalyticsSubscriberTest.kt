package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.TokenListAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class TokenListAnalyticsSubscriberTest {

    private val userWallet: UserWallet = mockk { every { walletId } returns UserWalletId("01") }
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val stateHolder: WalletStateController = mockk(relaxed = true)
    private val tokenListAnalyticsSender: TokenListAnalyticsSender = mockk(relaxed = true)

    private val balance = TotalFiatBalance.Loaded(amount = BigDecimal.ONE, source = StatusSource.ACTUAL)

    @BeforeEach
    fun resetMocks() {
        clearMocks(singleAccountStatusListSupplier, stateHolder, tokenListAnalyticsSender)
    }

    @Test
    fun `GIVEN only currencies count changes WHEN collected THEN sender receives both updates`() = runTest {
        // Arrange
        val accountStatusLists = MutableStateFlow(createAccountStatusList(currenciesCount = 2))
        every { singleAccountStatusListSupplier(params = any()) } returns accountStatusLists
        collectSubscriber()

        // Act
        accountStatusLists.value = createAccountStatusList(currenciesCount = 1)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) {
            tokenListAnalyticsSender.send(
                displayedUiState = any(),
                userWallet = any(),
                totalFiatBalance = balance,
                flattenCurrencies = match { it.size == 2 },
            )
        }
        coVerify(exactly = 1) {
            tokenListAnalyticsSender.send(
                displayedUiState = any(),
                userWallet = any(),
                totalFiatBalance = balance,
                flattenCurrencies = match { it.size == 1 },
            )
        }
    }

    @Test
    fun `GIVEN balance and currencies count unchanged WHEN collected THEN sender receives one update`() = runTest {
        // Arrange
        val accountStatusLists = MutableStateFlow(createAccountStatusList(currenciesCount = 2))
        every { singleAccountStatusListSupplier(params = any()) } returns accountStatusLists
        collectSubscriber()

        // Act
        accountStatusLists.value = createAccountStatusList(currenciesCount = 2)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) {
            tokenListAnalyticsSender.send(
                displayedUiState = any(),
                userWallet = any(),
                totalFiatBalance = any(),
                flattenCurrencies = any(),
            )
        }
    }

    private fun TestScope.collectSubscriber() {
        TokenListAnalyticsSubscriber(
            userWallet = userWallet,
            singleAccountStatusListSupplier = singleAccountStatusListSupplier,
            stateHolder = stateHolder,
            tokenListAnalyticsSender = tokenListAnalyticsSender,
        ).subscribe(
            coroutineScope = backgroundScope + UnconfinedTestDispatcher(testScheduler),
            dispatchers = UnconfinedTestDispatcher(testScheduler),
        )
    }

    private fun createAccountStatusList(currenciesCount: Int): AccountStatusList {
        val currencies = List(currenciesCount) { mockk<CryptoCurrencyStatus>() }
        return mockk {
            every { totalFiatBalance } returns balance
            every { flattenCurrencies() } returns currencies
        }
    }
}