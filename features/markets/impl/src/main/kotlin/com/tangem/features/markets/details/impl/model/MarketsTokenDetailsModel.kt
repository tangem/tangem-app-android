package com.tangem.features.markets.details.impl.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.common.ui.charts.state.*
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.*
import com.tangem.features.markets.component.BottomSheetState
import com.tangem.features.markets.details.api.MarketsTokenDetailsComponent
import com.tangem.features.markets.details.impl.model.converters.DescriptionConverter
import com.tangem.features.markets.details.impl.model.converters.TokenMarketInfoConverter
import com.tangem.features.markets.details.impl.model.formatter.*
import com.tangem.features.markets.details.impl.model.formatter.formatAsPrice
import com.tangem.features.markets.details.impl.model.formatter.getChangePercentBetween
import com.tangem.features.markets.details.impl.model.formatter.getPercentByInterval
import com.tangem.features.markets.details.impl.ui.state.InfoBottomSheetContent
import com.tangem.features.markets.details.impl.ui.state.MarketsTokenDetailsUM
import com.tangem.features.markets.impl.R
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LargeClass", "LongParameterList")
@Stable
internal class MarketsTokenDetailsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getTokenPriceChartUseCase: GetTokenPriceChartUseCase,
    private val getTokenMarketInfoUseCase: GetTokenMarketInfoUseCase,
    private val getTokenQuotesUseCase: GetTokenQuotesUseCase,
    private val urlOpener: UrlOpener,
) : Model() {

    private var quotesJob = JobHolder()
    private val params = paramsContainer.require<MarketsTokenDetailsComponent.Params>()

    private val currentAppCurrency = getSelectedAppCurrencyUseCase()
        .map { maybeAppCurrency ->
            maybeAppCurrency.getOrElse { AppCurrency.Default }
        }.stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = params.appCurrency,
        )

    private val infoConverter = TokenMarketInfoConverter(
        appCurrency = Provider { currentAppCurrency.value },
        onInfoClick = {
            showInfoBottomSheet(it)
        },
        onLinkClick = {
            urlOpener.openUrl(it.url)
        },
    )
    private val descriptionConverter = DescriptionConverter(
        onReadModeClicked = {
            showInfoBottomSheet(it)
        },
    )

    private val chartDataProducer = MarketChartDataProducer.build(dispatcher = dispatchers.default) {
        chartData = MarketChartData.NoData.Loading

        updateLook {
            val percentChangeType = params.token.tokenQuotes.h24Percent.percentChangeType()

            it.copy(
                type = percentChangeType.toChartType(),
                xAxisFormatter = MarketsDateTimeFormatters.getChartXFormatterByInterval(PriceChangeInterval.H24),
                yAxisFormatter = { value ->
                    BigDecimalFormatter.formatFiatPriceUncapped(
                        fiatAmount = value,
                        fiatCurrencyCode = currentAppCurrency.value.code,
                        fiatCurrencySymbol = "",
                    )
                },
            )
        }
    }

    private val currentQuotes = MutableStateFlow(
        TokenQuotes(
            currentPrice = params.token.tokenQuotes.currentPrice,
            h24ChangePercent = params.token.tokenQuotes.h24Percent,
            weekChangePercent = params.token.tokenQuotes.weekPercent,
            monthChangePercent = params.token.tokenQuotes.monthPercent,
            m3ChangePercent = null,
            m6ChangePercent = null,
            yearChangePercent = null,
            allTimeChangePercent = null,
        ),
    )

    private var lastUpdatedTimestamp: Long = DateTime.now().millis

    val containerBottomSheetState = MutableStateFlow(BottomSheetState.COLLAPSED)
    val isVisibleOnScreen = MutableStateFlow(false)

    val state = MutableStateFlow(
        MarketsTokenDetailsUM(
            tokenName = params.token.name,
            priceText = BigDecimalFormatter.formatFiatPriceUncapped(
                fiatAmount = params.token.tokenQuotes.currentPrice,
                fiatCurrencyCode = currentAppCurrency.value.code,
                fiatCurrencySymbol = currentAppCurrency.value.symbol,
            ),
            dateTimeText = resourceReference(R.string.common_today),
            priceChangePercentText = BigDecimalFormatter.formatPercent(
                percent = params.token.tokenQuotes.h24Percent,
                useAbsoluteValue = true,
            ),
            priceChangeType = if (params.token.tokenQuotes.h24Percent < BigDecimal.ZERO) {
                PriceChangeType.DOWN
            } else {
                PriceChangeType.UP
            },
            iconUrl = params.token.imageUrl,
            chartState = MarketsTokenDetailsUM.ChartState(
                dataProducer = chartDataProducer,
                chartLook = MarketChartLook(),
                onLoadRetryClick = ::onLoadRetryClicked,
                status = MarketsTokenDetailsUM.ChartState.Status.LOADING,
                onMarkerPointSelected = ::onMarkerPointSelected,
            ),
            selectedInterval = PriceChangeInterval.H24,
            onSelectedIntervalChange = ::onSelectedIntervalChange,
            markerSet = false,
            body = MarketsTokenDetailsUM.Body.Loading,
            triggerPriceChange = consumedEvent(),
            infoBottomSheet = TangemBottomSheetConfig(
                isShow = false,
                onDismissRequest = {},
                content = TangemBottomSheetConfigContent.Empty,
            ),
        ),
    )

    private val loadChartJobHolder = JobHolder()

    init {
        // reload screen if currency changed
        modelScope.launch {
            currentAppCurrency
                .filter { it != params.appCurrency }
                .collectLatest {
                    initialLoad()
                }
        }

        initialLoad()
    }

    private fun initialLoad() {
        loadInfo()
        loadChart(state.value.selectedInterval)
        modelScope.loadQuotesWithTimer(QUOTES_UPDATE_INTERVAL_MILLIS)
    }

    private fun loadQuotes() {
        modelScope.launch {
            val result = getTokenQuotesUseCase(
                tokenId = params.token.id,
                appCurrency = currentAppCurrency.value,
            )

            result.onRight { res ->
                updateQuotes(res)
            }
        }
    }

    private fun loadChart(interval: PriceChangeInterval) {
        modelScope.launch {
            state.update {
                it.copy(
                    chartState = it.chartState.copy(
                        status = MarketsTokenDetailsUM.ChartState.Status.LOADING,
                    ),
                )
            }

            chartDataProducer.runTransactionSuspend {
                chartData = MarketChartData.NoData.Loading
            }

            val chart = getTokenPriceChartUseCase.invoke(
                appCurrency = currentAppCurrency.value,
                interval = interval,
                tokenId = params.token.id,
            )

            state.update {
                it.copy(
                    selectedInterval = interval,
                    chartState = it.chartState.copy(
                        status = MarketsTokenDetailsUM.ChartState.Status.LOADING,
                    ),
                )
            }

            val xAxisFormatter = MarketsDateTimeFormatters.getChartXFormatterByInterval(state.value.selectedInterval)

            chart.onRight {
                chartDataProducer.runTransactionSuspend {
                    chartData = MarketChartData.Data(
                        x = it.timeStamps.map { it.toBigDecimal() }.toImmutableList(),
                        y = it.priceY.toImmutableList(),
                    )

                    updateLook {
                        it.copy(
                            xAxisFormatter = xAxisFormatter,
                            type = state.value.priceChangeType.toChartType(),
                        )
                    }
                }

                state.update {
                    it.copy(
                        chartState = it.chartState.copy(
                            status = MarketsTokenDetailsUM.ChartState.Status.DATA,
                        ),
                    )
                }
            }.onLeft {
                state.update {
                    it.copy(
                        chartState = it.chartState.copy(
                            status = MarketsTokenDetailsUM.ChartState.Status.ERROR,
                        ),
                        body = if (it.body is MarketsTokenDetailsUM.Body.Error) {
                            MarketsTokenDetailsUM.Body.Nothing
                        } else {
                            it.body
                        },
                    )
                }
            }
        }.saveIn(loadChartJobHolder)
    }

    private fun loadInfo() {
        state.update {
            it.copy(
                body = MarketsTokenDetailsUM.Body.Loading,
            )
        }

        modelScope.launch {
            val tokenMarketInfo = getTokenMarketInfoUseCase(
                appCurrency = currentAppCurrency.value,
                tokenId = params.token.id,
            )

            tokenMarketInfo.fold(
                ifRight = { result ->
                    currentQuotes.value = result.quotes
                    val percent = result.quotes.getPercentByInterval(interval = state.value.selectedInterval)
                    state.update {
                        it.copy(
                            priceText = result.quotes.currentPrice.formatAsPrice(currentAppCurrency.value),
                            priceChangePercentText = result.quotes.getFormattedPercentByInterval(
                                interval = it.selectedInterval,
                            ),
                            priceChangeType = percent.percentChangeType(),
                            body = MarketsTokenDetailsUM.Body.Content(
                                description = descriptionConverter.convert(result),
                                infoBlocks = infoConverter.convert(result),
                            ),
                        )
                    }

                    chartDataProducer.runTransaction {
                        updateLook {
                            it.copy(
                                type = getChartTypeByPercent(percent),
                            )
                        }
                    }
                },
                ifLeft = {
                    state.update {
                        if (it.chartState.status == MarketsTokenDetailsUM.ChartState.Status.DATA) {
                            it.copy(
                                body = MarketsTokenDetailsUM.Body.Error(
                                    onLoadRetryClick = ::onLoadRetryClicked,
                                ),
                            )
                        } else {
                            it.copy(
                                body = MarketsTokenDetailsUM.Body.Nothing,
                            )
                        }
                    }
                },
            )
        }
    }

    private suspend fun updateQuotes(newQuotes: TokenQuotes) {
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
        lastUpdatedTimestamp = DateTime.now().millis

        state.update { stateToUpdate ->
            stateToUpdate.copy(
                priceText = newQuotes.currentPrice.formatAsPrice(currentAppCurrency.value),
                priceChangePercentText = newQuotes.getFormattedPercentByInterval(
                    interval = stateToUpdate.selectedInterval,
                ),
                priceChangeType = priceChangeType,
                triggerPriceChange = trigger,
                dateTimeText = getDefaultDateTimeString(stateToUpdate.selectedInterval),
            )
        }

        chartDataProducer.runTransaction {
            updateLook {
                it.copy(
                    type = getChartTypeByPercent(percent),
                )
            }
        }
    }

    private fun onSelectedIntervalChange(interval: PriceChangeInterval) {
        if (state.value.selectedInterval == interval) return

        val quotes = currentQuotes.value
        val priceChangePercent = quotes.getFormattedPercentByInterval(interval)

        state.update {
            it.copy(
                priceChangePercentText = priceChangePercent,
                selectedInterval = interval,
                priceChangeType = quotes.getPercentByInterval(interval)?.percentChangeType()
                    ?: PriceChangeType.NEUTRAL,
                dateTimeText = getDefaultDateTimeString(interval),
            )
        }

        loadChart(interval)

        if (priceChangePercent.isEmpty()) {
            loadQuotes()
        }
    }

    @Suppress("MagicNumber")
    private fun onMarkerPointSelected(markerTimestamp: BigDecimal?, price: BigDecimal?) {
        val currentState = state.value

        val dateTimeText = markerTimestamp?.let {
            MarketsDateTimeFormatters.formatDateByIntervalWithMarker(
                interval = currentState.selectedInterval,
                markerTimestamp = it,
            )
        } ?: getDefaultDateTimeString(currentState.selectedInterval)

        val priceText = (price ?: currentQuotes.value.currentPrice).formatAsPrice(currentAppCurrency.value)

        val percent = price?.let {
            getChangePercentBetween(
                previousPrice = it,
                currentPrice = currentQuotes.value.currentPrice,
            )
        } ?: currentQuotes.value.getPercentByInterval(currentState.selectedInterval)

        val percentText = percent?.let {
            BigDecimalFormatter.formatPercent(
                percent = it,
                useAbsoluteValue = true,
            )
        } ?: ""

        state.update { stateToUpdate ->
            stateToUpdate.copy(
                markerSet = markerTimestamp != null,
                dateTimeText = dateTimeText,
                priceText = priceText,
                priceChangePercentText = percentText,
                priceChangeType = percent.percentChangeType(),
            )
        }

        chartDataProducer.runTransaction {
            updateLook {
                it.copy(
                    type = getChartTypeByPercent(percent),
                )
            }
        }
    }

    private fun showInfoBottomSheet(content: InfoBottomSheetContent) {
        state.update { stateToUpdate ->
            stateToUpdate.copy(
                infoBottomSheet = stateToUpdate.infoBottomSheet.copy(
                    isShow = true,
                    onDismissRequest = ::hideInfoBottomSheet,
                    content = content,
                ),
            )
        }
    }

    private fun hideInfoBottomSheet() {
        state.update { stateToUpdate ->
            stateToUpdate.copy(
                infoBottomSheet = stateToUpdate.infoBottomSheet.copy(
                    isShow = false,
                ),
            )
        }
    }

    private fun onLoadRetryClicked() {
        val currentState = state.value

        if (currentState.chartState.status == MarketsTokenDetailsUM.ChartState.Status.ERROR) {
            loadChart(currentState.selectedInterval)
        }

        if (currentState.body is MarketsTokenDetailsUM.Body.Error ||
            currentState.body is MarketsTokenDetailsUM.Body.Nothing
        ) {
            loadInfo()
            modelScope.loadQuotesWithTimer(QUOTES_UPDATE_INTERVAL_MILLIS)
        }
    }

    private fun CoroutineScope.loadQuotesWithTimer(timeMillis: Long) {
        launch {
            while (true) {
                delay(timeMillis)
                // Update quotes only when the container bottom sheet is in the expanded state
                containerBottomSheetState.first { it == BottomSheetState.EXPANDED }
                // and is visible on the screen
                isVisibleOnScreen.first { it }

                loadQuotes()
            }
        }.saveIn(quotesJob)
    }

    private fun getDefaultDateTimeString(interval: PriceChangeInterval): TextReference {
        return MarketsDateTimeFormatters.formatDateByInterval(
            interval = interval,
            startTimestamp = MarketsDateTimeFormatters.getStartTimestampByInterval(
                interval = interval,
                currentTimestamp = lastUpdatedTimestamp,
            ),
        )
    }

    private companion object {
        const val QUOTES_UPDATE_INTERVAL_MILLIS = 60000L
    }
}