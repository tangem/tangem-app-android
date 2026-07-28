package com.tangem.features.foryou.impl.model.converter.portfolioReview

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.foryou.impl.model.converter.toForYouPercent
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class ForYouPortfolioReviewTokenRowConverterTest {

    private val appCurrency: AppCurrency = AppCurrency.Default

    @Nested
    inner class Convert {

        @Test
        fun `GIVEN all statuses Loading WHEN convert THEN row is Loading with representative id`() {
            // Arrange
            val currency = createCurrency(id = "coin-eth", symbol = "ETH")
            val statuses = listOf(createStatus(currency, CryptoCurrencyStatus.Loading))
            val converter = createConverter(totalFiatBalance = BigDecimal("1000"))

            // Act
            val result = converter.convert(statuses)

            // Assert
            assertThat(result).isEqualTo(TangemTokenRowUM.Loading(id = "coin-eth"))
        }

        @Test
        fun `GIVEN single loaded status WHEN convert THEN row is Content with its amounts`() {
            // Arrange
            val currency = createCurrency(id = "coin-eth", symbol = "ETH", networkName = "Ethereum")
            val statuses = listOf(
                createStatus(currency, loadedValue(amount = BigDecimal("2"), fiatAmount = BigDecimal("400"))),
            )
            val converter = createConverter(totalFiatBalance = BigDecimal("1000"))

            // Act
            val result = converter.convert(statuses) as TangemTokenRowUM.Content

            // Assert
            assertThat(result.id).isEqualTo("coin-eth")
            val topEnd = result.topEndContentUM as TangemTokenRowUM.EndContentUM.Content
            val bottomEnd = result.bottomEndContentUM as TangemTokenRowUM.EndContentUM.Content
            assertThat(topEnd.text).isEqualTo(BigDecimal("400").expectedFiatText())
            assertThat(bottomEnd.text).isEqualTo(BigDecimal("400").expectedPercentText(BigDecimal("1000")))
        }

        @Test
        fun `GIVEN several statuses of the same asset on one network WHEN convert THEN amounts are summed`() {
            // Arrange — same asset held in two accounts on the same network aggregates into one row
            val currency = createCurrency(id = "coin-eth", symbol = "ETH", networkName = "Ethereum")
            val statuses = listOf(
                createStatus(currency, loadedValue(amount = BigDecimal("1"), fiatAmount = BigDecimal("200"))),
                createStatus(currency, loadedValue(amount = BigDecimal("2"), fiatAmount = BigDecimal("400"))),
            )
            val converter = createConverter(totalFiatBalance = BigDecimal("1000"))

            // Act
            val result = converter.convert(statuses) as TangemTokenRowUM.Content

            // Assert
            val topEnd = result.topEndContentUM as TangemTokenRowUM.EndContentUM.Content
            assertThat(topEnd.text).isEqualTo(BigDecimal("600").expectedFiatText())
        }

        @Test
        fun `GIVEN mixed Loading and Loaded statuses WHEN convert THEN row is Content`() {
            // Arrange — not *all* statuses are Loading, so it should not collapse to a Loading row
            val currency = createCurrency(id = "coin-eth", symbol = "ETH")
            val statuses = listOf(
                createStatus(currency, CryptoCurrencyStatus.Loading),
                createStatus(currency, loadedValue(amount = BigDecimal("1"), fiatAmount = BigDecimal("100"))),
            )
            val converter = createConverter(totalFiatBalance = BigDecimal("1000"))

            // Act
            val result = converter.convert(statuses)

            // Assert
            assertThat(result).isInstanceOf(TangemTokenRowUM.Content::class.java)
        }

        @Test
        fun `GIVEN loaded status from cache WHEN convert THEN content flickers`() {
            // Arrange
            val currency = createCurrency(id = "coin-eth", symbol = "ETH", networkName = "Ethereum")
            val statuses = listOf(
                createStatus(
                    currency,
                    loadedValue(amount = BigDecimal("1"), fiatAmount = BigDecimal("100"), source = StatusSource.CACHE),
                ),
            )
            val converter = createConverter(totalFiatBalance = BigDecimal("1000"))

            // Act
            val result = converter.convert(statuses) as TangemTokenRowUM.Content

            // Assert
            val topEnd = result.topEndContentUM as TangemTokenRowUM.EndContentUM.Content
            val bottomEnd = result.bottomEndContentUM as TangemTokenRowUM.EndContentUM.Content
            assertThat(topEnd.isFlickering).isTrue()
            assertThat(bottomEnd.isFlickering).isTrue()
            assertThat(topEnd.startIcons).isEmpty()
        }

        @Test
        fun `GIVEN loaded status only-cache WHEN convert THEN error-sync start icon shown`() {
            // Arrange
            val currency = createCurrency(id = "coin-eth", symbol = "ETH", networkName = "Ethereum")
            val statuses = listOf(
                createStatus(
                    currency,
                    loadedValue(
                        amount = BigDecimal("1"),
                        fiatAmount = BigDecimal("100"),
                        source = StatusSource.ONLY_CACHE,
                    ),
                ),
            )
            val converter = createConverter(totalFiatBalance = BigDecimal("1000"))

            // Act
            val result = converter.convert(statuses) as TangemTokenRowUM.Content

            // Assert
            val topEnd = result.topEndContentUM as TangemTokenRowUM.EndContentUM.Content
            assertThat(topEnd.isFlickering).isFalse()
            assertThat(topEnd.startIcons).hasSize(1)
        }

        @Test
        fun `GIVEN missed derivation status WHEN convert THEN no-address treatment`() {
            // Arrange
            val currency = createCurrency(id = "coin-eth", symbol = "ETH", networkName = "Ethereum")
            val statuses = listOf(createStatus(currency, missedDerivationValue()))
            val converter = createConverter(totalFiatBalance = BigDecimal("1000"))

            // Act
            val result = converter.convert(statuses) as TangemTokenRowUM.Content

            // Assert — top-end is a dash, bottom-end carries the attention "no address" icon
            val topEnd = result.topEndContentUM as TangemTokenRowUM.EndContentUM.Content
            val bottomEnd = result.bottomEndContentUM as TangemTokenRowUM.EndContentUM.Content
            assertThat(topEnd.endIcons).isEmpty()
            assertThat(bottomEnd.endIcons).hasSize(1)
        }

        @Test
        fun `GIVEN unreachable status WHEN convert THEN dash on top and attention icon on bottom`() {
            // Arrange
            val currency = createCurrency(id = "coin-eth", symbol = "ETH", networkName = "Ethereum")
            val statuses = listOf(createStatus(currency, unreachableValue()))
            val converter = createConverter(totalFiatBalance = BigDecimal("1000"))

            // Act
            val result = converter.convert(statuses) as TangemTokenRowUM.Content

            // Assert — top-end is a bare dash, the attention "unreachable" icon lives on the bottom end
            val topEnd = result.topEndContentUM as TangemTokenRowUM.EndContentUM.Content
            val bottomEnd = result.bottomEndContentUM as TangemTokenRowUM.EndContentUM.Content
            assertThat(topEnd.endIcons).isEmpty()
            assertThat(bottomEnd.endIcons).hasSize(1)
        }

        @Test
        fun `GIVEN mixed Loaded and Unreachable WHEN convert THEN collapses to unreachable`() {
            // Arrange — one account resolved, another unreachable: the row must surface the error state
            val currency = createCurrency(id = "coin-eth", symbol = "ETH", networkName = "Ethereum")
            val statuses = listOf(
                createStatus(currency, loadedValue(amount = BigDecimal("1"), fiatAmount = BigDecimal("100"))),
                createStatus(currency, unreachableValue()),
            )
            val converter = createConverter(totalFiatBalance = BigDecimal("1000"))

            // Act
            val result = converter.convert(statuses) as TangemTokenRowUM.Content

            // Assert — the unreachable treatment (attention icon on the bottom end) wins over the loaded amount
            val bottomEnd = result.bottomEndContentUM as TangemTokenRowUM.EndContentUM.Content
            assertThat(bottomEnd.endIcons).hasSize(1)
        }

        @Test
        fun `GIVEN wallet id present WHEN row clicked THEN token callback receives wallet id and currency`() {
            // Arrange
            val currency = createCurrency(id = "coin-eth", symbol = "ETH")
            val statuses = listOf(
                createStatus(currency, loadedValue(amount = BigDecimal("1"), fiatAmount = BigDecimal("100"))),
            )
            val walletId = UserWalletId("01")
            var clicked: Pair<UserWalletId, CryptoCurrency>? = null
            val converter = createConverter(
                totalFiatBalance = BigDecimal("1000"),
                userWalletId = walletId,
                onTokenClick = { id, clickedCurrency -> clicked = id to clickedCurrency },
            )

            // Act
            val result = converter.convert(statuses) as TangemTokenRowUM.Content
            result.onItemClick?.invoke()

            // Assert
            assertThat(clicked).isEqualTo(walletId to currency)
        }

        @Test
        fun `GIVEN no wallet id WHEN row clicked THEN token callback is not invoked`() {
            // Arrange — without a selected wallet there is nowhere to navigate, so the click is a no-op
            val currency = createCurrency(id = "coin-eth", symbol = "ETH")
            val statuses = listOf(
                createStatus(currency, loadedValue(amount = BigDecimal("1"), fiatAmount = BigDecimal("100"))),
            )
            var clicked = false
            val converter = createConverter(
                totalFiatBalance = BigDecimal("1000"),
                userWalletId = null,
                onTokenClick = { _, _ -> clicked = true },
            )

            // Act
            val result = converter.convert(statuses) as TangemTokenRowUM.Content
            result.onItemClick?.invoke()

            // Assert
            assertThat(clicked).isFalse()
        }

        @Test
        fun `GIVEN mixed MissedDerivation and Unreachable WHEN convert THEN missed-derivation wins`() {
            // Arrange — missed derivation is the most severe terminal state and dominates
            val currency = createCurrency(id = "coin-eth", symbol = "ETH", networkName = "Ethereum")
            val statuses = listOf(
                createStatus(currency, unreachableValue()),
                createStatus(currency, missedDerivationValue()),
            )
            val converter = createConverter(totalFiatBalance = BigDecimal("1000"))

            // Act
            val result = converter.convert(statuses) as TangemTokenRowUM.Content

            // Assert — top-end is a dash (no-address treatment), not an unreachable label
            val topEnd = result.topEndContentUM as TangemTokenRowUM.EndContentUM.Content
            assertThat(topEnd.endIcons).isEmpty()
        }
    }

    private fun createConverter(
        totalFiatBalance: BigDecimal,
        userWalletId: UserWalletId? = UserWalletId("01"),
        onTokenClick: (UserWalletId, CryptoCurrency) -> Unit = { _, _ -> },
    ) = ForYouPortfolioReviewTokenRowConverter(
        appCurrency = appCurrency,
        userWalletId = userWalletId,
        totalFiatBalance = totalFiatBalance,
        onTokenClick = onTokenClick,
    )

    /** Mirrors the production fiat rendering used by [ForYouPortfolioReviewTokenRowConverter] for a resolved row. */
    private fun BigDecimal.expectedFiatText(): TextReference = stringReference(
        format { fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol) },
    )

    /** Mirrors the production percent-share rendering of [ForYouPortfolioReviewTokenRowConverter]. */
    private fun BigDecimal.expectedPercentText(total: BigDecimal): TextReference = stringReference(
        toForYouPercent(total).format { percent() },
    )

    private fun createStatus(currency: CryptoCurrency, value: CryptoCurrencyStatus.Value) = CryptoCurrencyStatus(
        currency = currency,
        value = value,
    )

    private fun loadedValue(
        amount: BigDecimal,
        fiatAmount: BigDecimal,
        source: StatusSource = StatusSource.ACTUAL,
    ): CryptoCurrencyStatus.Loaded = mockk {
        every { this@mockk.amount } returns amount
        every { this@mockk.fiatAmount } returns fiatAmount
        every { isError } returns false
        every { sources } returns CryptoCurrencyStatus.Sources(
            networkSource = source,
            quoteSource = source,
            stakingBalanceSource = source,
        )
    }

    private fun missedDerivationValue(): CryptoCurrencyStatus.MissedDerivation = mockk {
        every { amount } returns null
        every { fiatAmount } returns null
        every { isError } returns true
    }

    private fun unreachableValue(): CryptoCurrencyStatus.Unreachable = mockk {
        every { amount } returns null
        every { fiatAmount } returns null
        every { isError } returns true
    }

    private fun createCurrency(
        id: String,
        symbol: String,
        networkName: String = "Network",
    ): CryptoCurrency {
        val network: Network = mockk {
            every { name } returns networkName
            every { isTestnet } returns false
            every { this@mockk.id } returns mockk { every { rawId } returns Network.RawID(id) }
        }
        val currencyId: CryptoCurrency.ID = mockk {
            every { value } returns id
            every { rawCurrencyId } returns null
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