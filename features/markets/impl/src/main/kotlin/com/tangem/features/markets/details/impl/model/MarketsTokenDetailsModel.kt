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
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetTokenMarketInfoUseCase
import com.tangem.domain.markets.GetTokenPriceChartUseCase
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.features.markets.details.api.MarketsTokenDetailsComponent
import com.tangem.features.markets.details.impl.model.converters.DescriptionConverter
import com.tangem.features.markets.details.impl.model.converters.TokenMarketInfoConverter
import com.tangem.features.markets.details.impl.ui.state.InfoBottomSheetContent
import com.tangem.features.markets.details.impl.ui.state.MarketsTokenDetailsUM
import com.tangem.features.markets.impl.R
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

@Suppress("LargeClass")
@Stable
internal class MarketsTokenDetailsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getTokenPriceChartUseCase: GetTokenPriceChartUseCase,
    private val getTokenMarketInfoUseCase: GetTokenMarketInfoUseCase,
    private val urlOpener: UrlOpener,
) : Model() {

    val params = paramsContainer.require<MarketsTokenDetailsComponent.Params>()

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
            it.copy(
                type = getChartTypeByPercent(params.token.tokenQuotes.h24Percent),
                xAxisFormatter = { value ->
                    value.toLong().toTimeFormat(DateTimeFormatters.timeFormatter)
                },
                yAxisFormatter = { value ->
                    BigDecimalFormatter.formatFiatAmountUncapped(
                        fiatAmount = value,
                        fiatCurrencyCode = currentAppCurrency.value.code,
                        fiatCurrencySymbol = "",
                    )
                },
            )
        }
    }

    val state = MutableStateFlow(
        MarketsTokenDetailsUM(
            tokenName = params.token.name,
            priceText = BigDecimalFormatter.formatFiatAmountUncapped(
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
            body = MarketsTokenDetailsUM.Body.Loading,
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
        loadChart(state.value.selectedInterval)
        loadInfo()
    }

    private fun onSelectedIntervalChange(interval: PriceChangeInterval) {
        if (state.value.selectedInterval == interval) return

        state.update {
            it.copy(
                selectedInterval = interval,
                priceChangeType = PriceChangeType.UP,
            )
        }

        loadChart(interval)
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

            val xAxisFormatter = getFormatterByInterval(state.value.selectedInterval)

            chart.onRight {
                chartDataProducer.runTransactionSuspend {
                    chartData = MarketChartData.Data(
                        x = it.timeStamps.map { it.toBigDecimal() }.toImmutableList(),
                        y = it.priceY.toImmutableList(),
                    )

                    updateLook {
                        it.copy(
                            xAxisFormatter = xAxisFormatter,
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
                    state.update {
                        it.copy(
                            body = MarketsTokenDetailsUM.Body.Content(
                                description = descriptionConverter.convert(result),
                                infoBlocks = infoConverter.convert(result),
                            ),
                        )
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

    private fun getFormatterByInterval(interval: PriceChangeInterval): (BigDecimal) -> String {
        return when (interval) {
            PriceChangeInterval.H24 -> { value: BigDecimal ->
                value.toLong().toTimeFormat(DateTimeFormatters.timeFormatter)
            }
            PriceChangeInterval.WEEK,
            PriceChangeInterval.MONTH,
            PriceChangeInterval.MONTH3,
            PriceChangeInterval.MONTH6,
            -> { value ->
                value.toLong().toTimeFormat(DateTimeFormatters.dateMMMMd)
            }
            PriceChangeInterval.YEAR -> { value ->
                value.toLong().toTimeFormat(DateTimeFormatters.dateMMMMd)
            }
            PriceChangeInterval.ALL_TIME -> { value ->
                value.toLong().toTimeFormat(DateTimeFormatters.dateYYYY)
            }
        }
    }

    @Suppress("MagicNumber")
    private fun onMarkerPointSelected(time: BigDecimal?, price: BigDecimal?) {
        val timeText = time?.toLong()?.toTimeFormat(DateTimeFormatters.dateTimeFormatter)?.let {
            resourceReference(R.string.common_range, wrappedList(it, resourceReference(R.string.common_now)))
        } ?: resourceReference(R.string.common_today)

        val percent = price?.subtract(params.token.tokenQuotes.currentPrice)
            ?.divide(params.token.tokenQuotes.currentPrice, 4, RoundingMode.HALF_UP)
            ?.multiply(BigDecimal(-100))
            ?: params.token.tokenQuotes.h24Percent

        val percentText = BigDecimalFormatter.formatPercent(
            percent = percent,
            useAbsoluteValue = true,
        )

        state.update {
            it.copy(
                dateTimeText = timeText,
                priceText = BigDecimalFormatter.formatFiatAmountUncapped(
                    fiatAmount = price ?: params.token.tokenQuotes.currentPrice,
                    fiatCurrencyCode = currentAppCurrency.value.code,
                    fiatCurrencySymbol = currentAppCurrency.value.symbol,
                ),
                priceChangePercentText = percentText,
                priceChangeType = when {
                    percent < BigDecimal.ZERO -> PriceChangeType.DOWN
                    percent > BigDecimal.ZERO -> PriceChangeType.UP
                    else -> PriceChangeType.NEUTRAL
                },
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

    private fun getChartTypeByPercent(percent: BigDecimal): MarketChartLook.Type {
        return if (percent >= BigDecimal.ZERO) {
            MarketChartLook.Type.Growing
        } else {
            MarketChartLook.Type.Falling
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
        }
    }
}
