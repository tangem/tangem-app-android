package com.tangem.features.foryou.impl.model.converter.portfolioReview

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds.badge.TangemBadgeColor
import com.tangem.core.ui.ds.badge.TangemBadgeUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.CoinIndicators
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.foryou.impl.R
import com.tangem.features.foryou.impl.components.state.MarketChartUM
import com.tangem.features.foryou.impl.entity.ForYouTokenListItemUM
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM
import com.tangem.features.foryou.impl.model.ForYouSelectedPortfolio
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class ForYouPortfolioReviewConverterTest {

    private val appCurrency: AppCurrency = AppCurrency.Default

    @Nested
    inner class AssetRanking {

        @Test
        fun `GIVEN currency with resolved zero fiat balance WHEN convert THEN it is dropped from the list`() {
            // Arrange
            val zeroBalance = createCoin(rawCurrencyId = "btc", symbol = "BTC", networkId = "bitcoin")
            val nonZeroBalance = createCoin(rawCurrencyId = "eth", symbol = "ETH", networkId = "ethereum")
            val statuses = listOf(
                createStatus(zeroBalance, loadedValue(BigDecimal.ONE, BigDecimal.ZERO)),
                createStatus(nonZeroBalance, loadedValue(BigDecimal.ONE, BigDecimal("100"))),
            )

            // Act
            val result = convert(statuses, totalFiatBalance = BigDecimal("100"))

            // Assert — only the ETH asset survives; the zero-fiat BTC is dropped
            assertThat(result.tokenList.map { it.tokenRowUM.id }).containsExactly("eth")
        }

        @Test
        fun `GIVEN non-content status with null fiat WHEN convert THEN it is kept not dropped`() {
            // Arrange — a non-content status (Unreachable) carries a null fiatAmount, not a resolved zero;
            // it must still be shown so the user sees the token they hold, with the appropriate treatment.
            val unreachable = createCoin(rawCurrencyId = "btc", symbol = "BTC", networkId = "bitcoin")
            val loaded = createCoin(rawCurrencyId = "eth", symbol = "ETH", networkId = "ethereum")
            val statuses = listOf(
                createStatus(unreachable, unreachableValue()),
                createStatus(loaded, loadedValue(BigDecimal.ONE, BigDecimal("100"))),
            )

            // Act
            val result = convert(statuses, totalFiatBalance = BigDecimal("100"))

            // Assert — both assets kept, ranked by summed fiat (eth 100 > btc 0)
            assertThat(result.tokenList.map { it.tokenRowUM.id }).containsExactly("eth", "btc").inOrder()
        }

        @Test
        fun `GIVEN same asset across networks WHEN convert THEN aggregated into one asset ranked by summed fiat`() {
            // Arrange — the same asset (shared rawCurrencyId "usdc") aggregates into one asset
            val onEth = createToken(rawCurrencyId = "usdc", symbol = "USDC", networkId = "ethereum")
            val onSol = createToken(rawCurrencyId = "usdc", symbol = "USDC", networkId = "solana")
            val other = createCoin(rawCurrencyId = "btc", symbol = "BTC", networkId = "bitcoin")
            val statuses = listOf(
                createStatus(onEth, loadedValue(BigDecimal.ONE, BigDecimal("50"))),
                createStatus(onSol, loadedValue(BigDecimal.ONE, BigDecimal("60"))),
                createStatus(other, loadedValue(BigDecimal.ONE, BigDecimal("10"))),
            )

            // Act
            val result = convert(statuses, totalFiatBalance = BigDecimal("120"))

            // Assert — 2 ranked assets: usdc (110 total) ahead of btc (10)
            assertThat(result.tokenList.map { it.tokenRowUM.id }).containsExactly("usdc", "btc").inOrder()
        }

        @Test
        fun `GIVEN more than four assets WHEN convert THEN excess assets collapse into Other`() {
            // Arrange — 5 distinct assets, top 4 kept individually, 5th collapsed into "Other"
            val statuses = (1..5).map { index ->
                createStatus(
                    createCoin(rawCurrencyId = "asset-$index", symbol = "A$index", networkId = "net-$index"),
                    loadedValue(BigDecimal.ONE, BigDecimal(100 - index)),
                )
            }

            // Act
            val result = convert(statuses, totalFiatBalance = BigDecimal("470"))

            // Assert — 4 top asset rows + 1 "Other" row
            assertThat(result.tokenList).hasSize(5)
            assertThat(result.tokenList.last().tokenRowUM.id).isEqualTo("for_you_other_assets")
            assertThat(result.tokenList.last().isExpandable).isFalse()
        }

        @Test
        fun `GIVEN exactly four assets WHEN convert THEN no Other row is appended`() {
            // Arrange
            val statuses = (1..4).map { index ->
                createStatus(
                    createCoin(rawCurrencyId = "asset-$index", symbol = "A$index", networkId = "net-$index"),
                    loadedValue(BigDecimal.ONE, BigDecimal(100 - index)),
                )
            }

            // Act
            val result = convert(statuses, totalFiatBalance = BigDecimal("394"))

            // Assert
            assertThat(result.tokenList).hasSize(4)
        }

        @Test
        fun `GIVEN a single other asset WHEN convert THEN Other row subtitle is singular`() {
            // Arrange — 5 assets: the lowest-balance one collapses into an "Other" row of count 1
            val statuses = (1..5).map { index ->
                createStatus(
                    createCoin(rawCurrencyId = "asset-$index", symbol = "A$index", networkId = "net-$index"),
                    loadedValue(BigDecimal.ONE, BigDecimal(100 - index)),
                )
            }

            // Act
            val result = convert(statuses, totalFiatBalance = BigDecimal("470"))

            // Assert
            val otherRow = result.tokenList.last().tokenRowUM as TangemTokenRowUM.Content
            val subtitle = otherRow.subtitleUM as TangemTokenRowUM.SubtitleUM.Content
            assertThat(subtitle.text).isEqualTo(
                pluralReference(R.plurals.market_chart_assets_android, count = 1, formatArgs = wrappedList(1)),
            )
        }

        @Test
        fun `GIVEN several other assets WHEN convert THEN Other row subtitle is plural`() {
            // Arrange — 7 assets: three lowest-balance ones collapse into an "Other" row of count 3
            val statuses = (1..7).map { index ->
                createStatus(
                    createCoin(rawCurrencyId = "asset-$index", symbol = "A$index", networkId = "net-$index"),
                    loadedValue(BigDecimal.ONE, BigDecimal(100 - index)),
                )
            }

            // Act
            val result = convert(statuses, totalFiatBalance = BigDecimal("658"))

            // Assert
            val otherRow = result.tokenList.last().tokenRowUM as TangemTokenRowUM.Content
            val subtitle = otherRow.subtitleUM as TangemTokenRowUM.SubtitleUM.Content
            assertThat(subtitle.text).isEqualTo(
                pluralReference(R.plurals.market_chart_assets_android, count = 3, formatArgs = wrappedList(3)),
            )
        }

        @Test
        fun `GIVEN empty portfolio WHEN convert THEN token list is empty`() {
            // Act
            val result = createConverter()
                .convert(selectedPortfolio(currencies = emptyList(), totalFiatBalance = BigDecimal.ZERO))
                as PortfolioReviewUM.Content

            // Assert
            assertThat(result.tokenList).isEmpty()
        }
    }

    @Nested
    inner class AssetRow {

        @Test
        fun `GIVEN single-network coin WHEN convert THEN subtitle is common main network`() {
            // Arrange
            val currency = createCoin(rawCurrencyId = "bitcoin", symbol = "BTC", networkId = "bitcoin")
            val statuses = listOf(createStatus(currency, loadedValue(BigDecimal.ONE, BigDecimal("100"))))

            // Act
            val result = convert(statuses, totalFiatBalance = BigDecimal("100"))

            // Assert
            val row = result.tokenList.single().tokenRowUM as TangemTokenRowUM.Content
            val subtitle = row.subtitleUM as TangemTokenRowUM.SubtitleUM.Content
            assertThat(subtitle.text).isEqualTo(resourceReference(R.string.common_main_network))
        }

        @Test
        fun `GIVEN single-network token WHEN convert THEN subtitle is the network name`() {
            // Arrange — the fixture's network name mirrors its id, so "ethereum" is the network name here
            val currency = createToken(rawCurrencyId = "usdc", symbol = "USDC", networkId = "ethereum")
            val statuses = listOf(createStatus(currency, loadedValue(BigDecimal.ONE, BigDecimal("100"))))

            // Act
            val result = convert(statuses, totalFiatBalance = BigDecimal("100"))

            // Assert
            val row = result.tokenList.single().tokenRowUM as TangemTokenRowUM.Content
            val subtitle = row.subtitleUM as TangemTokenRowUM.SubtitleUM.Content
            assertThat(subtitle.text).isEqualTo(stringReference("ethereum"))
        }

        @Test
        fun `GIVEN asset row WHEN convert THEN title text is the currency name`() {
            // Arrange — name differs from symbol so the assertion pins which field the title uses
            val currency = createCoin(rawCurrencyId = "bitcoin", symbol = "BTC", networkId = "bitcoin", name = "Bitcoin")
            val statuses = listOf(createStatus(currency, loadedValue(BigDecimal.ONE, BigDecimal("100"))))

            // Act
            val result = convert(statuses, totalFiatBalance = BigDecimal("100"))

            // Assert
            val row = result.tokenList.single().tokenRowUM as TangemTokenRowUM.Content
            val title = row.titleUM as TangemTokenRowUM.TitleUM.Content
            assertThat(title.text).isEqualTo(stringReference("Bitcoin"))
        }

        @Test
        fun `GIVEN asset spans multiple networks WHEN convert THEN subtitle shows network count with child rows`() {
            // Arrange — same asset (shared rawCurrencyId) on two different networks
            val onEth = createToken(rawCurrencyId = "usdc", symbol = "USDC", networkId = "ethereum")
            val onSol = createToken(rawCurrencyId = "usdc", symbol = "USDC", networkId = "solana")
            val statuses = listOf(
                createStatus(onEth, loadedValue(BigDecimal.ONE, BigDecimal("100"))),
                createStatus(onSol, loadedValue(BigDecimal("2"), BigDecimal("200"))),
            )

            // Act
            val result = convert(statuses, totalFiatBalance = BigDecimal("300"))

            // Assert
            val item = result.tokenList.single()
            val row = item.tokenRowUM as TangemTokenRowUM.Content
            val subtitle = row.subtitleUM as TangemTokenRowUM.SubtitleUM.Content
            assertThat(subtitle.text).isEqualTo(
                pluralReference(R.plurals.common_networks_count, count = 2, formatArgs = wrappedList(2)),
            )
            assertThat(item.tokenList).hasSize(2)
        }

        @Test
        fun `GIVEN multi-network asset WHEN convert THEN child rows ordered by descending fiat balance`() {
            // Arrange
            val onEth = createToken(rawCurrencyId = "usdc", symbol = "USDC", networkId = "ethereum")
            val onSol = createToken(rawCurrencyId = "usdc", symbol = "USDC", networkId = "solana")
            val statuses = listOf(
                createStatus(onEth, loadedValue(BigDecimal.ONE, BigDecimal("100"))),
                createStatus(onSol, loadedValue(BigDecimal("2"), BigDecimal("500"))),
            )

            // Act
            val result = convert(statuses, totalFiatBalance = BigDecimal("600"))

            // Assert — Solana holding (500) ranks above Ethereum holding (100)
            val childIds = result.tokenList.single().tokenList.map { it.id }
            assertThat(childIds).containsExactly("token-usdc-solana", "token-usdc-ethereum").inOrder()
        }

        @Test
        fun `GIVEN all statuses of an asset are Loading WHEN convert THEN asset row is Loading`() {
            // Arrange
            val currency = createCoin(rawCurrencyId = "bitcoin", symbol = "BTC", networkId = "bitcoin")
            val statuses = listOf(createStatus(currency, CryptoCurrencyStatus.Loading))

            // Act
            val result = convert(statuses, totalFiatBalance = BigDecimal.ZERO)

            // Assert
            assertThat(result.tokenList.single().tokenRowUM).isInstanceOf(TangemTokenRowUM.Loading::class.java)
        }

        @Test
        fun `GIVEN expandable asset WHEN convert THEN item is built collapsed`() {
            // Expansion is applied after conversion by ApplyExpandedAssetsTransformer.
            // Arrange
            val currency = createCoin(rawCurrencyId = "bitcoin", symbol = "BTC", networkId = "bitcoin")
            val statuses = listOf(createStatus(currency, loadedValue(BigDecimal.ONE, BigDecimal("100"))))
            val converter = createConverter()

            // Act
            val result = converter.convert(
                selectedPortfolio(statuses, BigDecimal("100")),
            ) as PortfolioReviewUM.Content

            // Assert
            assertThat(result.tokenList.single().isExpanded).isFalse()
            assertThat(result.tokenList.single().isExpandable).isTrue()
        }

        @Test
        fun `GIVEN single-network asset clicked WHEN convert THEN token callback receives wallet id and currency`() {
            // Arrange — a single-network asset has nothing to expand, so a click navigates straight to the token
            val currency = createCoin(rawCurrencyId = "bitcoin", symbol = "BTC", networkId = "bitcoin")
            val statuses = listOf(createStatus(currency, loadedValue(BigDecimal.ONE, BigDecimal("100"))))
            var clicked: Pair<UserWalletId, CryptoCurrency>? = null
            var expanded = false
            val converter = createConverter(
                expandClick = { expanded = true },
                onTokenClick = { id, clickedCurrency -> clicked = id to clickedCurrency },
            )

            // Act
            val result = converter.convert(
                selectedPortfolio(statuses, BigDecimal("100")),
            ) as PortfolioReviewUM.Content
            (result.tokenList.single().tokenRowUM as TangemTokenRowUM.Content).onItemClick?.invoke()

            // Assert
            assertThat(clicked).isEqualTo(UserWalletId("01") to currency)
            assertThat(expanded).isFalse()
        }

        @Test
        fun `GIVEN multi-network asset clicked WHEN convert THEN expand callback receives the asset id`() {
            // Arrange — the same asset on two networks: a click expands to reveal the per-network breakdown
            val onEth = createToken(rawCurrencyId = "usdc", symbol = "USDC", networkId = "ethereum")
            val onSol = createToken(rawCurrencyId = "usdc", symbol = "USDC", networkId = "solana")
            val statuses = listOf(
                createStatus(onEth, loadedValue(BigDecimal.ONE, BigDecimal("100"))),
                createStatus(onSol, loadedValue(BigDecimal.ONE, BigDecimal("200"))),
            )
            var clickedAssetId: String? = null
            var tokenClicked = false
            val converter = createConverter(
                expandClick = { clickedAssetId = it },
                onTokenClick = { _, _ -> tokenClicked = true },
            )

            // Act
            val result = converter.convert(
                selectedPortfolio(statuses, BigDecimal("300")),
            ) as PortfolioReviewUM.Content
            (result.tokenList.single().tokenRowUM as TangemTokenRowUM.Content).onItemClick?.invoke()

            // Assert
            assertThat(clickedAssetId).isEqualTo("usdc")
            assertThat(tokenClicked).isFalse()
        }
    }

    @Nested
    inner class MarketChart {

        @Test
        fun `GIVEN loaded total balance WHEN convert THEN market chart is Loaded with one segment per top asset`() {
            // Arrange
            val statuses = listOf(
                createStatus(
                    createCoin(rawCurrencyId = "btc", symbol = "BTC", networkId = "bitcoin"),
                    loadedValue(BigDecimal.ONE, BigDecimal("70")),
                ),
                createStatus(
                    createCoin(rawCurrencyId = "eth", symbol = "ETH", networkId = "ethereum"),
                    loadedValue(BigDecimal.ONE, BigDecimal("30")),
                ),
            )

            // Act
            val result = convert(statuses, totalFiatBalance = BigDecimal("100"))

            // Assert
            val marketChart = result.marketChartUM as MarketChartUM.Loaded
            assertThat(marketChart.assetCount).isEqualTo(2)
        }

        @Test
        fun `GIVEN loaded total balance WHEN the donut is tapped twice THEN the callback is invoked per tap`() {
            // Arrange
            var taps = 0
            val statuses = listOf(
                createStatus(
                    createCoin(rawCurrencyId = "btc", symbol = "BTC", networkId = "bitcoin"),
                    loadedValue(BigDecimal.ONE, BigDecimal("100")),
                ),
            )
            val portfolio = selectedPortfolio(currencies = statuses, totalFiatBalance = BigDecimal("100"))

            // Act
            val result = createConverter(onDiagramTap = { taps++ })
                .convert(portfolio) as PortfolioReviewUM.Content
            val donutChart = (result.marketChartUM as MarketChartUM.Loaded).donutChart
            donutChart.onSegmentTap()
            donutChart.onSegmentTap()

            // Assert — taps are not deduplicated
            assertThat(taps).isEqualTo(2)
        }

        @Test
        fun `GIVEN non-loaded total balance WHEN convert THEN market chart is NoData`() {
            // Arrange
            val statuses = listOf(
                createStatus(
                    createCoin(rawCurrencyId = "btc", symbol = "BTC", networkId = "bitcoin"),
                    loadedValue(BigDecimal.ONE, BigDecimal("100")),
                ),
            )
            val portfolio = selectedPortfolio(currencies = statuses, totalFiatBalance = TotalFiatBalance.Loading)

            // Act
            val result = createConverter().convert(portfolio) as PortfolioReviewUM.Content

            // Assert
            assertThat(result.marketChartUM).isInstanceOf(MarketChartUM.NoData::class.java)
        }

        @Test
        fun `GIVEN empty portfolio WHEN convert THEN market chart is NoData`() {
            // Act
            val result = createConverter()
                .convert(selectedPortfolio(currencies = emptyList(), totalFiatBalance = BigDecimal.ZERO))
                as PortfolioReviewUM.Content

            // Assert
            assertThat(result.marketChartUM).isInstanceOf(MarketChartUM.NoData::class.java)
        }
    }

    @Nested
    inner class ZeroBalancePortfolio {

        @Test
        fun `GIVEN all currencies have zero fiat WHEN convert THEN market chart is the no-amount NoData`() {
            // Arrange
            val statuses = listOf(
                createStatus(
                    createCoin(rawCurrencyId = "btc", symbol = "BTC", networkId = "bitcoin"),
                    loadedValue(BigDecimal.ZERO, BigDecimal.ZERO),
                ),
                createStatus(
                    createCoin(rawCurrencyId = "eth", symbol = "ETH", networkId = "ethereum"),
                    loadedValue(BigDecimal.ZERO, BigDecimal.ZERO),
                ),
            )

            // Act
            val result = convert(statuses, totalFiatBalance = BigDecimal.ZERO)

            // Assert — the zero-balance treatment, not the generic can-not-load-data chart
            assertThat(result.marketChartUM).isEqualTo(
                MarketChartUM.NoData(
                    title = resourceReference(R.string.market_chart_no_amount),
                    donutText = resourceReference(R.string.market_chart_bubble_no_amount),
                ),
            )
        }

        @Test
        fun `GIVEN all currencies have zero fiat WHEN add funds clicked THEN callback receives the selected wallet id`() {
            // Arrange — the currencies belong to wallet "01", but add-funds must target the selected wallet
            val statuses = listOf(
                createStatus(
                    createCoin(rawCurrencyId = "btc", symbol = "BTC", networkId = "bitcoin"),
                    loadedValue(BigDecimal.ZERO, BigDecimal.ZERO),
                ),
            )
            var addFundsWalletId: UserWalletId? = null
            val converter = createConverter(
                selectedWalletId = UserWalletId("99"),
                onAddFundsClick = { addFundsWalletId = it },
            )

            // Act
            val result = converter.convert(
                selectedPortfolio(statuses, BigDecimal.ZERO),
            ) as PortfolioReviewUM.Content
            result.onAddFundsClick?.invoke()

            // Assert
            assertThat(result.onAddFundsClick).isNotNull()
            assertThat(addFundsWalletId).isEqualTo(UserWalletId("99"))
        }

        @Test
        fun `GIVEN a non-zero balance WHEN convert THEN add funds action is absent`() {
            // Arrange
            val statuses = listOf(
                createStatus(
                    createCoin(rawCurrencyId = "btc", symbol = "BTC", networkId = "bitcoin"),
                    loadedValue(BigDecimal.ONE, BigDecimal("100")),
                ),
            )

            // Act
            val result = convert(statuses, totalFiatBalance = BigDecimal("100"))

            // Assert
            assertThat(result.onAddFundsClick).isNull()
        }

        @Test
        fun `GIVEN no selected wallet WHEN add funds clicked THEN callback is not invoked`() {
            // Arrange — without a selected wallet there is nowhere to add funds, so the click must be a no-op
            var clicked = false
            val converter = createConverter(selectedWalletId = null, onAddFundsClick = { clicked = true })

            // Act
            val result = converter
                .convert(selectedPortfolio(currencies = emptyList(), totalFiatBalance = BigDecimal.ZERO))
                as PortfolioReviewUM.Content
            result.onAddFundsClick?.invoke()

            // Assert
            assertThat(clicked).isFalse()
        }

        @Test
        fun `GIVEN more than five zero-fiat currencies WHEN convert THEN list is capped with no Other row`() {
            // Arrange — 7 distinct zero-balance assets; the zero-balance branch shows the first 5
            // as-is instead of ranking and collapsing the excess into an "Other" row
            val statuses = (1..7).map { index ->
                createStatus(
                    createCoin(rawCurrencyId = "asset-$index", symbol = "A$index", networkId = "net-$index"),
                    loadedValue(BigDecimal.ZERO, BigDecimal.ZERO),
                )
            }

            // Act
            val result = convert(statuses, totalFiatBalance = BigDecimal.ZERO)

            // Assert
            assertThat(result.tokenList.map { it.tokenRowUM.id })
                .containsExactly("asset-1", "asset-2", "asset-3", "asset-4", "asset-5")
                .inOrder()
        }

        @Test
        fun `GIVEN zero-fiat asset on several networks WHEN convert THEN grouped into one expandable item`() {
            // Arrange — the same asset (shared rawCurrencyId) with zero balances on two networks
            val onEth = createToken(rawCurrencyId = "usdc", symbol = "USDC", networkId = "ethereum")
            val onSol = createToken(rawCurrencyId = "usdc", symbol = "USDC", networkId = "solana")
            val statuses = listOf(
                createStatus(onEth, loadedValue(BigDecimal.ZERO, BigDecimal.ZERO)),
                createStatus(onSol, loadedValue(BigDecimal.ZERO, BigDecimal.ZERO)),
            )

            // Act
            val result = convert(statuses, totalFiatBalance = BigDecimal.ZERO)

            // Assert
            val item = result.tokenList.single()
            assertThat(item.tokenRowUM.id).isEqualTo("usdc")
            assertThat(item.tokenList).hasSize(2)
            assertThat(item.isExpandable).isTrue()
        }

        @Test
        fun `GIVEN all-zero portfolio with indicators WHEN convert THEN rows still carry sentiment badges`() {
            // Arrange — the zero-balance branch flows through the same row construction, so badges apply
            val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC", networkId = "bitcoin")
            val statuses = listOf(createStatus(currency, loadedValue(BigDecimal.ZERO, BigDecimal.ZERO)))

            // Act
            val result = convert(
                statuses = statuses,
                totalFiatBalance = BigDecimal.ZERO,
                coinIndicators = mapOf("BTC" to createIndicators("BTC", positiveReading())),
            )

            // Assert
            val badge = result.tokenList.single().assetBadge()
            assertThat(badge?.text).isEqualTo(resourceReference(R.string.common_positive))
        }

        @Test
        fun `GIVEN zero and null fiat currencies mixed WHEN convert THEN zero-balance treatment is not applied`() {
            // Arrange — an unreachable holding has an *unknown* balance, not a resolved zero, so the
            // portfolio must not collapse into the add-funds empty state
            val statuses = listOf(
                createStatus(
                    createCoin(rawCurrencyId = "eth", symbol = "ETH", networkId = "ethereum"),
                    loadedValue(BigDecimal.ZERO, BigDecimal.ZERO),
                ),
                createStatus(
                    createCoin(rawCurrencyId = "btc", symbol = "BTC", networkId = "bitcoin"),
                    unreachableValue(),
                ),
            )

            // Act
            val result = convert(statuses, totalFiatBalance = BigDecimal.ZERO)

            // Assert — falls through to the ranked branch: no add-funds action, the resolved zero is
            // dropped, the unknown-balance holding stays visible
            assertThat(result.onAddFundsClick).isNull()
            assertThat(result.tokenList.map { it.tokenRowUM.id }).containsExactly("btc")
        }
    }

    @Nested
    inner class SentimentBadge {

        @Test
        fun `GIVEN indicators for held symbol WHEN convert THEN asset row carries the sentiment badge`() {
            // Arrange
            val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC", networkId = "bitcoin")
            val statuses = listOf(createStatus(currency, loadedValue(BigDecimal.ONE, BigDecimal("100"))))

            // Act
            val result = convert(
                statuses = statuses,
                totalFiatBalance = BigDecimal("100"),
                coinIndicators = mapOf("BTC" to createIndicators("BTC", positiveReading())),
            )

            // Assert
            val badge = result.tokenList.single().assetBadge()
            assertThat(badge?.text).isEqualTo(resourceReference(R.string.common_positive))
            assertThat(badge?.color).isEqualTo(TangemBadgeColor.Green)
        }

        @Test
        fun `GIVEN no indicators entry for symbol WHEN convert THEN asset row has no badge`() {
            // Arrange
            val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC", networkId = "bitcoin")
            val statuses = listOf(createStatus(currency, loadedValue(BigDecimal.ONE, BigDecimal("100"))))

            // Act — indicators exist only for another symbol
            val result = convert(
                statuses = statuses,
                totalFiatBalance = BigDecimal("100"),
                coinIndicators = mapOf("ETH" to createIndicators("ETH", positiveReading())),
            )

            // Assert
            assertThat(result.tokenList.single().assetBadge()).isNull()
        }

        @Test
        fun `GIVEN lowercase currency symbol WHEN convert THEN uppercase-keyed indicators still match`() {
            // Arrange — the lookup must be case-insensitive (map keys are normalized to uppercase)
            val currency = createCoin(rawCurrencyId = "btc", symbol = "btc", networkId = "bitcoin")
            val statuses = listOf(createStatus(currency, loadedValue(BigDecimal.ONE, BigDecimal("100"))))

            // Act
            val result = convert(
                statuses = statuses,
                totalFiatBalance = BigDecimal("100"),
                coinIndicators = mapOf("BTC" to createIndicators("BTC", positiveReading())),
            )

            // Assert
            assertThat(result.tokenList.single().assetBadge()).isNotNull()
        }

        @Test
        fun `GIVEN WEEK timeframe WHEN convert THEN badge reflects the WEEK reading`() {
            // Arrange — positive for DAY, negative for WEEK
            val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC", networkId = "bitcoin")
            val statuses = listOf(createStatus(currency, loadedValue(BigDecimal.ONE, BigDecimal("100"))))
            val indicators = mapOf(
                "BTC" to createIndicators(
                    "BTC",
                    createReading(CoinIndicators.Reading.Signal.POSITIVE, CoinIndicators.Reading.Timeframe.DAY),
                    createReading(CoinIndicators.Reading.Signal.NEGATIVE, CoinIndicators.Reading.Timeframe.WEEK),
                ),
            )

            // Act
            val result = convert(
                statuses = statuses,
                totalFiatBalance = BigDecimal("100"),
                coinIndicators = indicators,
                timeframe = CoinIndicators.Reading.Timeframe.WEEK,
            )

            // Assert
            val badge = result.tokenList.single().assetBadge()
            assertThat(badge?.text).isEqualTo(resourceReference(R.string.common_negative))
            assertThat(badge?.color).isEqualTo(TangemBadgeColor.Red)
        }

        @Test
        fun `GIVEN neutral indicators WHEN convert THEN asset row badge is neutral`() {
            // Arrange — an actionable but zero-scoring reading yields the neutral (blue) badge
            val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC", networkId = "bitcoin")
            val statuses = listOf(createStatus(currency, loadedValue(BigDecimal.ONE, BigDecimal("100"))))
            val neutral = createReading(CoinIndicators.Reading.Signal.NEUTRAL)

            // Act
            val result = convert(
                statuses = statuses,
                totalFiatBalance = BigDecimal("100"),
                coinIndicators = mapOf("BTC" to createIndicators("BTC", neutral)),
            )

            // Assert
            val badge = result.tokenList.single().assetBadge()
            assertThat(badge?.text).isEqualTo(resourceReference(R.string.common_neutral))
            assertThat(badge?.color).isEqualTo(TangemBadgeColor.Blue)
        }

        @Test
        fun `GIVEN multi-network asset WHEN convert THEN child rows carry the same badge as the asset row`() {
            // Arrange
            val onEth = createToken(rawCurrencyId = "usdc", symbol = "USDC", networkId = "ethereum")
            val onSol = createToken(rawCurrencyId = "usdc", symbol = "USDC", networkId = "solana")
            val statuses = listOf(
                createStatus(onEth, loadedValue(BigDecimal.ONE, BigDecimal("100"))),
                createStatus(onSol, loadedValue(BigDecimal.ONE, BigDecimal("200"))),
            )

            // Act
            val result = convert(
                statuses = statuses,
                totalFiatBalance = BigDecimal("300"),
                coinIndicators = mapOf("USDC" to createIndicators("USDC", positiveReading())),
            )

            // Assert — the same badge on the asset row and both child rows
            val item = result.tokenList.single()
            val assetBadge = item.assetBadge()
            assertThat(assetBadge).isNotNull()
            item.tokenList.forEach { childRow ->
                val childTitle = (childRow as TangemTokenRowUM.Content).titleUM as TangemTokenRowUM.TitleUM.Content
                assertThat(childTitle.badge).isEqualTo(assetBadge)
            }
        }

        @Test
        fun `GIVEN more than four assets with indicators WHEN convert THEN Other row has no badge`() {
            // Arrange — 5 assets; indicators exist for every symbol, but the collapsed "Other" row is
            // an aggregate of several assets and must stay badge-less
            val statuses = (1..5).map { index ->
                createStatus(
                    createCoin(rawCurrencyId = "asset-$index", symbol = "A$index", networkId = "net-$index"),
                    loadedValue(BigDecimal.ONE, BigDecimal(100 - index)),
                )
            }
            val indicators = (1..5).associate { index ->
                "A$index" to createIndicators("A$index", positiveReading())
            }

            // Act
            val result = convert(statuses, totalFiatBalance = BigDecimal("470"), coinIndicators = indicators)

            // Assert
            assertThat(result.tokenList.last().assetBadge()).isNull()
        }
    }

    private fun ForYouTokenListItemUM.assetBadge(): TangemBadgeUM? =
        ((tokenRowUM as TangemTokenRowUM.Content).titleUM as TangemTokenRowUM.TitleUM.Content).badge

    private fun createIndicators(symbol: String, vararg readings: CoinIndicators.Reading): CoinIndicators =
        CoinIndicators(symbol = symbol, readings = readings.toList())

    private fun positiveReading(): CoinIndicators.Reading =
        createReading(signal = CoinIndicators.Reading.Signal.POSITIVE)

    private fun createReading(
        signal: CoinIndicators.Reading.Signal,
        timeframe: CoinIndicators.Reading.Timeframe = CoinIndicators.Reading.Timeframe.DAY,
        type: CoinIndicators.Reading.Type = CoinIndicators.Reading.Type.RSI,
    ): CoinIndicators.Reading = CoinIndicators.Reading(
        type = type,
        name = type.name,
        timeframe = timeframe,
        value = null,
        signal = signal,
        updatedAt = null,
    )

    private fun convert(
        statuses: List<CryptoCurrencyStatus>,
        totalFiatBalance: BigDecimal,
        coinIndicators: Map<String, CoinIndicators> = emptyMap(),
        timeframe: CoinIndicators.Reading.Timeframe = CoinIndicators.Reading.Timeframe.DAY,
    ): PortfolioReviewUM.Content =
        createConverter(coinIndicators = coinIndicators, timeframe = timeframe).convert(selectedPortfolio(statuses, totalFiatBalance)) as PortfolioReviewUM.Content

    private fun createConverter(
        expandClick: (String) -> Unit = {},
        onTokenClick: (UserWalletId, CryptoCurrency) -> Unit = { _, _ -> },
        onAddFundsClick: (UserWalletId) -> Unit = {},
        onDiagramTap: () -> Unit = {},
        selectedWalletId: UserWalletId? = UserWalletId("01"),
        coinIndicators: Map<String, CoinIndicators> = emptyMap(),
        timeframe: CoinIndicators.Reading.Timeframe = CoinIndicators.Reading.Timeframe.DAY,
    ): ForYouPortfolioReviewConverter = ForYouPortfolioReviewConverter(
        appCurrency = appCurrency,
        expandClick = expandClick,
        onTokenClick = onTokenClick,
        onAddFundsClick = onAddFundsClick,
        onDiagramTap = onDiagramTap,
        selectedWalletId = selectedWalletId,
        coinIndicators = coinIndicators,
        timeframe = timeframe,
    )

    private fun selectedPortfolio(
        currencies: List<CryptoCurrencyStatus>,
        totalFiatBalance: BigDecimal,
        source: StatusSource = StatusSource.ACTUAL,
    ): ForYouSelectedPortfolio = selectedPortfolio(
        currencies = currencies,
        totalFiatBalance = TotalFiatBalance.Loaded(amount = totalFiatBalance, source = source),
    )

    private fun selectedPortfolio(
        currencies: List<CryptoCurrencyStatus>,
        totalFiatBalance: TotalFiatBalance,
    ): ForYouSelectedPortfolio = ForYouSelectedPortfolio(
        accountCryptoCurrencyStatuses = currencies.map(::accountCryptoCurrencyStatus),
        totalAccountsCount = 1,
        totalFiatBalance = totalFiatBalance,
    )

    private fun accountCryptoCurrencyStatus(
        currencyStatus: CryptoCurrencyStatus,
        walletId: UserWalletId = UserWalletId("01"),
    ): AccountCryptoCurrencyStatus {
        val mockAccount = mockk<Account.CryptoPortfolio> { every { userWalletId } returns walletId }
        return mockk {
            every { account } returns mockAccount
            every { status } returns currencyStatus
        }
    }

    private fun createStatus(currency: CryptoCurrency, value: CryptoCurrencyStatus.Value) = CryptoCurrencyStatus(
        currency = currency,
        value = value,
    )

    private fun loadedValue(amount: BigDecimal, fiatAmount: BigDecimal): CryptoCurrencyStatus.Loaded = mockk {
        every { this@mockk.amount } returns amount
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

    private fun createCoin(
        rawCurrencyId: String,
        symbol: String,
        networkId: String,
        name: String = symbol,
    ): CryptoCurrency.Coin {
        val network = createNetwork(networkId = networkId, standardTypeName = "MAIN")
        val currencyId = createCurrencyId(idValue = "coin-$rawCurrencyId-$networkId", rawCurrencyId = rawCurrencyId)
        return mockk<CryptoCurrency.Coin> {
            every { this@mockk.id } returns currencyId
            every { this@mockk.symbol } returns symbol
            every { this@mockk.name } returns name
            every { this@mockk.network } returns network
            every { this@mockk.decimals } returns 8
            every { isCustom } returns false
            every { iconUrl } returns null
        }
    }

    private fun createToken(
        rawCurrencyId: String,
        symbol: String,
        networkId: String,
        standardTypeName: String = "ERC20",
    ): CryptoCurrency.Token {
        val network = createNetwork(networkId = networkId, standardTypeName = standardTypeName)
        val currencyId = createCurrencyId(idValue = "token-$rawCurrencyId-$networkId", rawCurrencyId = rawCurrencyId)
        return mockk<CryptoCurrency.Token> {
            every { this@mockk.id } returns currencyId
            every { this@mockk.symbol } returns symbol
            every { this@mockk.name } returns symbol
            every { this@mockk.network } returns network
            every { this@mockk.decimals } returns 6
            every { isCustom } returns false
            every { iconUrl } returns null
            every { contractAddress } returns "0xCONTRACT"
        }
    }

    private fun createCurrencyId(idValue: String, rawCurrencyId: String): CryptoCurrency.ID = mockk {
        every { value } returns idValue
        every { this@mockk.rawCurrencyId } returns CryptoCurrency.RawID(rawCurrencyId)
    }

    private fun createNetwork(networkId: String, standardTypeName: String): Network {
        val standardType: Network.StandardType = mockk {
            every { name } returns standardTypeName
        }
        return mockk {
            every { id } returns mockk {
                every { rawId } returns Network.RawID(networkId)
            }
            every { name } returns networkId
            every { isTestnet } returns false
            every { this@mockk.standardType } returns standardType
        }
    }
}