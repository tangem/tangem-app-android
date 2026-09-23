package com.tangem.features.feed.crypto.model.converter

import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.compact
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.MarketIndicators
import com.tangem.features.feed.crypto.impl.R
import com.tangem.features.feed.crypto.ui.state.MarketIndexStatus
import com.tangem.features.feed.crypto.ui.state.MarketIndexUM
import com.tangem.features.feed.crypto.ui.state.TotalMarketCapUM
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Converts [MarketIndicators] to the state of the "Total market cap" block.
 *
 * The backend already converts the capitalisation into the currency it was asked for, so [appCurrency]
 * only formats it for the device locale — and must be the currency that amount was fetched with, never
 * whichever one is selected at render time.
 */
internal class TotalMarketCapConverter(
    private val appCurrency: AppCurrency,
    private val onIndexClick: (MarketIndexUM) -> Unit,
    private val onInfoClick: () -> Unit,
) {

    fun convert(value: MarketIndicators): TotalMarketCapUM {
        // a zero global capitalisation is broken data rather than a reading, as in MarketPulseItemConverter
        val marketCap = value.totalMarketCap?.takeIf { it.signum() != 0 } ?: return TotalMarketCapUM.Empty

        return TotalMarketCapUM.Content(
            title = resourceReference(R.string.market_crypto_total_market_cap_title),
            value = stringReference(marketCap.formatCompactFiat()),
            change = TangemPriceChange.State(
                value = stringReference(value.marketCapChange24h.format { percent() }),
                direction = value.marketCapChange24h.toPriceChangeDirection(),
            ),
            indexes = listOfNotNull(
                value.fearGreed?.toIndexUM(
                    id = FEAR_GREED_ID,
                    title = resourceReference(R.string.market_crypto_total_cap_fear_greed_title_short),
                    fullTitle = resourceReference(R.string.market_crypto_total_cap_fear_greed_title_full),
                    description = resourceReference(R.string.market_crypto_total_cap_fear_greed_info),
                ),
                value.altcoinSeason?.toIndexUM(
                    id = ALTCOIN_INDEX_ID,
                    title = resourceReference(R.string.market_crypto_total_cap_altcoin_index_title),
                    fullTitle = resourceReference(R.string.market_crypto_total_cap_altcoin_index_title),
                    description = resourceReference(R.string.market_crypto_total_cap_altcoin_index_info),
                ),
            ).toImmutableList(),
            onIndexClick = onIndexClick,
            onInfoClick = onInfoClick,
        )
    }

    private fun BigDecimal.formatCompactFiat(): String = format {
        fiat(
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        ).compact(
            threeDigitsMethod = true,
        )
    }

    /** Rounded to the precision `percent()` renders, so the arrow never contradicts a displayed `0.00%` */
    private fun BigDecimal?.toPriceChangeDirection(): TangemPriceChange.Direction {
        val scaled = this?.setScale(PERCENT_DISPLAY_SCALE, RoundingMode.HALF_UP)

        return when {
            scaled == null -> TangemPriceChange.Direction.Neutral
            scaled.signum() > 0 -> TangemPriceChange.Direction.Up
            scaled.signum() < 0 -> TangemPriceChange.Direction.Down
            else -> TangemPriceChange.Direction.Neutral
        }
    }

    private fun MarketIndicators.Index.toIndexUM(
        id: String,
        title: TextReference,
        fullTitle: TextReference,
        description: TextReference,
    ): MarketIndexUM {
        val position = value.coerceIn(SCALE)

        return MarketIndexUM(
            id = id,
            title = title,
            fullTitle = fullTitle,
            description = description,
            statusLabel = stringReference(classification),
            status = position.toStatus(),
            value = stringReference(value.toString()),
            progress = (position - SCALE.first).toFloat() / (SCALE.last - SCALE.first),
        )
    }

    /** Bands the design assigns to the scale, running in the order of the gauge's gradient */
    private fun Int.toStatus(): MarketIndexStatus = when {
        this <= ERROR_BAND_MAX -> MarketIndexStatus.Error
        this <= WARNING_BAND_MAX -> MarketIndexStatus.Warning
        this <= SUCCESS_BAND_MAX -> MarketIndexStatus.Success
        else -> MarketIndexStatus.Info
    }

    private companion object {
        const val FEAR_GREED_ID = "fear_greed"
        const val ALTCOIN_INDEX_ID = "altcoin_index"

        /** Fraction digits of a percentage `percent()` renders, expressed on the fraction itself */
        const val PERCENT_DISPLAY_SCALE = 4

        const val ERROR_BAND_MAX = 33
        const val WARNING_BAND_MAX = 49
        const val SUCCESS_BAND_MAX = 66

        val SCALE = MarketIndicators.Index.SCALE
    }
}