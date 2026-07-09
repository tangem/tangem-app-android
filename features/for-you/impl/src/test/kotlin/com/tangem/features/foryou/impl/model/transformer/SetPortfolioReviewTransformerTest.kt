package com.tangem.features.foryou.impl.model.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.features.foryou.impl.components.state.MarketChartUM
import com.tangem.features.foryou.impl.entity.ForYouTokenListItemUM
import com.tangem.features.foryou.impl.entity.ForYouUM
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM
import com.tangem.features.foryou.impl.model.ForYouNotification
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class SetPortfolioReviewTransformerTest {

    private val appCurrency: AppCurrency = AppCurrency.Default

    @Nested
    inner class TokenList {

        @Test
        fun `GIVEN currency with resolved zero fiat balance WHEN transform THEN it is dropped from the list`() {
            // Arrange
            val zeroBalance = createCurrency(rawCurrencyId = "btc", symbol = "BTC")
            val nonZeroBalance = createCurrency(rawCurrencyId = "eth", symbol = "ETH")
            val currencies = listOf(
                createStatus(zeroBalance, loadedValue(BigDecimal.ZERO)),
                createStatus(nonZeroBalance, loadedValue(BigDecimal("100"))),
            )
            val transformer = createTransformer(accountStatusList(currencies, loaded(BigDecimal("100"))))

            // Act
            val result = transformer.transform(loadingState()).portfolioReviewUM as PortfolioReviewUM.Content

            // Assert — only the ETH asset survives; the zero-fiat BTC is dropped
            assertThat(result.tokenList.map { it.tokenRowUM.id }).containsExactly("eth")
        }

        @Test
        fun `GIVEN non-content status with null fiat WHEN transform THEN it is kept not dropped`() {
            // Arrange — a non-content status (Unreachable) carries a null fiatAmount, not a resolved zero;
            // it must still be shown so the user sees the token they hold, with the appropriate treatment.
            val unreachable = createCurrency(rawCurrencyId = "btc", symbol = "BTC")
            val loaded = createCurrency(rawCurrencyId = "eth", symbol = "ETH")
            val currencies = listOf(
                createStatus(unreachable, unreachableValue()),
                createStatus(loaded, loadedValue(BigDecimal("100"))),
            )
            val transformer = createTransformer(accountStatusList(currencies, loaded(BigDecimal("100"))))

            // Act
            val result = transformer.transform(loadingState()).portfolioReviewUM as PortfolioReviewUM.Content

            // Assert — both assets kept, ranked by summed fiat (eth 100 > btc 0)
            assertThat(result.tokenList.map { it.tokenRowUM.id }).containsExactly("eth", "btc").inOrder()
        }

        @Test
        fun `GIVEN same asset across networks WHEN transform THEN aggregated into one asset ranked by summed fiat`() {
            // Arrange — the same asset (shared rawCurrencyId "usdc") aggregates into one asset
            val onEth = createCurrency(rawCurrencyId = "usdc", symbol = "USDC")
            val onSol = createCurrency(rawCurrencyId = "usdc", symbol = "USDC")
            val other = createCurrency(rawCurrencyId = "btc", symbol = "BTC")
            val currencies = listOf(
                createStatus(onEth, loadedValue(BigDecimal("50"))),
                createStatus(onSol, loadedValue(BigDecimal("60"))),
                createStatus(other, loadedValue(BigDecimal("10"))),
            )
            val transformer = createTransformer(accountStatusList(currencies, loaded(BigDecimal("120"))))

            // Act
            val result = transformer.transform(loadingState()).portfolioReviewUM as PortfolioReviewUM.Content

            // Assert — 2 ranked assets: usdc (110 total) ahead of btc (10)
            assertThat(result.tokenList.map { it.tokenRowUM.id }).containsExactly("usdc", "btc").inOrder()
        }

        @Test
        fun `GIVEN more than TOP_HOLDINGS_COUNT assets WHEN transform THEN excess assets collapse into Other`() {
            // Arrange — 5 distinct assets, top 4 kept individually, 5th collapsed into "Other"
            val currencies = (1..5).map { index ->
                createStatus(
                    createCurrency(rawCurrencyId = "asset-$index", symbol = "A$index"),
                    loadedValue(BigDecimal(100 - index)),
                )
            }
            val transformer = createTransformer(accountStatusList(currencies, loaded(BigDecimal("470"))))

            // Act
            val result = transformer.transform(loadingState()).portfolioReviewUM as PortfolioReviewUM.Content

            // Assert — 4 top asset rows + 1 "Other" row
            assertThat(result.tokenList).hasSize(5)
            assertThat(result.tokenList.last().tokenRowUM.id).isEqualTo("for_you_other_assets")
        }

        @Test
        fun `GIVEN exactly TOP_HOLDINGS_COUNT assets WHEN transform THEN no Other row is appended`() {
            // Arrange
            val currencies = (1..4).map { index ->
                createStatus(
                    createCurrency(rawCurrencyId = "asset-$index", symbol = "A$index"),
                    loadedValue(BigDecimal(100 - index)),
                )
            }
            val transformer = createTransformer(accountStatusList(currencies, loaded(BigDecimal("394"))))

            // Act
            val result = transformer.transform(loadingState()).portfolioReviewUM as PortfolioReviewUM.Content

            // Assert
            assertThat(result.tokenList).hasSize(4)
        }

        @Test
        fun `GIVEN null account status list WHEN transform THEN token list is empty`() {
            // Arrange
            val transformer = createTransformer(accountStatusList = null)

            // Act
            val result = transformer.transform(loadingState()).portfolioReviewUM as PortfolioReviewUM.Content

            // Assert
            assertThat(result.tokenList).isEmpty()
        }
    }

    @Nested
    inner class MarketChart {

        @Test
        fun `GIVEN loaded total balance WHEN transform THEN market chart is Loaded with one segment per top asset`() {
            // Arrange
            val currencies = listOf(
                createStatus(createCurrency(rawCurrencyId = "btc", symbol = "BTC"), loadedValue(BigDecimal("70"))),
                createStatus(createCurrency(rawCurrencyId = "eth", symbol = "ETH"), loadedValue(BigDecimal("30"))),
            )
            val transformer = createTransformer(accountStatusList(currencies, loaded(BigDecimal("100"))))

            // Act
            val result = transformer.transform(loadingState()).portfolioReviewUM as PortfolioReviewUM.Content

            // Assert
            val marketChart = result.marketChartUM as MarketChartUM.Loaded
            assertThat(marketChart.assetCount).isEqualTo(2)
        }

        @Test
        fun `GIVEN non-loaded total balance WHEN transform THEN market chart is NoData`() {
            // Arrange
            val currencies = listOf(
                createStatus(createCurrency(rawCurrencyId = "btc", symbol = "BTC"), loadedValue(BigDecimal("100"))),
            )
            val transformer = createTransformer(accountStatusList(currencies, TotalFiatBalance.Loading))

            // Act
            val result = transformer.transform(loadingState()).portfolioReviewUM as PortfolioReviewUM.Content

            // Assert
            assertThat(result.marketChartUM).isEqualTo(MarketChartUM.NoData)
        }

        @Test
        fun `GIVEN null account status list WHEN transform THEN market chart is NoData`() {
            // Arrange
            val transformer = createTransformer(accountStatusList = null)

            // Act
            val result = transformer.transform(loadingState()).portfolioReviewUM as PortfolioReviewUM.Content

            // Assert
            assertThat(result.marketChartUM).isEqualTo(MarketChartUM.NoData)
        }
    }

    @Nested
    inner class PeriodPicker {

        @Test
        fun `GIVEN prev state is Loading WHEN transform THEN period picker is freshly created with Day selected`() {
            // Arrange
            val currencies = listOf(
                createStatus(createCurrency(rawCurrencyId = "btc", symbol = "BTC"), loadedValue(BigDecimal("10"))),
            )
            val transformer = createTransformer(accountStatusList(currencies, loaded(BigDecimal("10"))))

            // Act
            val result = transformer.transform(loadingState()).portfolioReviewUM as PortfolioReviewUM.Content

            // Assert
            assertThat(result.periodPickerUM.items.map { it.title }).containsExactly(
                stringReference("Day"),
                stringReference("Week"),
                stringReference("Month"),
            ).inOrder()
            assertThat(result.periodPickerUM.initialSelectedItem?.title).isEqualTo(stringReference("Day"))
        }

        @Test
        fun `GIVEN prev state is Content WHEN transform THEN period picker selection is preserved`() {
            // Arrange
            val currencies = listOf(
                createStatus(createCurrency(rawCurrencyId = "btc", symbol = "BTC"), loadedValue(BigDecimal("10"))),
            )
            val transformer = createTransformer(accountStatusList(currencies, loaded(BigDecimal("10"))))
            val prevContent = transformer.transform(loadingState()).portfolioReviewUM as PortfolioReviewUM.Content
            val weekItem = prevContent.periodPickerUM.items[1]
            val prevState = loadingState().copy(
                portfolioReviewUM = prevContent.copy(
                    periodPickerUM = prevContent.periodPickerUM.copy(initialSelectedItem = weekItem),
                ),
            )

            // Act
            val result = transformer.transform(prevState).portfolioReviewUM as PortfolioReviewUM.Content

            // Assert
            assertThat(result.periodPickerUM.initialSelectedItem).isEqualTo(weekItem)
        }
    }

    @Nested
    inner class Notifications {

        @Test
        fun `GIVEN total balance from outdated source WHEN transform THEN outdated-data notification is emitted`() {
            // Arrange
            val currencies = listOf(
                createStatus(createCurrency(rawCurrencyId = "btc", symbol = "BTC"), loadedValue(BigDecimal("10"))),
            )
            val transformer = createTransformer(
                accountStatusList(currencies, loaded(BigDecimal("10"), source = StatusSource.ONLY_CACHE)),
            )

            // Act
            val result = transformer.transform(loadingState())

            // Assert
            assertThat(result.notifications).containsExactly(ForYouNotification.UsedOutdatedData)
        }

        @Test
        fun `GIVEN total balance from actual source WHEN transform THEN no notification is emitted`() {
            // Arrange
            val currencies = listOf(
                createStatus(createCurrency(rawCurrencyId = "btc", symbol = "BTC"), loadedValue(BigDecimal("10"))),
            )
            val transformer = createTransformer(
                accountStatusList(currencies, loaded(BigDecimal("10"), source = StatusSource.ACTUAL)),
            )

            // Act
            val result = transformer.transform(loadingState())

            // Assert
            assertThat(result.notifications).isEmpty()
        }

        @Test
        fun `GIVEN null account status list WHEN transform THEN no notification is emitted`() {
            // Arrange
            val transformer = createTransformer(accountStatusList = null)

            // Act
            val result = transformer.transform(loadingState())

            // Assert
            assertThat(result.notifications).isEmpty()
        }
    }

    private fun createTransformer(
        accountStatusList: AccountStatusList?,
        expandedAssetIds: Set<String> = emptySet(),
    ) = SetPortfolioReviewTransformer(
        accountStatusList = accountStatusList,
        appCurrency = appCurrency,
        expandedAssetIds = expandedAssetIds,
        expandClick = {},
        onPeriodClick = {},
    )

    private fun accountStatusList(
        currencies: List<CryptoCurrencyStatus>,
        totalFiatBalance: TotalFiatBalance,
    ): AccountStatusList = mockk {
        every { flattenCurrencies() } returns currencies
        every { this@mockk.totalFiatBalance } returns totalFiatBalance
    }

    private fun loaded(amount: BigDecimal, source: StatusSource = StatusSource.ACTUAL): TotalFiatBalance.Loaded =
        TotalFiatBalance.Loaded(amount = amount, source = source)

    private fun loadingState(): ForYouUM = ForYouUM(
        portfolioReviewUM = PortfolioReviewUM.Loading(
            tokenList = persistentListOf<ForYouTokenListItemUM>(),
            marketChartUM = MarketChartUM.NoData,
        ),
        notifications = persistentListOf(),
    )

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

    /** A non-content status: carries a null fiatAmount (unknown balance), not a resolved zero. */
    private fun unreachableValue(): CryptoCurrencyStatus.Unreachable = CryptoCurrencyStatus.Unreachable(
        priceChange = null,
        fiatRate = null,
        networkAddress = null,
    )

    private fun createCurrency(rawCurrencyId: String, symbol: String): CryptoCurrency.Coin {
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