package com.tangem.features.foryou.impl.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.foryou.impl.components.state.MarketChartUM
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class ForYouModelTest {

    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val multiAccountStatusListSupplier: MultiAccountStatusListSupplier = mockk()
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()

    private var model: ForYouModel? = null

    @BeforeEach
    fun setup() {
        // Default: a real, non-empty emission so the model's `getOrElse { Default }` mapping path is
        // actually exercised in every test, not bypassed by an empty flow.
        every { getSelectedAppCurrencyUseCase() } returns flowOf(AppCurrency.Default.right())
    }

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
    }

    @Nested
    inner class InitialState {

        @Test
        fun `GIVEN model created WHEN not yet advanced THEN uiState is Loading with skeleton rows`() = runTest {
            // Arrange
            every { userWalletsListRepository.selectedUserWallet } returns MutableStateFlow(null)
            every { multiAccountStatusListSupplier.invokeAsMap() } returns flowOf(linkedMapOf())

            // Act
            val model = createModel(testScope = this)

            // Assert — before advancing, the model exposes skeleton placeholder rows
            val loading = model.uiState.value.portfolioReviewUM as PortfolioReviewUM.Loading
            assertThat(loading.tokenList).hasSize(4)
            assertThat(loading.tokenList.all { it.tokenRowUM is TangemTokenRowUM.Loading }).isTrue()
            assertThat(loading.marketChartUM).isEqualTo(MarketChartUM.NoData)
        }
    }

    @Nested
    inner class ContentState {

        @Test
        fun `GIVEN selected wallet and statuses emitted WHEN advanced THEN uiState becomes Content`() = runTest {
            // Arrange
            val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC")
            stubSelectedWallet(
                currencies = listOf(createStatus(currency, loadedValue(BigDecimal("100")))),
                totalFiatBalance = BigDecimal("100"),
            )

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            val content = model.uiState.value.portfolioReviewUM as PortfolioReviewUM.Content
            assertThat(content.tokenList.map { it.tokenRowUM.id }).containsExactly("btc")
            assertThat(content.marketChartUM).isInstanceOf(MarketChartUM.Loaded::class.java)
            assertThat(model.uiState.value.notifications).isEmpty()
        }

        @Test
        fun `GIVEN total balance from outdated source WHEN advanced THEN outdated-data notification is shown`() =
            runTest {
                // Arrange
                val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC")
                stubSelectedWallet(
                    currencies = listOf(createStatus(currency, loadedValue(BigDecimal("100")))),
                    totalFiatBalance = BigDecimal("100"),
                    source = StatusSource.ONLY_CACHE,
                )

                // Act
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value.notifications).containsExactly(ForYouNotification.UsedOutdatedData)
            }
    }

    @Nested
    inner class ExpandClick {

        @Test
        fun `GIVEN asset row clicked WHEN clicked again THEN isExpanded toggles back to false`() = runTest {
            // Arrange
            val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC")
            stubSelectedWallet(
                currencies = listOf(createStatus(currency, loadedValue(BigDecimal("100")))),
                totalFiatBalance = BigDecimal("100"),
            )
            val model = createModel(testScope = this)
            advanceUntilIdle()
            val initialContent = model.uiState.value.portfolioReviewUM as PortfolioReviewUM.Content
            assertThat(initialContent.tokenList.single().isExpanded).isFalse()

            // Act — click once to expand
            initialContent.assetRow().onItemClick?.invoke()
            advanceUntilIdle()

            // Assert
            val expandedContent = model.uiState.value.portfolioReviewUM as PortfolioReviewUM.Content
            assertThat(expandedContent.tokenList.single().isExpanded).isTrue()

            // Act — click again to collapse
            expandedContent.assetRow().onItemClick?.invoke()
            advanceUntilIdle()

            // Assert
            val collapsedContent = model.uiState.value.portfolioReviewUM as PortfolioReviewUM.Content
            assertThat(collapsedContent.tokenList.single().isExpanded).isFalse()
        }
    }

    @Nested
    inner class PeriodClick {

        @Test
        fun `GIVEN Content state WHEN period clicked THEN initialSelectedItem updates without resetting rest`() =
            runTest {
                // Arrange
                val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC")
                stubSelectedWallet(
                    currencies = listOf(createStatus(currency, loadedValue(BigDecimal("100")))),
                    totalFiatBalance = BigDecimal("100"),
                )
                val model = createModel(testScope = this)
                advanceUntilIdle()
                val contentBefore = model.uiState.value.portfolioReviewUM as PortfolioReviewUM.Content
                val weekItem = contentBefore.periodPickerUM.items[1]

                // Act
                contentBefore.onPeriodClick(weekItem)

                // Assert
                val contentAfter = model.uiState.value.portfolioReviewUM as PortfolioReviewUM.Content
                assertThat(contentAfter.periodPickerUM.initialSelectedItem).isEqualTo(weekItem)
                assertThat(contentAfter.tokenList).isEqualTo(contentBefore.tokenList)
            }
    }

    private fun PortfolioReviewUM.Content.assetRow(): TangemTokenRowUM.Content =
        tokenList.single().tokenRowUM as TangemTokenRowUM.Content

    /** Wires the repository + supplier so the model derives Content from a single selected wallet. */
    private fun stubSelectedWallet(
        currencies: List<CryptoCurrencyStatus>,
        totalFiatBalance: BigDecimal,
        source: StatusSource = StatusSource.ACTUAL,
    ) {
        val wallet = MockUserWalletFactory.create().copy(walletId = UserWalletId("01"))
        every { userWalletsListRepository.selectedUserWallet } returns MutableStateFlow(wallet)
        every { multiAccountStatusListSupplier.invokeAsMap() } returns flowOf(
            linkedMapOf(
                wallet.walletId to createAccountStatusList(currencies, totalFiatBalance, source),
            ),
        )
    }

    private fun createModel(testScope: TestScope): ForYouModel {
        return ForYouModel(
            userWalletsListRepository = userWalletsListRepository,
            multiAccountStatusListSupplier = multiAccountStatusListSupplier,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
        ).also { model = it }
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }

    private fun createAccountStatusList(
        currencies: List<CryptoCurrencyStatus>,
        totalFiatBalance: BigDecimal,
        source: StatusSource = StatusSource.ACTUAL,
    ): AccountStatusList = mockk {
        every { flattenCurrencies() } returns currencies
        every { this@mockk.totalFiatBalance } returns TotalFiatBalance.Loaded(
            amount = totalFiatBalance,
            source = source,
        )
    }

    private fun createStatus(currency: CryptoCurrency, value: CryptoCurrencyStatus.Value) = CryptoCurrencyStatus(
        currency = currency,
        value = value,
    )

    private fun loadedValue(fiatAmount: BigDecimal): CryptoCurrencyStatus.Loaded = mockk {
        every { amount } returns BigDecimal.ONE
        every { this@mockk.fiatAmount } returns fiatAmount
        every { isError } returns false
        every { sources } returns CryptoCurrencyStatus.Sources()
    }

    private fun createCoin(rawCurrencyId: String, symbol: String): CryptoCurrency.Coin {
        val network: Network = mockk {
            every { name } returns "Network"
            every { isTestnet } returns false
            every { id } returns mockk { every { rawId } returns Network.RawID(rawCurrencyId) }
        }
        val currencyId: CryptoCurrency.ID = mockk {
            every { value } returns "coin-$rawCurrencyId"
            every { this@mockk.rawCurrencyId } returns CryptoCurrency.RawID(rawCurrencyId)
        }
        return mockk<CryptoCurrency.Coin> {
            every { this@mockk.id } returns currencyId
            every { this@mockk.symbol } returns symbol
            every { this@mockk.name } returns symbol
            every { this@mockk.network } returns network
            every { this@mockk.decimals } returns 8
            every { isCustom } returns false
            every { iconUrl } returns null
        }
    }
}