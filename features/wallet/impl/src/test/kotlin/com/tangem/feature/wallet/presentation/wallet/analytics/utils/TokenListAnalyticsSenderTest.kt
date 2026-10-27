package com.tangem.feature.wallet.presentation.wallet.analytics.utils

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.domain.analytics.CheckIsWalletToppedUpUseCase
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.utils.ScreenLifecycleProvider
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class TokenListAnalyticsSenderTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val checkIsWalletToppedUpUseCase: CheckIsWalletToppedUpUseCase = mockk()
    private val sentEvents = mutableListOf<AnalyticsEvent>()

    private val displayedState: WalletState = mockk {
        every { pullToRefreshConfig } returns PullToRefreshConfig(isRefreshing = false, onRefresh = {})
    }

    @BeforeEach
    fun setUp() {
        clearMocks(analyticsEventHandler, checkIsWalletToppedUpUseCase)
        sentEvents.clear()
        every { analyticsEventHandler.send(capture(sentEvents)) } returns Unit
        coEvery { checkIsWalletToppedUpUseCase(any(), any()) } returns false.right()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class BalanceLoaded {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN balance updates WHEN send THEN Balance Loaded is sent only when its params change`(
            model: BalanceLoadedModel,
        ) = runTest {
            // Arrange
            val sender = createSender()
            val wallet = createWallet(id = "01")

            // Act
            model.updates.forEach { (balance, tokensCount) ->
                sender.send(wallet = wallet, balance = balance, tokensCount = tokensCount)
            }

            // Assert
            assertThat(sentBalanceLoadedParams()).containsExactlyElementsIn(model.expected).inOrder()
        }

        @Test
        fun `GIVEN two wallets with the same balance WHEN send THEN Balance Loaded is sent for each`() = runTest {
            // Arrange
            val sender = createSender()
            val balance = TotalFiatBalance.Loaded(amount = BigDecimal.ONE, source = StatusSource.ACTUAL)

            // Act
            sender.send(wallet = createWallet(id = "01"), balance = balance, tokensCount = 2)
            sender.send(wallet = createWallet(id = "02"), balance = balance, tokensCount = 2)

            // Assert
            assertThat(sentBalanceLoadedParams()).containsExactly(params(full, 2), params(full, 2))
        }

        private fun provideTestModels() = listOf(
            BalanceLoadedModel(
                updates = listOf(loaded(BigDecimal.ZERO, StatusSource.ACTUAL) to 2),
                expected = listOf(params(empty, 2)),
            ),
            BalanceLoadedModel(
                updates = listOf(
                    loaded(BigDecimal.ZERO, StatusSource.ACTUAL) to 2,
                    loaded(BigDecimal.ZERO, StatusSource.ONLY_CACHE) to 2,
                ),
                expected = listOf(params(empty, 2)),
            ),
            BalanceLoadedModel(
                updates = listOf(
                    loaded(BigDecimal.ONE, StatusSource.ACTUAL) to 3,
                    loaded(BigDecimal.TEN, StatusSource.ACTUAL) to 3,
                ),
                expected = listOf(params(full, 3)),
            ),
            BalanceLoadedModel(
                updates = listOf(
                    loaded(BigDecimal.ZERO, StatusSource.ACTUAL) to 2,
                    loaded(BigDecimal.ONE, StatusSource.ACTUAL) to 2,
                ),
                expected = listOf(params(empty, 2), params(full, 2)),
            ),
            BalanceLoadedModel(
                updates = listOf(
                    loaded(BigDecimal.ONE, StatusSource.ACTUAL) to 2,
                    loaded(BigDecimal.ONE, StatusSource.ACTUAL) to 3,
                ),
                expected = listOf(params(full, 2), params(full, 3)),
            ),
            BalanceLoadedModel(
                updates = listOf(
                    TotalFiatBalance.Failed to 2,
                    loaded(BigDecimal.ONE, StatusSource.ACTUAL) to 2,
                    TotalFiatBalance.Failed to 2,
                ),
                expected = listOf(params(blockchainError, 2), params(full, 2), params(blockchainError, 2)),
            ),
        )
    }

    private fun createSender() = TokenListAnalyticsSender(
        analyticsEventHandler = analyticsEventHandler,
        checkIsWalletToppedUpUseCase = checkIsWalletToppedUpUseCase,
        screenLifecycleProvider = ScreenLifecycleProvider(),
    )

    private suspend fun TokenListAnalyticsSender.send(wallet: UserWallet, balance: TotalFiatBalance, tokensCount: Int) {
        send(
            displayedUiState = displayedState,
            userWallet = wallet,
            totalFiatBalance = balance,
            flattenCurrencies = createStatuses(count = tokensCount),
        )
    }

    private fun createWallet(id: String): UserWallet = mockk {
        every { walletId } returns UserWalletId(id)
    }

    private fun createStatuses(count: Int): List<CryptoCurrencyStatus> = List(count) {
        mockk { every { value } returns CryptoCurrencyStatus.Loading }
    }

    private fun sentBalanceLoadedParams(): List<Map<String, String>> {
        return sentEvents.filterIsInstance<Basic.BalanceLoaded>().map { it.params }
    }

    private fun loaded(amount: BigDecimal, source: StatusSource) = TotalFiatBalance.Loaded(amount, source)

    private fun params(balance: AnalyticsParam.CardBalanceState, tokensCount: Int) = mapOf(
        AnalyticsParam.BALANCE to balance.value,
        AnalyticsParam.TOKENS_COUNT to tokensCount.toString(),
    )

    data class BalanceLoadedModel(
        val updates: List<Pair<TotalFiatBalance, Int>>,
        val expected: List<Map<String, String>>,
    )

    private companion object {
        val empty = AnalyticsParam.CardBalanceState.Empty
        val full = AnalyticsParam.CardBalanceState.Full
        val blockchainError = AnalyticsParam.CardBalanceState.BlockchainError
    }
}