package com.tangem.features.markets.details.impl.model

import androidx.compose.runtime.Stable
import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.ui.charts.state.MarketChartData
import com.tangem.common.ui.charts.state.MarketChartDataProducer
import com.tangem.common.ui.charts.state.sorted
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.markets.*
import com.tangem.features.markets.details.MarketsTokenDetailsComponent
import com.tangem.features.markets.details.impl.analytics.MarketDetailsAnalyticsEvent
import com.tangem.features.markets.details.impl.model.converters.DescriptionConverter
import com.tangem.features.markets.details.impl.model.converters.ExchangeItemStateConverter
import com.tangem.features.markets.details.impl.model.converters.TokenMarketInfoConverter
import com.tangem.features.markets.details.impl.model.formatter.*
import com.tangem.features.markets.details.impl.model.state.QuotesStateUpdater
import com.tangem.features.markets.details.impl.model.state.TokenNetworksState
import com.tangem.features.markets.details.impl.ui.state.ExchangesBottomSheetContent
import com.tangem.features.markets.details.impl.ui.state.MarketsTokenDetailsUM
import com.tangem.features.markets.impl.R
import com.tangem.lib.crypto.BlockchainUtils
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
@ComponentScoped
internal class MarketsTokenDetailsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getTokenPriceChartUseCase: GetTokenPriceChartUseCase,
    private val getTokenMarketInfoUseCase: GetTokenMarketInfoUseCase,
    private val getTokenFullQuotesUseCase: GetTokenFullQuotesUseCase,
    private val getTokenExchangesUseCase: GetTokenExchangesUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val urlOpener: UrlOpener,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val excludedBlockchains: ExcludedBlockchains,
) : Model() {

    private var quotesJob = JobHolder()
    private val params = paramsContainer.require<MarketsTokenDetailsComponent.Params>()
    private val analyticsEventBuilder = MarketDetailsAnalyticsEvent.EventBuilder(token = params.token)

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
        onInfoClick = { showBottomSheet(it) },
        onListedOnClick = ::onListedOnClick,
        onLinkClick = { link ->
            urlOpener.openUrl(link.url)
            // === Analytics ===
            analyticsEventHandler.send(analyticsEventBuilder.linkClicked(linkTitle = link.title))
        },
        onSecurityScoreInfoClick = {
            showBottomSheet(it)

            // === Analytics ===
            analyticsEventHandler.send(analyticsEventBuilder.securityScoreOpened())
        },
        onSecurityScoreProviderLinkClick = {
            it.urlData?.fullUrl?.let { url ->
                urlOpener.openUrl(url)
            }

            // === Analytics ===
            analyticsEventHandler.send(analyticsEventBuilder.securityScoreProviderClicked(it.name))
        },
        // === Analytics ===
        onPricePerformanceIntervalChanged = {
            analyticsEventHandler.send(
                analyticsEventBuilder.intervalChanged(
                    intervalType = MarketDetailsAnalyticsEvent.IntervalType.PricePerformance,
                    interval = it,
                ),
            )
        },
        onInsightsIntervalChanged = {
            analyticsEventHandler.send(
                analyticsEventBuilder.intervalChanged(
                    intervalType = MarketDetailsAnalyticsEvent.IntervalType.Insights,
                    interval = it,
                ),
            )
        },
        // ==================
    )

    private val descriptionConverter = DescriptionConverter(
        onReadModeClicked = {
            showBottomSheet(it)
            // === Analytics ===
            analyticsEventHandler.send(analyticsEventBuilder.readMoreClicked())
        },
        onGeneratedAINotificationClick = {
            modelScope.launch {
                sendFeedbackEmailUseCase(
                    type = FeedbackEmailType.CurrencyDescriptionError(
                        currencyId = params.token.id,
                        currencyName = params.token.name,
                    ),
                )
            }
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
                        fiatCurrencySymbol = currentAppCurrency.value.symbol,
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

    private val currentTokenInfo = MutableStateFlow<TokenMarketInfo?>(null)
    private val lastUpdatedTimestamp = MutableStateFlow(DateTime.now().millis)

    val isVisibleOnScreen = MutableStateFlow(false)
    val networksState = MutableStateFlow<TokenNetworksState>(TokenNetworksState.Loading)

    val state = MutableStateFlow(
        MarketsTokenDetailsUM(
            tokenName = params.token.name,
            priceText = BigDecimalFormatter.formatFiatPriceUncapped(
                fiatAmount = params.token.tokenQuotes.currentPrice,
                fiatCurrencyCode = currentAppCurrency.value.code,
                fiatCurrencySymbol = currentAppCurrency.value.symbol,
            ),
            dateTimeText = resourceReference(R.string.common_today),
            priceChangePercentText = params.token.tokenQuotes.h24Percent?.format { percent() },
            priceChangeType = params.token.tokenQuotes.h24Percent.percentChangeType(),
            iconUrl = params.token.imageUrl,
            chartState = MarketsTokenDetailsUM.ChartState(
                dataProducer = chartDataProducer,
                onLoadRetryClick = ::onLoadRetryClicked,
                status = MarketsTokenDetailsUM.ChartState.Status.LOADING,
                onMarkerPointSelected = ::onMarkerPointSelected,
            ),
            selectedInterval = PriceChangeInterval.H24,
            onSelectedIntervalChange = ::onSelectedIntervalChange,
            markerSet = false,
            body = MarketsTokenDetailsUM.Body.Loading,
            triggerPriceChange = consumedEvent(),
            bottomSheetConfig = TangemBottomSheetConfig(
                isShown = false,
                onDismissRequest = {},
                content = TangemBottomSheetConfigContent.Empty,
            ),
        ),
    )

    private val quotesStateUpdater = QuotesStateUpdater(
        currentAppCurrency = Provider { currentAppCurrency.value },
        state = state,
        currentQuotes = currentQuotes,
        lastUpdatedTimestamp = lastUpdatedTimestamp,
        currentTokenInfo = currentTokenInfo,
        onPricePerformanceIntervalChanged = {
            analyticsEventHandler.send(
                analyticsEventBuilder.intervalChanged(
                    intervalType = MarketDetailsAnalyticsEvent.IntervalType.PricePerformance,
                    interval = it,
                ),
            )
        },
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
            val result = getTokenFullQuotesUseCase(
                tokenId = params.token.id,
                appCurrency = currentAppCurrency.value,
                tokenSymbol = params.token.symbol,
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
                tokenSymbol = params.token.symbol,
                preview = false,
            )

            state.update {
                it.copy(
                    selectedInterval = interval,
                    chartState = it.chartState.copy(
                        status = MarketsTokenDetailsUM.ChartState.Status.LOADING,
                    ),
                )
            }

            chart
                .onRight { updateTokenChart(it) }
                .onLeft {
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

    private suspend fun updateTokenChart(tokenChart: TokenChart) {
        val xAxisFormatter = MarketsDateTimeFormatters.getChartXFormatterByInterval(state.value.selectedInterval)

        chartDataProducer.runTransactionSuspend {
            chartData = MarketChartData.Data(
                y = tokenChart.priceY.toImmutableList(),
                x = tokenChart.timeStamps.map { it.toBigDecimal() }.toImmutableList(),
            ).sorted()

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
                body = if (it.body is MarketsTokenDetailsUM.Body.Nothing) {
                    MarketsTokenDetailsUM.Body.Error(onLoadRetryClick = ::onLoadRetryClicked)
                } else {
                    it.body
                },
            )
        }
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
                tokenSymbol = params.token.symbol,
            )

            tokenMarketInfo.fold(
                ifRight = { result -> updateInfo(result) },
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

    private fun updateInfo(newInfo: TokenMarketInfo) {
        lastUpdatedTimestamp.value = DateTime.now().millis

        currentTokenInfo.value = newInfo
        currentQuotes.value = newInfo.quotes

        val percent = newInfo.quotes.getPercentByInterval(interval = state.value.selectedInterval)

        state.update {
            it.copy(
                priceText = newInfo.quotes.currentPrice.formatAsPrice(currentAppCurrency.value),
                priceChangePercentText = newInfo.quotes.getFormattedPercentByInterval(
                    interval = it.selectedInterval,
                ),
                priceChangeType = percent.percentChangeType(),
                body = MarketsTokenDetailsUM.Body.Content(
                    description = descriptionConverter.convert(newInfo),
                    infoBlocks = infoConverter.convert(newInfo),
                ),
            )
        }

        val networks = newInfo.networks?.filter {
            BlockchainUtils.isSupportedNetworkId(it.networkId, excludedBlockchains)
        }

        networksState.value = if (networks.isNullOrEmpty()) {
            TokenNetworksState.NoNetworksAvailable
        } else {
            TokenNetworksState.NetworksAvailable(networks)
        }

        chartDataProducer.runTransaction {
            updateLook {
                it.copy(type = percent.percentChangeType().toChartType())
            }
        }
    }

    private suspend fun updateQuotes(newQuotes: TokenQuotes) {
        val populatedNewQuotes = currentQuotes.value.populateWith(newQuotes)

        quotesStateUpdater.updateQuotes(newQuotes = populatedNewQuotes)

        val percent = populatedNewQuotes
            .getPercentByInterval(interval = state.value.selectedInterval)

        chartDataProducer.runTransaction {
            updateLook {
                it.copy(type = percent.percentChangeType().toChartType())
            }
        }
    }

    private fun onSelectedIntervalChange(interval: PriceChangeInterval) {
        if (state.value.selectedInterval == interval) return

        // === Analytics ===
        analyticsEventHandler.send(
            analyticsEventBuilder.intervalChanged(
                intervalType = MarketDetailsAnalyticsEvent.IntervalType.Chart,
                interval = interval,
            ),
        )
        // ==================

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

        val percentText = percent?.format { percent() } ?: ""

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
                    type = percent.percentChangeType().toChartType(),
                )
            }
        }
    }

    private fun showBottomSheet(content: TangemBottomSheetConfigContent) {
        state.update { stateToUpdate ->
            stateToUpdate.copy(
                bottomSheetConfig = stateToUpdate.bottomSheetConfig.copy(
                    isShown = true,
                    onDismissRequest = ::hideBottomSheet,
                    content = content,
                ),
            )
        }
    }

    private fun hideBottomSheet() {
        state.update { stateToUpdate ->
            stateToUpdate.copy(
                bottomSheetConfig = stateToUpdate.bottomSheetConfig.copy(isShown = false),
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

    private fun onListedOnClick(exchangesCount: Int) {
        modelScope.launch {
            analyticsEventHandler.send(analyticsEventBuilder.exchangesScreenOpened())

            showBottomSheet(content = ExchangesBottomSheetContent.Loading(exchangesCount))

            val maybeExchanges = getTokenExchangesUseCase(tokenId = params.token.id)

            // Delay to show the bottom sheet
            delay(timeMillis = 400L)

            updateExchangeBSContent(maybeExchanges = maybeExchanges, exchangesCount = exchangesCount)
        }
    }

    private fun updateExchangeBSContent(
        maybeExchanges: Either<Throwable, List<TokenMarketExchange>>,
        exchangesCount: Int,
    ) {
        val content = maybeExchanges
            .fold(
                ifLeft = {
                    ExchangesBottomSheetContent.Error(onRetryClick = { onListedOnClick(exchangesCount) })
                },
                ifRight = {
                    ExchangesBottomSheetContent.Content(
                        exchangeItems = ExchangeItemStateConverter.convertList(it).toImmutableList(),
                    )
                },
            )

        state.update { stateToUpdate ->
            stateToUpdate.copy(
                bottomSheetConfig = stateToUpdate.bottomSheetConfig.copy(content = content),
            )
        }
    }

    private fun CoroutineScope.loadQuotesWithTimer(timeMillis: Long) {
        launch {
            while (true) {
                delay(timeMillis)
                // Update quotes only when content is visible on the screen
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
                currentTimestamp = lastUpdatedTimestamp.value,
            ),
        )
    }

    private companion object {
        const val QUOTES_UPDATE_INTERVAL_MILLIS = 60000L
    }
}