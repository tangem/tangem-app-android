package com.tangem.features.feed.model.market.details.converter

import androidx.compose.runtime.Stable
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.compact
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.market.detailed.state.*
import com.tangem.utils.Provider
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.math.RoundingMode

@Suppress("LargeClass")
@Stable
internal class MetricsConverter(
    private val appCurrency: Provider<AppCurrency>,
    private val tokenSymbol: String,
    private val onInfoClick: (InfoBottomSheetContent) -> Unit,
    private val isRedesignEnabled: Boolean,
) : Converter<TokenMarketInfo.Metrics, MetricsUM> {

    @Suppress("LongMethod")
    override fun convert(value: TokenMarketInfo.Metrics): MetricsUM {
        return with(value) {
            MetricsUM(
                metrics = persistentListOf(
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_market_capitalization),
                        value = marketCap.formatAmount() ?: StringsSigns.DASH_SIGN,
                        onInfoClick = {
                            onInfoClick(
                                InfoBottomSheetContent(
                                    title = resourceReference(
                                        R.string.markets_token_details_market_capitalization_full,
                                    ),
                                    body = resourceReference(
                                        R.string.markets_token_details_market_capitalization_description,
                                    ),
                                ),
                            )
                        },
                    ),
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_market_rating),
                        value = marketRating?.toString() ?: StringsSigns.DASH_SIGN,
                        onInfoClick = {
                            onInfoClick(
                                InfoBottomSheetContent(
                                    title = resourceReference(R.string.markets_token_details_market_rating_full),
                                    body = resourceReference(R.string.markets_token_details_market_rating_description),
                                ),
                            )
                        },
                    ),
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_trading_volume),
                        value = volume24h.formatAmount() ?: StringsSigns.DASH_SIGN,
                        onInfoClick = {
                            onInfoClick(
                                InfoBottomSheetContent(
                                    title = resourceReference(R.string.markets_token_details_trading_volume_full),
                                    body = resourceReference(
                                        R.string.markets_token_details_trading_volume_24h_description,
                                    ),
                                ),
                            )
                        },
                    ),
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_fully_diluted_valuation),
                        value = fullyDilutedValuation.formatAmount() ?: StringsSigns.DASH_SIGN,
                        onInfoClick = {
                            onInfoClick(
                                InfoBottomSheetContent(
                                    title = resourceReference(
                                        R.string.markets_token_details_fully_diluted_valuation_full,
                                    ),
                                    body = resourceReference(
                                        R.string.markets_token_details_fully_diluted_valuation_description,
                                    ),
                                ),
                            )
                        },
                    ),
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_circulating_supply),
                        value = circulatingSupply.formatAmount(crypto = true) ?: StringsSigns.DASH_SIGN,
                        onInfoClick = {
                            onInfoClick(
                                InfoBottomSheetContent(
                                    title = resourceReference(R.string.markets_token_details_circulating_supply_full),
                                    body = resourceReference(
                                        R.string.markets_token_details_circulating_supply_description,
                                    ),
                                ),
                            )
                        },
                    ),
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_max_supply),
                        value = maxSupply.formatMaxSupply() ?: StringsSigns.DASH_SIGN,
                        onInfoClick = {
                            onInfoClick(
                                InfoBottomSheetContent(
                                    title = resourceReference(R.string.markets_token_details_max_supply_full),
                                    body = resourceReference(R.string.markets_token_details_total_supply_description),
                                ),
                            )
                        },
                    ),
                ),
                metricsV2 = if (isRedesignEnabled) {
                    convertToMetricsV2UM(value)
                } else {
                    null
                },
            )
        }
    }

    @Suppress("LongMethod", "NestedScopeFunctions")
    private fun convertToMetricsV2UM(value: TokenMarketInfo.Metrics): MetricsV2UM {
        val infoPoints = with(value) {
            val liquidity = getLiquidity(value.volume24h, value.marketCap)
            persistentListOf(
                InfoPointUMV2.MarketCap(
                    capitalizationValue = marketCap.formatAmount()?.let(::stringReference),
                    onInfoClick = {
                        onInfoClick(
                            InfoBottomSheetContent(
                                title = resourceReference(
                                    R.string.markets_token_details_market_capitalization_full,
                                ),
                                body = resourceReference(
                                    R.string.markets_token_details_market_capitalization_description,
                                ),
                            ),
                        )
                    },
                ),
                InfoPointUMV2.TradingVolume(
                    tradingValue = volume24h.formatAmount()?.let(::stringReference),
                    liquidity = liquidity,
                    trendingVolumeLiquidityType = getTrendingVolumeLiquidityType(liquidity),
                    onInfoClick = {
                        onInfoClick(
                            InfoBottomSheetContent(
                                title = resourceReference(R.string.markets_token_details_trading_volume_full),
                                body = resourceReference(
                                    R.string.markets_token_details_trading_volume_24h_description,
                                ),
                            ),
                        )
                    },
                ),
                InfoPointUMV2.MarketPosition(
                    position = marketRating?.let {
                        stringReference(it.toString())
                    },
                    rangeValue = getMarketRatingRangeValue(marketRating),
                    marketRatingType = getMarketRatingType(marketRating),
                    onInfoClick = {
                        onInfoClick(
                            InfoBottomSheetContent(
                                title = resourceReference(R.string.markets_token_details_market_rating_full),
                                body = resourceReference(R.string.markets_token_details_market_rating_description),
                            ),
                        )
                    },
                    marketRatingChange24H = getMarketRatingChange(marketRatingChange24h),
                ),
                InfoPointUMV2.FullyDilutedValuation(
                    value = fullyDilutedValuation?.formatAmount()?.let { formatted ->
                        resourceReference(
                            id = R.string.markets_token_details_valuation_value_in_total,
                            formatArgs = wrappedList(formatted),
                        )
                    },
                    fullyDilutedValuationChange24 = fullyDilutedValuationChange24?.convertChange(),
                    onInfoClick = {
                        onInfoClick(
                            InfoBottomSheetContent(
                                title = resourceReference(
                                    R.string.markets_token_details_fully_diluted_valuation_full,
                                ),
                                body = resourceReference(
                                    R.string.markets_token_details_fully_diluted_valuation_description,
                                ),
                            ),
                        )
                    },
                ),
                InfoPointUMV2.CirculatingSupply(
                    currentValue = circulatingSupply?.formatAmount(true)?.let(::stringReference),
                    maxValue = when (maxSupply) {
                        null -> null
                        BigDecimal.ZERO -> {
                            resourceReference(
                                R.string
                                    .markets_token_details_metrics_no_limited,
                            )
                        }
                        else -> maxSupply.formatAmount(true)?.let(::stringReference)
                    },
                    fillValue = getCirculatingSupplyFillValue(circulatingSupply, maxSupply),
                    onInfoClick = {
                        onInfoClick(
                            InfoBottomSheetContent(
                                title = resourceReference(
                                    R.string.markets_token_details_max_supply_and_circulation_full,
                                ),
                                body = combinedReference(
                                    resourceReference(R.string.markets_token_details_circulating_supply_description),
                                    stringReference("\n\n"),
                                    resourceReference(R.string.markets_token_details_total_supply_description),
                                ),
                            ),
                        )
                    },
                ),
            )
        }

        return buildMetricsV2UM(infoPoints)
    }

    private fun buildMetricsV2UM(infoPoints: ImmutableList<InfoPointUMV2>): MetricsV2UM {
        val rows = infoPoints.chunked(size = 2)
            .map { chunk ->
                MetricsV2UM.Row(
                    first = chunk.first(),
                    second = chunk.getOrNull(1),
                )
            }

        return MetricsV2UM(
            rows = rows.toImmutableList(),
        )
    }

    private fun BigDecimal?.formatMaxSupply(): String? {
        when (this) {
            null -> return StringsSigns.DASH_SIGN
            BigDecimal.ZERO -> return StringsSigns.INFINITY_SIGN
        }

        return this.formatAmount(crypto = true)
    }

    private fun BigDecimal?.formatAmount(crypto: Boolean = false): String? {
        if (this == null) return null

        return if (crypto) {
            format {
                crypto(
                    symbol = tokenSymbol,
                    decimals = 2,
                ).compact()
            }
        } else {
            val currency = appCurrency()

            format {
                fiat(
                    fiatCurrencyCode = currency.code,
                    fiatCurrencySymbol = currency.symbol,
                ).compact()
            }
        }
    }

    private fun BigDecimal?.convertChange(): TextReference? {
        if (this == null) return null
        val amount = this.abs().formatAmount()
        val value = when {
            this > BigDecimal.ZERO -> StringsSigns.PLUS + amount
            this < BigDecimal.ZERO -> StringsSigns.MINUS + amount
            else -> amount
        }
        return value?.let(::stringReference)
    }

    @Suppress("MagicNumber")
    private fun getLiquidity(volume24h: BigDecimal?, marketCap: BigDecimal?): Float? {
        if (volume24h == null || marketCap == null || marketCap == BigDecimal.ZERO) return null
        val ratio = volume24h
            .divide(marketCap, 6, RoundingMode.HALF_UP)
            .toFloat()

        return ratio.coerceIn(0f, 1f)
    }

    @Suppress("MagicNumber")
    private fun getTrendingVolumeLiquidityType(liquidity: Float?): TrendingVolumeLiquidityType {
        return when {
            liquidity == null -> TrendingVolumeLiquidityType.UNKNOWN
            liquidity >= 0.5f -> TrendingVolumeLiquidityType.HIGH
            liquidity in 0.2f..<0.5f -> TrendingVolumeLiquidityType.MEDIUM
            else -> TrendingVolumeLiquidityType.LOW
        }
    }

    @Suppress("MagicNumber")
    private fun getMarketRatingType(marketRating: Int?): MarketRatingType {
        return when (marketRating) {
            1 -> MarketRatingType.GOLD
            2 -> MarketRatingType.SILVER
            3 -> MarketRatingType.BRONZE
            else -> MarketRatingType.OTHER
        }
    }

    @Suppress("MagicNumber")
    private fun getMarketRatingRangeValue(marketRating: Int?): Float? {
        if (marketRating == null) return null

        return when {
            marketRating <= 20 -> {
                val segmentStart = 1f
                val segmentEnd = 0.75f
                val rangeMin = 1
                val rangeMax = 20
                val progress = segmentStart +
                    (marketRating - rangeMin).toFloat() / (rangeMax - rangeMin) * (segmentEnd - segmentStart)
                progress.coerceIn(0f, 1f)
            }
            marketRating <= 100 -> {
                val segmentStart = 0.76f
                val segmentEnd = 0.5f
                val rangeMin = 21
                val rangeMax = 100
                val progress = segmentStart +
                    (marketRating - rangeMin).toFloat() / (rangeMax - rangeMin) * (segmentEnd - segmentStart)
                progress.coerceIn(0f, 1f)
            }
            marketRating <= 1000 -> {
                val segmentStart = 0.51f
                val segmentEnd = 0.25f
                val rangeMin = 101
                val rangeMax = 1000
                val progress = segmentStart +
                    (marketRating - rangeMin).toFloat() / (rangeMax - rangeMin) * (segmentEnd - segmentStart)
                progress.coerceIn(0f, 1f)
            }
            marketRating <= 10000 -> {
                val segmentStart = 0.26f
                val segmentEnd = 0.01f
                val rangeMin = 1001
                val rangeMax = 10000
                val progress = segmentStart +
                    (marketRating - rangeMin).toFloat() / (rangeMax - rangeMin) * (segmentEnd - segmentStart)
                progress.coerceIn(0f, 1f)
            }
            else -> {
                0f
            }
        }
    }

    @Suppress("MagicNumber")
    private fun getCirculatingSupplyFillValue(circulatingSupply: BigDecimal?, maxSupply: BigDecimal?): Float? {
        if (circulatingSupply == null || maxSupply == null || maxSupply == BigDecimal.ZERO) return null

        val ratio = circulatingSupply
            .divide(maxSupply, 6, RoundingMode.HALF_UP)
            .toFloat()

        return ratio.coerceIn(0f, 1f)
    }

    private fun getMarketRatingChange(change: Int?): MarketRatingChange24H {
        return when {
            change == null || change == 0 -> MarketRatingChange24H.NoChanges
            change < 0 -> MarketRatingChange24H.Down(-change)
            else -> MarketRatingChange24H.Up(change)
        }
    }
}