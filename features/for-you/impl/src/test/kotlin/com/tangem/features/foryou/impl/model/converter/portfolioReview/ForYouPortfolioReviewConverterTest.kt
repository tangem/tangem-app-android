package com.tangem.features.foryou.impl.model.converter.portfolioReview

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds.badge.TangemBadgeColor
import com.tangem.core.ui.ds.badge.TangemBadgeUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.CoinIndicators
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.foryou.impl.R
import com.tangem.features.foryou.impl.components.isRingClosed
import com.tangem.features.foryou.impl.components.state.DonutSegmentColor
import com.tangem.features.foryou.impl.components.state.DonutSegmentUM
import com.tangem.features.foryou.impl.components.state.MarketChartUM
import com.tangem.features.foryou.impl.components.visualSweepAngles
import com.tangem.features.foryou.impl.createLoadedValue
import com.tangem.features.foryou.impl.createStakedBalance
import com.tangem.features.foryou.impl.createUnreachableValue
import com.tangem.features.foryou.impl.entity.ForYouTokenListItemUM
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM
import com.tangem.features.foryou.impl.model.ForYouSelectedPortfolio
import com.tangem.utils.StringsSigns.THREE_STARS
import com.tangem.utils.StringsSigns.TILDE_SIGN
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class ForYouPortfolioReviewConverterTest {

    private val appCurrency: AppCurrency = AppCurrency.Default

    /**
     * Mirrors the converter's private `TOP_HOLDINGS_COUNT`: assets ranked beyond it collapse into the
     * single "Other" row, and into the donut's grey "Other" slice.
     */
    private val topHoldingsCount = 10

    /** A holding far too small for the two-decimal percent rendering to show. */
    private val dustBalance = BigDecimal("0.01")

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
            // Arrange — the same asset (shared raw currency id "usdc") aggregates into one asset
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
        fun `GIVEN more assets than the top holdings WHEN convert THEN excess assets collapse into Other`() {
            // Arrange — one asset beyond the top holdings, so it alone collapses into "Other"
            val (statuses, total) = descendingAssets(topHoldingsCount + 1)

            // Act
            val result = convert(statuses, totalFiatBalance = total)

            // Assert — one row per top asset + 1 "Other" row
            assertThat(result.tokenList).hasSize(topHoldingsCount + 1)
            assertThat(result.tokenList.last().tokenRowUM.id).isEqualTo("for_you_other_assets")
            assertThat(result.tokenList.last().isExpandable).isFalse()
        }

        @Test
        fun `GIVEN exactly the top holdings count of assets WHEN convert THEN no Other row is appended`() {
            // Arrange
            val (statuses, total) = descendingAssets(topHoldingsCount)

            // Act
            val result = convert(statuses, totalFiatBalance = total)

            // Assert
            assertThat(result.tokenList).hasSize(topHoldingsCount)
        }

        @Test
        fun `GIVEN a single other asset WHEN convert THEN Other row subtitle is singular`() {
            // Arrange — one asset beyond the top holdings collapses into an "Other" row of count 1
            val (statuses, total) = descendingAssets(topHoldingsCount + 1)

            // Act
            val result = convert(statuses, totalFiatBalance = total)

            // Assert
            val otherRow = result.tokenList.last().tokenRowUM as TangemTokenRowUM.Content
            val subtitle = otherRow.subtitleUM as TangemTokenRowUM.SubtitleUM.Content
            assertThat(subtitle.text).isEqualTo(
                pluralReference(R.plurals.common_assets_count, count = 1, formatArgs = wrappedList(1)),
            )
        }

        @Test
        fun `GIVEN several other assets WHEN convert THEN Other row subtitle is plural`() {
            // Arrange — three assets beyond the top holdings collapse into an "Other" row of count 3
            val (statuses, total) = descendingAssets(topHoldingsCount + 3)

            // Act
            val result = convert(statuses, totalFiatBalance = total)

            // Assert
            val otherRow = result.tokenList.last().tokenRowUM as TangemTokenRowUM.Content
            val subtitle = otherRow.subtitleUM as TangemTokenRowUM.SubtitleUM.Content
            assertThat(subtitle.text).isEqualTo(
                pluralReference(R.plurals.common_assets_count, count = 3, formatArgs = wrappedList(3)),
            )
        }

        @Test
        fun `GIVEN staking outranks a larger bare balance WHEN convert THEN assets ordered by their totals`() {
            // Arrange — BTC holds more on-chain, but ETH's staked balance puts it ahead on the total
            val statuses = listOf(
                createStatus(
                    createCoin(rawCurrencyId = "btc", symbol = "BTC", networkId = "bitcoin"),
                    loadedValue(BigDecimal.ONE, BigDecimal("100")),
                ),
                createStatus(
                    createCoin(rawCurrencyId = "eth", symbol = "ETH", networkId = "ethereum"),
                    loadedValue(
                        amount = BigDecimal.ONE,
                        fiatAmount = BigDecimal("50"),
                        fiatRate = BigDecimal("25"),
                        staking = createStakedBalance(BigDecimal("4")),
                    ),
                ),
            )

            // Act
            val result = convert(statuses, totalFiatBalance = BigDecimal("250"))

            // Assert — 50 + 25 x 4 staked beats the bare 100
            assertThat(result.tokenList.map { it.tokenRowUM.id }).containsExactly("eth", "btc").inOrder()
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
        fun `GIVEN asset row WHEN convert THEN title text is the currency symbol`() {
            // Arrange — name differs from symbol so the assertion pins which field the title uses
            val currency = createCoin(rawCurrencyId = "bitcoin", symbol = "BTC", networkId = "bitcoin", name = "Bitcoin")
            val statuses = listOf(createStatus(currency, loadedValue(BigDecimal.ONE, BigDecimal("100"))))

            // Act
            val result = convert(statuses, totalFiatBalance = BigDecimal("100"))

            // Assert
            val row = result.tokenList.single().tokenRowUM as TangemTokenRowUM.Content
            val title = row.titleUM as TangemTokenRowUM.TitleUM.Content
            assertThat(title.text).isEqualTo(stringReference("BTC"))
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
        fun `GIVEN multi-network asset with staking WHEN convert THEN asset total sums the staked fiat`() {
            // Arrange — the same asset on two networks, staked on one of them
            val onEth = createToken(rawCurrencyId = "usdc", symbol = "USDC", networkId = "ethereum")
            val onSol = createToken(rawCurrencyId = "usdc", symbol = "USDC", networkId = "solana")
            val statuses = listOf(
                createStatus(
                    onEth,
                    loadedValue(
                        amount = BigDecimal.ONE,
                        fiatAmount = BigDecimal("100"),
                        fiatRate = BigDecimal("50"),
                        staking = createStakedBalance(BigDecimal("2")),
                    ),
                ),
                createStatus(onSol, loadedValue(BigDecimal.ONE, BigDecimal("100"))),
            )

            // Act
            val result = convert(statuses, totalFiatBalance = BigDecimal("300"))

            // Assert — (100 + 50 x 2 staked) on Ethereum plus 100 on Solana
            val row = result.tokenList.single().tokenRowUM as TangemTokenRowUM.Content
            val topEnd = row.topEndContentUM as TangemTokenRowUM.EndContentUM.Content
            assertThat(topEnd.text).isEqualTo(BigDecimal("300").expectedFiatText())
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
        fun `GIVEN non-loaded total balance WHEN convert THEN rows carry no segment colour`() {
            // Arrange — the segment colour is the row's dot keying it to a donut slice, and the donut is
            // NoData until the total resolves, so a coloured dot here would point at a segment never drawn
            val portfolio = selectedPortfolio(
                currencies = twoRankedAssets(),
                totalFiatBalance = TotalFiatBalance.Loading,
            )

            // Act
            val result = createConverter().convert(portfolio) as PortfolioReviewUM.Content

            // Assert
            assertThat(result.tokenList.map { it.segmentColor }).containsExactly(null, null)
        }

        @Test
        fun `GIVEN loaded total balance WHEN convert THEN rows carry segment colours in rank order`() {
            // Act
            val result = convert(twoRankedAssets(), totalFiatBalance = BigDecimal("300"))

            // Assert — colours are assigned by rank, so the larger holding takes the first donut colour
            assertThat(result.tokenList.map { it.segmentColor })
                .containsExactly(DonutSegmentColor.Blue, DonutSegmentColor.Green)
                .inOrder()
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

        @Test
        fun `GIVEN assets collapsed into Other WHEN convert THEN donut closes its ring with a grey Other slice`() {
            // Arrange — one asset beyond the top holdings, so an "Other" bucket exists
            val (statuses, total) = descendingAssets(topHoldingsCount + 1)

            // Act
            val result = convert(statuses, totalFiatBalance = total)

            // Assert — the grey slice carries the collapsed balance and is weighted at the complement of
            // the others (not at otherBalance / total), which is what closes the ring exactly
            val segments = donutSegments(result)
            assertThat(segments.last()).isEqualTo(
                DonutSegmentUM(
                    color = DonutSegmentColor.Grey,
                    weight = BigDecimal.ONE - segments.dropLast(1).sumOf { it.weight },
                    title = resourceReference(R.string.common_other),
                    fiatValue = assetFiatBalance(topHoldingsCount + 1).expectedFiatText(),
                ),
            )
        }

        @Test
        fun `GIVEN assets collapsed into Other WHEN convert THEN the slice weights close the ring`() {
            // Arrange
            val (statuses, total) = descendingAssets(topHoldingsCount + 1)

            // Act
            val result = convert(statuses, totalFiatBalance = total)

            // Assert — a closed ring is what stops DonutChart reserving a floored grey gap beside the
            // grey slice, and lets it skip the translucent track underneath
            assertThat(donutSegments(result).sumOf { it.weight }).isEqualToIgnoringScale(BigDecimal.ONE)
        }

        @Test
        fun `GIVEN a grey Other slice WHEN convert THEN the asset count excludes it`() {
            // Arrange
            val (statuses, total) = descendingAssets(topHoldingsCount + 1)

            // Act
            val result = convert(statuses, totalFiatBalance = total)

            // Assert — the "Top N assets" header counts assets, and the grey slice is a collapsed bucket
            val marketChart = result.marketChartUM as MarketChartUM.Loaded
            assertThat(marketChart.donutChart.donutSegmentList).hasSize(topHoldingsCount + 1)
            assertThat(marketChart.assetCount).isEqualTo(topHoldingsCount)
        }

        @Test
        fun `GIVEN no assets beyond the top holdings WHEN convert THEN donut has no grey slice`() {
            // Arrange
            val (statuses, total) = descendingAssets(topHoldingsCount)

            // Act
            val result = convert(statuses, totalFiatBalance = total)

            // Assert — nothing was collapsed, so there is no remainder to label
            val segments = donutSegments(result)
            assertThat(segments).hasSize(topHoldingsCount)
            assertThat(segments.map { it.color }).doesNotContain(DonutSegmentColor.Grey)
        }

        @Test
        fun `GIVEN shares that do not divide evenly WHEN convert THEN the slice weights still close the ring`() {
            // Arrange — seven equal holdings, so every share is 0.142857... A share rounded to the fiat
            // amount's own two decimals would be 0.14, and the seven together would fall 2% short of the
            // circle: enough for the donut to reserve a grey wedge with no balance behind it.
            val assetCount = 7
            val balance = BigDecimal("100.00")
            val statuses = (1..assetCount).map { rank ->
                createStatus(
                    createCoin(rawCurrencyId = "asset-$rank", symbol = "A$rank", networkId = "net-$rank"),
                    loadedValue(BigDecimal.ONE, balance),
                )
            }

            // Act
            val result = convert(statuses, totalFiatBalance = balance * assetCount.toBigDecimal())

            // Assert — nothing was collapsed, so there is no grey slice to close the ring and the shares
            // themselves have to, within the angle at which DonutChart stops calling the ring closed.
            val segments = donutSegments(result)
            assertThat(segments.map { it.color }).doesNotContain(DonutSegmentColor.Grey)
            val sweeps = visualSweepAngles(weights = segments.map { it.weight.toFloat() }, capDeg = 0f)
            assertThat(isRingClosed(sweeps)).isTrue()
        }

        @Test
        fun `GIVEN balance hidden WHEN convert THEN the grey Other slice value is masked`() {
            // Arrange
            val (statuses, total) = descendingAssets(topHoldingsCount + 1)

            // Act
            val result = convert(statuses, totalFiatBalance = total, isBalanceHidden = true)

            // Assert
            assertThat(donutSegments(result).last().fiatValue).isEqualTo(stringReference(THREE_STARS))
        }

        @Test
        fun `GIVEN an Other bucket too small to show WHEN convert THEN the top holding share is marked approximate`() {
            // Arrange — a dust asset beyond the top holdings: the top assets are mathematically below 100%,
            // but two decimal places round their share up to exactly it
            val (topStatuses, topBalance) = descendingAssets(topHoldingsCount)
            val dust = createStatus(
                createCoin(rawCurrencyId = "dust", symbol = "DUST", networkId = "net-dust"),
                loadedValue(BigDecimal.ONE, dustBalance),
            )

            // Act
            val result = convert(topStatuses + dust, totalFiatBalance = topBalance + dustBalance)

            // Assert
            assertThat(topHoldingText(result)).isEqualTo(TILDE_SIGN + BigDecimal.ONE.format { percent() })
        }

        @Test
        fun `GIVEN an Other bucket large enough to show WHEN convert THEN the top holding share is left bare`() {
            // Arrange — one more asset than the top holdings, holding a visible share
            val (statuses, total) = descendingAssets(topHoldingsCount + 1)

            // Act
            val result = convert(statuses, totalFiatBalance = total)

            // Assert — 945.00 of 1034.00; the rendering does not claim the whole portfolio, so nothing is marked
            assertThat(topHoldingText(result)).isEqualTo(BigDecimal("0.9139").format { percent() })
        }

        @Test
        fun `GIVEN no assets beyond the top holdings WHEN convert THEN the top holding share is left bare`() {
            // Arrange
            val (statuses, total) = descendingAssets(topHoldingsCount)

            // Act
            val result = convert(statuses, totalFiatBalance = total)

            // Assert — the top assets really are the whole portfolio, so 100% is exact rather than rounded
            assertThat(topHoldingText(result)).isEqualTo(BigDecimal.ONE.format { percent() })
        }

        /** The share substituted into `market_chart_top_holding`, as the converter rendered it. */
        private fun topHoldingText(result: PortfolioReviewUM.Content): String {
            val reference = (result.marketChartUM as MarketChartUM.Loaded).topHoldingPercent
            return (reference as TextReference.Res).formatArgs.first() as String
        }

        private fun donutSegments(result: PortfolioReviewUM.Content): List<DonutSegmentUM> =
            (result.marketChartUM as MarketChartUM.Loaded).donutChart.donutSegmentList

        private fun twoRankedAssets(): List<CryptoCurrencyStatus> = listOf(
            createStatus(
                createCoin(rawCurrencyId = "btc", symbol = "BTC", networkId = "bitcoin"),
                loadedValue(BigDecimal.ONE, BigDecimal("200")),
            ),
            createStatus(
                createCoin(rawCurrencyId = "eth", symbol = "ETH", networkId = "ethereum"),
                loadedValue(BigDecimal.ONE, BigDecimal("100")),
            ),
        )
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
        fun `GIVEN zero network balance but staked balance WHEN convert THEN portfolio is not treated as empty`() {
            // Arrange — nothing left on-chain, the whole holding is staked
            val statuses = listOf(
                createStatus(
                    createCoin(rawCurrencyId = "eth", symbol = "ETH", networkId = "ethereum"),
                    loadedValue(
                        amount = BigDecimal.ZERO,
                        fiatAmount = BigDecimal.ZERO,
                        fiatRate = BigDecimal("200"),
                        staking = createStakedBalance(BigDecimal("3")),
                    ),
                ),
            )

            // Act
            val result = convert(statuses, totalFiatBalance = BigDecimal("600"))

            // Assert — the staked fiat keeps it out of the no-amount / add-funds treatment
            assertThat(result.marketChartUM).isInstanceOf(MarketChartUM.Loaded::class.java)
            assertThat(result.onAddFundsClick).isNull()
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
        fun `GIVEN more assets than the top holdings with indicators WHEN convert THEN Other row has no badge`() {
            // Arrange — indicators exist for every symbol, but the collapsed "Other" row is an aggregate
            // of several assets and must stay badge-less
            val (statuses, total) = descendingAssets(topHoldingsCount + 1)
            val indicators = (1..topHoldingsCount + 1).associate { index ->
                "A$index" to createIndicators("A$index", positiveReading())
            }

            // Act
            val result = convert(statuses, totalFiatBalance = total, coinIndicators = indicators)

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
        isBalanceHidden: Boolean = false,
    ): PortfolioReviewUM.Content = createConverter(
        coinIndicators = coinIndicators,
        timeframe = timeframe,
        isBalanceHidden = isBalanceHidden,
    ).convert(selectedPortfolio(statuses, totalFiatBalance)) as PortfolioReviewUM.Content

    private fun createConverter(
        expandClick: (String) -> Unit = {},
        onTokenClick: (UserWalletId, CryptoCurrency) -> Unit = { _, _ -> },
        onAddFundsClick: (UserWalletId) -> Unit = {},
        onDiagramTap: () -> Unit = {},
        selectedWalletId: UserWalletId? = UserWalletId("01"),
        coinIndicators: Map<String, CoinIndicators> = emptyMap(),
        timeframe: CoinIndicators.Reading.Timeframe = CoinIndicators.Reading.Timeframe.DAY,
        isBalanceHidden: Boolean = false,
    ): ForYouPortfolioReviewConverter = ForYouPortfolioReviewConverter(
        appCurrency = appCurrency,
        expandClick = expandClick,
        onTokenClick = onTokenClick,
        onAddFundsClick = onAddFundsClick,
        onDiagramTap = onDiagramTap,
        selectedWalletId = selectedWalletId,
        coinIndicators = coinIndicators,
        timeframe = timeframe,
        isBalanceHidden = isBalanceHidden,
    )

    /**
     * [count] distinct single-network assets with strictly descending fiat balances, paired with the
     * portfolio total they sum to — so a test can say "one more asset than the top holdings" without
     * hand-summing the total.
     */
    private fun descendingAssets(count: Int): Pair<List<CryptoCurrencyStatus>, BigDecimal> {
        val statuses = (1..count).map { rank ->
            createStatus(
                createCoin(rawCurrencyId = "asset-$rank", symbol = "A$rank", networkId = "net-$rank"),
                loadedValue(BigDecimal.ONE, assetFiatBalance(rank)),
            )
        }
        return statuses to (1..count).sumOf(::assetFiatBalance)
    }

    /**
     * Fiat balance of the [rank]-th asset from [descendingAssets] — rank 1 is the largest. Scaled like a
     * fiat amount a backend would actually hand over, so the shares derived from it are exercised at a
     * realistic precision rather than one chosen to flatter the arithmetic.
     */
    private fun assetFiatBalance(rank: Int): BigDecimal = BigDecimal(100 - rank).setScale(2)

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
    ): ForYouSelectedPortfolio {
        val accountStatuses = currencies.map(::accountCryptoCurrencyStatus)
        return ForYouSelectedPortfolio(
            accountCryptoCurrencyStatuses = accountStatuses,
            selectedAccounts = accountStatuses.map { it.account }.distinct(),
            totalAccountsCount = 1,
            totalFiatBalance = totalFiatBalance,
        )
    }

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

    /** Mirrors the production fiat rendering used by [ForYouPortfolioReviewTokenRowConverter] for a resolved row. */
    private fun BigDecimal.expectedFiatText(): TextReference = stringReference(
        format { fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol) },
    )

    private fun createStatus(currency: CryptoCurrency, value: CryptoCurrencyStatus.Value) = CryptoCurrencyStatus(
        currency = currency,
        value = value,
    )

    private fun loadedValue(
        amount: BigDecimal,
        fiatAmount: BigDecimal,
        fiatRate: BigDecimal = BigDecimal.ONE,
        staking: StakingBalance? = null,
    ): CryptoCurrencyStatus.Loaded = createLoadedValue(
        amount = amount,
        fiatAmount = fiatAmount,
        fiatRate = fiatRate,
        staking = staking,
    )

    /** A non-content status: carries a null fiatAmount (unknown balance), not a resolved zero. */
    private fun unreachableValue(): CryptoCurrencyStatus.Unreachable = createUnreachableValue()

    /**
     * A real [CryptoCurrency.Coin] rather than a mock: the converter rebuilds the asset row's head icon
     * with `copy(iconUrl = ...)`, and a mock answers no generated member it was not stubbed with.
     */
    private fun createCoin(
        rawCurrencyId: String,
        symbol: String,
        networkId: String,
        name: String = symbol,
    ): CryptoCurrency.Coin = CryptoCurrency.Coin(
        id = createCurrencyId(idValue = "coin-$rawCurrencyId-$networkId", rawCurrencyId = rawCurrencyId),
        network = createNetwork(networkId = networkId, standardTypeName = "MAIN"),
        name = name,
        symbol = symbol,
        decimals = 8,
        iconUrl = null,
        isCustom = false,
    )

    private fun createToken(
        rawCurrencyId: String,
        symbol: String,
        networkId: String,
        standardTypeName: String = "ERC20",
    ): CryptoCurrency.Token = CryptoCurrency.Token(
        id = createCurrencyId(idValue = "token-$rawCurrencyId-$networkId", rawCurrencyId = rawCurrencyId),
        network = createNetwork(networkId = networkId, standardTypeName = standardTypeName),
        name = symbol,
        symbol = symbol,
        decimals = 6,
        iconUrl = null,
        isCustom = false,
        contractAddress = "0xCONTRACT",
    )

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
            // Read by the staking accessors to decide whether the staked principal sits outside the balance.
            every { rawId } returns networkId
            every { this@mockk.standardType } returns standardType
        }
    }
}