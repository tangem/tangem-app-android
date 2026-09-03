package com.tangem.features.foryou.impl.model.converter.portfolioReview

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds.badge.TangemBadgeUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.foryou.impl.createLoadedValue
import com.tangem.features.foryou.impl.createMissedDerivationValue
import com.tangem.features.foryou.impl.createStakedBalance
import com.tangem.features.foryou.impl.createUnreachableValue
import com.tangem.features.foryou.impl.model.converter.toForYouPercent
import com.tangem.utils.StringsSigns
import com.tangem.utils.StringsSigns.THREE_STARS
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

        @Test
        fun `GIVEN title badge passed WHEN convert THEN row title carries that badge`() {
            // Arrange
            val badge = TangemBadgeUM(text = stringReference("Positive"))
            val currency = createCurrency(id = "coin-eth", symbol = "ETH", networkName = "Ethereum")
            val statuses = listOf(
                createStatus(currency, loadedValue(amount = BigDecimal.ONE, fiatAmount = BigDecimal("100"))),
            )
            val converter = createConverter(totalFiatBalance = BigDecimal("1000"), titleBadge = badge)

            // Act
            val result = converter.convert(statuses) as TangemTokenRowUM.Content

            // Assert
            val title = result.titleUM as TangemTokenRowUM.TitleUM.Content
            assertThat(title.badge).isEqualTo(badge)
        }

        @Test
        fun `GIVEN no title badge WHEN convert THEN row title has no badge`() {
            // Arrange
            val currency = createCurrency(id = "coin-eth", symbol = "ETH", networkName = "Ethereum")
            val statuses = listOf(
                createStatus(currency, loadedValue(amount = BigDecimal.ONE, fiatAmount = BigDecimal("100"))),
            )
            val converter = createConverter(totalFiatBalance = BigDecimal("1000"))

            // Act
            val result = converter.convert(statuses) as TangemTokenRowUM.Content

            // Assert
            val title = result.titleUM as TangemTokenRowUM.TitleUM.Content
            assertThat(title.badge).isNull()
        }
    }

    @Nested
    inner class BalanceHiding {

        @Test
        fun `GIVEN balance hidden WHEN convert THEN fiat and crypto masked but network and percentage kept`() {
            // Arrange
            val currency = createCurrency(id = "coin-eth", symbol = "ETH", networkName = "Ethereum")
            val statuses = listOf(
                createStatus(currency, loadedValue(amount = BigDecimal("2"), fiatAmount = BigDecimal("400"))),
            )
            val converter = createConverter(totalFiatBalance = BigDecimal("1000"), isBalanceHidden = true)

            // Act
            val result = converter.convert(statuses) as TangemTokenRowUM.Content

            // Assert — fiat total and crypto amount are masked; the network name and % share stay visible
            val topEnd = result.topEndContentUM as TangemTokenRowUM.EndContentUM.Content
            val bottomEnd = result.bottomEndContentUM as TangemTokenRowUM.EndContentUM.Content
            val subtitle = result.subtitleUM as TangemTokenRowUM.SubtitleUM.Content
            assertThat(topEnd.text).isEqualTo(stringReference(THREE_STARS))
            assertThat(subtitle.text).isEqualTo(stringReference("Ethereum ${StringsSigns.DOT} $THREE_STARS"))
            assertThat(bottomEnd.text).isEqualTo(BigDecimal("400").expectedPercentText(BigDecimal("1000")))
        }
    }

    @Nested
    inner class Staking {

        @Test
        fun `GIVEN staked balance WHEN convert THEN subtitle crypto amount includes the staked amount`() {
            // Arrange — 2 held on the network plus 3 staked outside it
            val currency = createCurrency(id = "coin-eth", symbol = "ETH", networkName = "Ethereum")
            val statuses = listOf(
                createStatus(
                    currency,
                    loadedValue(
                        amount = BigDecimal("2"),
                        fiatAmount = BigDecimal("400"),
                        fiatRate = BigDecimal("200"),
                        staking = createStakedBalance(BigDecimal("3")),
                    ),
                ),
            )
            val converter = createConverter(totalFiatBalance = BigDecimal("1000"))

            // Act
            val result = converter.convert(statuses) as TangemTokenRowUM.Content

            // Assert
            val subtitle = result.subtitleUM as TangemTokenRowUM.SubtitleUM.Content
            assertThat(subtitle.text).isEqualTo(BigDecimal("5").expectedSubtitleText(currency, "Ethereum"))
        }

        @Test
        fun `GIVEN staked balance WHEN convert THEN fiat total and percent share include the staked fiat`() {
            // Arrange — fiat 400 plus rate 200 x 3 staked = 1000, half of the 2000 portfolio
            val currency = createCurrency(id = "coin-eth", symbol = "ETH", networkName = "Ethereum")
            val statuses = listOf(
                createStatus(
                    currency,
                    loadedValue(
                        amount = BigDecimal("2"),
                        fiatAmount = BigDecimal("400"),
                        fiatRate = BigDecimal("200"),
                        staking = createStakedBalance(BigDecimal("3")),
                    ),
                ),
            )
            val converter = createConverter(totalFiatBalance = BigDecimal("2000"))

            // Act
            val result = converter.convert(statuses) as TangemTokenRowUM.Content

            // Assert
            val topEnd = result.topEndContentUM as TangemTokenRowUM.EndContentUM.Content
            val bottomEnd = result.bottomEndContentUM as TangemTokenRowUM.EndContentUM.Content
            assertThat(topEnd.text).isEqualTo(BigDecimal("1000").expectedFiatText())
            assertThat(bottomEnd.text).isEqualTo(BigDecimal("1000").expectedPercentText(BigDecimal("2000")))
        }

        @Test
        fun `GIVEN several staked statuses WHEN convert THEN staked amounts are summed across them`() {
            // Arrange — the same asset staked in two accounts on one network
            val currency = createCurrency(id = "coin-eth", symbol = "ETH", networkName = "Ethereum")
            val statuses = List(size = 2) {
                createStatus(
                    currency,
                    loadedValue(
                        amount = BigDecimal.ONE,
                        fiatAmount = BigDecimal("100"),
                        fiatRate = BigDecimal("100"),
                        staking = createStakedBalance(BigDecimal.ONE),
                    ),
                )
            }
            val converter = createConverter(totalFiatBalance = BigDecimal("400"))

            // Act
            val result = converter.convert(statuses) as TangemTokenRowUM.Content

            // Assert — (1 + 1 staked) x 2 crypto, (100 + 100 staked fiat) x 2 fiat
            val subtitle = result.subtitleUM as TangemTokenRowUM.SubtitleUM.Content
            val topEnd = result.topEndContentUM as TangemTokenRowUM.EndContentUM.Content
            assertThat(subtitle.text).isEqualTo(BigDecimal("4").expectedSubtitleText(currency, "Ethereum"))
            assertThat(topEnd.text).isEqualTo(BigDecimal("400").expectedFiatText())
        }

        @Test
        fun `GIVEN staked balance on Cardano WHEN convert THEN only rewards are added on top`() {
            // Arrange — Cardano keeps the staked principal inside the network balance, so it must not double
            val currency = createCurrency(
                id = "coin-ada",
                symbol = "ADA",
                networkName = "Cardano",
                networkRawId = "cardano",
            )
            val statuses = listOf(
                createStatus(
                    currency,
                    loadedValue(
                        amount = BigDecimal("2"),
                        fiatAmount = BigDecimal("400"),
                        fiatRate = BigDecimal("200"),
                        staking = createStakedBalance(BigDecimal("3")),
                    ),
                ),
            )
            val converter = createConverter(totalFiatBalance = BigDecimal("1000"))

            // Act
            val result = converter.convert(statuses) as TangemTokenRowUM.Content

            // Assert — the staked 3 carries no rewards, so the bare amounts are rendered
            val subtitle = result.subtitleUM as TangemTokenRowUM.SubtitleUM.Content
            val topEnd = result.topEndContentUM as TangemTokenRowUM.EndContentUM.Content
            assertThat(subtitle.text).isEqualTo(BigDecimal("2").expectedSubtitleText(currency, "Cardano"))
            assertThat(topEnd.text).isEqualTo(BigDecimal("400").expectedFiatText())
        }
    }

    private fun createConverter(
        totalFiatBalance: BigDecimal,
        userWalletId: UserWalletId? = UserWalletId("01"),
        onTokenClick: (UserWalletId, CryptoCurrency) -> Unit = { _, _ -> },
        titleBadge: TangemBadgeUM? = null,
        isBalanceHidden: Boolean = false,
    ) = ForYouPortfolioReviewTokenRowConverter(
        appCurrency = appCurrency,
        userWalletId = userWalletId,
        totalFiatBalance = totalFiatBalance,
        onTokenClick = onTokenClick,
        isBalanceHidden = isBalanceHidden,
        titleBadge = titleBadge,
    )

    /** Mirrors the production fiat rendering used by [ForYouPortfolioReviewTokenRowConverter] for a resolved row. */
    private fun BigDecimal.expectedFiatText(): TextReference = stringReference(
        format { fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol) },
    )

    /** Mirrors the production `network - amount` subtitle rendering for a resolved row. */
    private fun BigDecimal.expectedSubtitleText(currency: CryptoCurrency, networkName: String): TextReference =
        stringReference("$networkName ${StringsSigns.DOT} ${format { crypto(cryptoCurrency = currency) }}")

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
        fiatRate: BigDecimal = BigDecimal.ONE,
        staking: StakingBalance? = null,
    ): CryptoCurrencyStatus.Loaded = createLoadedValue(
        amount = amount,
        fiatAmount = fiatAmount,
        fiatRate = fiatRate,
        staking = staking,
        source = source,
    )

    private fun missedDerivationValue(): CryptoCurrencyStatus.MissedDerivation = createMissedDerivationValue()

    private fun unreachableValue(): CryptoCurrencyStatus.Unreachable = createUnreachableValue()

    private fun createCurrency(
        id: String,
        symbol: String,
        networkName: String = "Network",
        networkRawId: String = "ethereum",
    ): CryptoCurrency {
        val network: Network = mockk {
            every { name } returns networkName
            every { isTestnet } returns false
            every { this@mockk.id } returns mockk { every { rawId } returns Network.RawID(id) }
            every { this@mockk.rawId } returns networkRawId
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
            every { this@mockk.displayDecimals } returns 8
            every { isCustom } returns false
            every { iconUrl } returns null
        }
    }
}