package com.tangem.features.foryou.impl.model.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.features.commonfeatures.api.choosetoken.model.WalletListUM
import com.tangem.features.foryou.impl.entity.ForYouTokenListItemUM
import com.tangem.features.foryou.impl.entity.ForYouUM
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class SetPortfolioReviewTransformerTest {

    private val appCurrency: AppCurrency = AppCurrency.Default
    private val walletListUM = WalletListUM(items = persistentListOf())

    @Nested
    inner class Transform {

        @Test
        fun `GIVEN currency with zero fiat balance WHEN transform THEN it is dropped from asset count`() {
            // Arrange
            val zeroBalance = createCurrency(rawCurrencyId = "btc", symbol = "BTC")
            val nonZeroBalance = createCurrency(rawCurrencyId = "eth", symbol = "ETH")
            val currencies = listOf(
                createStatus(zeroBalance, loadedValue(BigDecimal.ZERO)),
                createStatus(nonZeroBalance, loadedValue(BigDecimal("100"))),
            )
            val transformer = createTransformer(currencies = currencies, totalFiatBalance = BigDecimal("100"))

            // Act
            val result = transformer.transform(loadingState()).portfolioReviewUM as PortfolioReviewUM.Content

            // Assert
            assertThat(result.assetCount).isEqualTo(stringReference("1 assets"))
        }

        @Test
        fun `GIVEN assets across networks WHEN transform THEN they are aggregated and ranked by summed fiat`() {
            // Arrange — same asset (rawCurrencyId "usdc") on two networks aggregates into one asset
            val onEth = createCurrency(rawCurrencyId = "usdc", symbol = "USDC")
            val onSol = createCurrency(rawCurrencyId = "usdc", symbol = "USDC")
            val other = createCurrency(rawCurrencyId = "btc", symbol = "BTC")
            val currencies = listOf(
                createStatus(onEth, loadedValue(BigDecimal("50"))),
                createStatus(onSol, loadedValue(BigDecimal("60"))),
                createStatus(other, loadedValue(BigDecimal("10"))),
            )
            val transformer = createTransformer(currencies = currencies, totalFiatBalance = BigDecimal("120"))

            // Act
            val result = transformer.transform(loadingState()).portfolioReviewUM as PortfolioReviewUM.Content

            // Assert — 2 ranked assets: usdc (110 total) and btc (10)
            assertThat(result.assetCount).isEqualTo(stringReference("2 assets"))
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
            val transformer = createTransformer(currencies = currencies, totalFiatBalance = BigDecimal("470"))

            // Act
            val result = transformer.transform(loadingState()).portfolioReviewUM as PortfolioReviewUM.Content

            // Assert — tokenList has 4 top asset rows + 1 "Other" row = 5 items
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
            val transformer = createTransformer(currencies = currencies, totalFiatBalance = BigDecimal("394"))

            // Act
            val result = transformer.transform(loadingState()).portfolioReviewUM as PortfolioReviewUM.Content

            // Assert
            assertThat(result.tokenList).hasSize(4)
        }

        @Test
        fun `GIVEN total and top balance non-zero WHEN transform THEN topHoldingPercent is computed`() {
            // Arrange — a single asset means top balance == total balance == 100%
            val currency = createCurrency(rawCurrencyId = "btc", symbol = "BTC")
            val currencies = listOf(createStatus(currency, loadedValue(BigDecimal("100"))))
            val transformer = createTransformer(currencies = currencies, totalFiatBalance = BigDecimal("100"))

            // Act
            val result = transformer.transform(loadingState()).portfolioReviewUM as PortfolioReviewUM.Content

            // Assert
            assertThat(result.topHoldingPercent).isEqualTo(stringReference("Top holding 100.00%"))
        }

        @Test
        fun `GIVEN zero total fiat balance WHEN transform THEN topHoldingPercent is DASH_SIGN`() {
            // Arrange
            val currency = createCurrency(rawCurrencyId = "btc", symbol = "BTC")
            val currencies = listOf(createStatus(currency, loadedValue(BigDecimal.ZERO)))
            val transformer = createTransformer(currencies = currencies, totalFiatBalance = BigDecimal.ZERO)

            // Act
            val result = transformer.transform(loadingState()).portfolioReviewUM as PortfolioReviewUM.Content

            // Assert
            assertThat(result.topHoldingPercent).isEqualTo(stringReference("Top holding —"))
        }

        @Test
        fun `GIVEN prev state is Loading WHEN transform THEN period picker is freshly created with Day selected`() {
            // Arrange
            val currency = createCurrency(rawCurrencyId = "btc", symbol = "BTC")
            val currencies = listOf(createStatus(currency, loadedValue(BigDecimal("10"))))
            val transformer = createTransformer(currencies = currencies, totalFiatBalance = BigDecimal("10"))

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
            val currency = createCurrency(rawCurrencyId = "btc", symbol = "BTC")
            val currencies = listOf(createStatus(currency, loadedValue(BigDecimal("10"))))
            val transformer = createTransformer(currencies = currencies, totalFiatBalance = BigDecimal("10"))
            val prevContentState = transformer.transform(loadingState()).portfolioReviewUM as PortfolioReviewUM.Content
            val weekItem = prevContentState.periodPickerUM.items[1]
            val prevWithWeekSelected = prevContentState.copy(
                periodPickerUM = prevContentState.periodPickerUM.copy(initialSelectedItem = weekItem),
            )
            val prevState = ForYouUM(walletListUM = walletListUM, portfolioReviewUM = prevWithWeekSelected)

            // Act
            val result = transformer.transform(prevState).portfolioReviewUM as PortfolioReviewUM.Content

            // Assert
            assertThat(result.periodPickerUM.initialSelectedItem).isEqualTo(weekItem)
        }

        @Test
        fun `GIVEN new state WHEN transform THEN walletListUM is applied from constructor`() {
            // Arrange
            val currency = createCurrency(rawCurrencyId = "btc", symbol = "BTC")
            val currencies = listOf(createStatus(currency, loadedValue(BigDecimal("10"))))
            val newWalletListUM = WalletListUM(items = persistentListOf())
            val transformer = SetPortfolioReviewTransformer(
                walletListUM = newWalletListUM,
                currencies = currencies,
                totalFiatBalance = BigDecimal("10"),
                appCurrency = appCurrency,
                expandedAssetIds = emptySet(),
                expandClick = {},
                onPeriodClick = {},
            )

            // Act
            val result = transformer.transform(loadingState())

            // Assert
            assertThat(result.walletListUM).isSameInstanceAs(newWalletListUM)
        }
    }

    private fun createTransformer(
        currencies: List<CryptoCurrencyStatus>,
        totalFiatBalance: BigDecimal,
        expandedAssetIds: Set<String> = emptySet(),
    ) = SetPortfolioReviewTransformer(
        walletListUM = walletListUM,
        currencies = currencies,
        totalFiatBalance = totalFiatBalance,
        appCurrency = appCurrency,
        expandedAssetIds = expandedAssetIds,
        expandClick = {},
        onPeriodClick = {},
    )

    private fun loadingState(): ForYouUM = ForYouUM(
        walletListUM = walletListUM,
        portfolioReviewUM = PortfolioReviewUM.Loading(tokenList = persistentListOf<ForYouTokenListItemUM>()),
    )

    private fun createStatus(currency: CryptoCurrency, value: CryptoCurrencyStatus.Value) = CryptoCurrencyStatus(
        currency = currency,
        value = value,
    )

    private fun loadedValue(fiatAmount: BigDecimal): CryptoCurrencyStatus.Loaded = mockk {
        every { amount } returns BigDecimal.ONE
        every { this@mockk.fiatAmount } returns fiatAmount
        every { isError } returns false
    }

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
            every { this@mockk.network } returns network
            every { this@mockk.decimals } returns 8
            every { isCustom } returns false
            every { iconUrl } returns null
        }
    }
}