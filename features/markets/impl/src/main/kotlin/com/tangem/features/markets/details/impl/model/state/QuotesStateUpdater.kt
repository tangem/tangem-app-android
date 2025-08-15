package com.tangem.features.markets.details.impl.model.state

import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.price
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenQuotes
import com.tangem.features.markets.details.impl.model.converters.PricePerformanceConverter
import com.tangem.features.markets.details.impl.model.formatter.*
import com.tangem.features.markets.details.impl.ui.state.MarketsTokenDetailsUM
import com.tangem.utils.Provider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import org.joda.time.DateTime
import java.math.BigDecimal

internal class QuotesStateUpdater(
    private val currentAppCurrency: Provider<AppCurrency>,
    private val state: MutableStateFlow<MarketsTokenDetailsUM>,
    private val currentQuotes: MutableStateFlow<TokenQuotes>,
    private val lastUpdatedTimestamp: MutableStateFlow<Long>,
    private val currentTokenInfo: MutableStateFlow<TokenMarketInfo?>,
    private val onPricePerformanceIntervalChanged: (PriceChangeInterval) -> Unit,
) {
    private val pricePerformanceConverter = PricePerformanceConverter(
        currentAppCurrency,
        onIntervalChanged = onPricePerformanceIntervalChanged,
    )

    suspend fun updateQuotes(newQuotes: TokenQuotes) {
        val triggerPriceChangeType = getFormattedPriceChange(
            currentPrice = currentQuotes.value.currentPrice,
            updatedPrice = newQuotes.currentPrice,
        )
        val trigger = if (triggerPriceChangeType != PriceChangeType.NEUTRAL) {
            triggeredEvent(
                data = triggerPriceChangeType,
                onConsume = {
                    state.update { it.copy(triggerPriceChange = consumedEvent()) }
                },
            )
        } else {
            consumedEvent()
        }

        val percent = newQuotes.getPercentByInterval(interval = state.value.selectedInterval)
        val priceChangeType = percent.percentChangeType()

        // wait until marker is removed
        state.first { it.markerSet.not() }

        currentQuotes.value = newQuotes
        lastUpdatedTimestamp.value = DateTime.now().millis

        state.update { stateToUpdate ->
            stateToUpdate.copy(
                priceText = newQuotes.currentPrice.format {
                    fiat(
                        fiatCurrencySymbol = currentAppCurrency().symbol,
                        fiatCurrencyCode = currentAppCurrency().code,
                    ).price()
                },
                priceChangePercentText = newQuotes.getFormattedPercentByInterval(
                    interval = stateToUpdate.selectedInterval,
                ),
                priceChangeType = priceChangeType,
                triggerPriceChange = trigger,
                dateTimeText = MarketsDateTimeFormatters.getDefaultDateTimeString(
                    stateToUpdate.selectedInterval,
                    currentTimestamp = lastUpdatedTimestamp.value,
                ),
                body = stateToUpdate.body.updatePricePerformance(newQuotes.currentPrice),
            )
        }
    }

    private fun MarketsTokenDetailsUM.Body.updatePricePerformance(price: BigDecimal): MarketsTokenDetailsUM.Body {
        val currentPricePerformance = currentTokenInfo.value?.pricePerformance ?: return this

        return if (this is MarketsTokenDetailsUM.Body.Content) {
            copy(
                infoBlocks = infoBlocks.copy(
                    pricePerformance = pricePerformanceConverter.convert(currentPricePerformance, price),
                ),
            )
        } else {
            this
        }
    }
}