package com.tangem.features.feed.model.market.details

import androidx.compose.runtime.Stable
import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.ui.charts.state.MarketChartData
import com.tangem.common.ui.charts.state.MarketChartDataProducer
import com.tangem.common.ui.charts.state.sorted
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.format.bigdecimal.price
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.card.common.extensions.hotWalletExcludedBlockchains
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.markets.*
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.news.model.NewsListConfig
import com.tangem.domain.news.usecase.GetNewsUseCase
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.domain.settings.usercountry.models.needApplyFCARestrictions
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.feed.components.market.details.DefaultMarketsTokenDetailsComponent
import com.tangem.features.feed.components.market.details.analytics.MarketTokenAnalyticsEvent
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.model.converter.ShortArticleToArticleConfigUMConverter
import com.tangem.features.feed.model.market.details.analytics.MarketDetailsAnalyticsEvent
import com.tangem.features.feed.model.market.details.converter.DescriptionConverter
import com.tangem.features.feed.model.market.details.converter.ExchangeItemStateConverter
import com.tangem.features.feed.model.market.details.converter.TokenMarketInfoConverter
import com.tangem.features.feed.model.market.details.formatter.*
import com.tangem.features.feed.model.market.details.state.QuotesStateUpdater
import com.tangem.features.feed.model.market.details.state.TokenNetworksState
import com.tangem.features.feed.ui.market.detailed.state.ExchangesBottomSheetContent
import com.tangem.features.feed.ui.market.detailed.state.MarketsTokenDetailsUM
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import java.math.BigDecimal
import java.util.Locale
import javax.inject.Inject

@Suppress("LargeClass", "LongParameterList")
@Stable
@ModelScoped
internal class MarketsTokenDetailsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    getUserCountryUseCase: GetUserCountryUseCase,
    paramsContainer: ParamsContainer,
    private val getTokenPriceChartUseCase: GetTokenPriceChartUseCase,
    private val getTokenMarketInfoUseCase: GetTokenMarketInfoUseCase,
    private val getTokenFullQuotesUseCase: GetTokenFullQuotesUseCase,
    private val getTokenExchangesUseCase: GetTokenExchangesUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val getUserWalletsUseCase: GetWalletsUseCase,
    private val excludedBlockchains: ExcludedBlockchains,
    private val urlOpener: UrlOpener,
    private val getNewsUseCase: GetNewsUseCase,
    private val shareManager: ShareManager,
) : Model() {

    private val quotesJob = JobHolder()
    private var userCountry: UserCountry? = null
    private val params = paramsContainer.require<DefaultMarketsTokenDetailsComponent.Params>()
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
        onSecurityScoreInfoClick = { content ->
            showBottomSheet(content)

            // === Analytics ===
            analyticsEventHandler.send(analyticsEventBuilder.securityScoreOpened())
        },
        onSecurityScoreProviderLinkClick = { securityScoreProviderUM ->
            securityScoreProviderUM.urlData?.fullUrl?.let { url ->
                urlOpener.openUrl(url)
            }

            // === Analytics ===
            analyticsEventHandler.send(analyticsEventBuilder.securityScoreProviderClicked(securityScoreProviderUM.name))
        },
        // === Analytics ===
        onPricePerformanceIntervalChanged = { interval ->
            analyticsEventHandler.send(
                analyticsEventBuilder.intervalChanged(
                    intervalType = MarketDetailsAnalyticsEvent.IntervalType.PricePerformance,
                    interval = interval,
                ),
            )
        },
        onInsightsIntervalChanged = { interval ->
            analyticsEventHandler.send(
                analyticsEventBuilder.intervalChanged(
                    intervalType = MarketDetailsAnalyticsEvent.IntervalType.Insights,
                    interval = interval,
                ),
            )
        },
        needApplyFCARestrictions = Provider {
            userCountry.needApplyFCARestrictions()
        },
        // ==================
    )

    private val shortArticleToArticleConfigUMConverter by lazy {
        ShortArticleToArticleConfigUMConverter(isTrending = Provider { false })
    }

    private val descriptionConverter = DescriptionConverter(
        onReadModeClicked = { content ->
            showBottomSheet(content)
            // === Analytics ===
            analyticsEventHandler.send(analyticsEventBuilder.readMoreClicked())
        },
        needApplyFCARestrictions = Provider {
            userCountry.needApplyFCARestrictions()
        },
        onGeneratedAINotificationClick = {
            modelScope.launch {
                sendFeedbackEmailUseCase(
                    type = FeedbackEmailType.CurrencyDescriptionError(
                        currencyId = params.token.id.value,
                        currencyName = params.token.name,
                    ),
                )
            }
        },
    )

    private val chartDataProducer = MarketChartDataProducer.build(dispatcher = dispatchers.default) {
        chartData = MarketChartData.NoData.Loading

        updateLook { marketChartLook ->
            val percentChangeType = params.token.tokenQuotes.h24Percent.percentChangeType()

            marketChartLook.copy(
                type = percentChangeType.toChartType(),
                xAxisFormatter = MarketsDateTimeFormatters.getChartXFormatterByInterval(PriceChangeInterval.H24),
                yAxisFormatter = { value ->
                    value.format {
                        fiat(
                            fiatCurrencyCode = currentAppCurrency.value.code,
                            fiatCurrencySymbol = currentAppCurrency.value.symbol,
                        ).price()
                    }
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
            priceText = params.token.tokenQuotes.currentPrice.format {
                fiat(
                    fiatCurrencyCode = currentAppCurrency.value.code,
                    fiatCurrencySymbol = currentAppCurrency.value.symbol,
                ).price()
            },
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
            isMarkerSet = false,
            body = MarketsTokenDetailsUM.Body.Loading,
            triggerPriceChange = consumedEvent(),
            bottomSheetConfig = TangemBottomSheetConfig(
                isShown = false,
                onDismissRequest = {},
                content = TangemBottomSheetConfigContent.Empty,
            ),
            shouldShowPriceSubtitle = false,
            onShouldShowPriceSubtitleChange = ::onShouldShowPriceSubtitleChange,
            relatedNews = MarketsTokenDetailsUM.RelatedNews(
                articles = persistentListOf(),
                onArticledClicked = {},
                onFirstVisible = {},
                onScroll = {},
            ),
            onShareClick = ::onShareClick,
        ),
    )

    private val quotesStateUpdater = QuotesStateUpdater(
        currentAppCurrency = Provider { currentAppCurrency.value },
        state = state,
        currentQuotes = currentQuotes,
        lastUpdatedTimestamp = lastUpdatedTimestamp,
        currentTokenInfo = currentTokenInfo,
        onPricePerformanceIntervalChanged = { interval ->
            analyticsEventHandler.send(
                analyticsEventBuilder.intervalChanged(
                    intervalType = MarketDetailsAnalyticsEvent.IntervalType.PricePerformance,
                    interval = interval,
                ),
            )
        },
    )

    private val loadChartJobHolder = JobHolder()

    init {
        userCountry = getUserCountryUseCase.invokeSync().getOrNull()
            ?: UserCountry.Other(Locale.getDefault().country)
        // reload screen if currency changed
        modelScope.launch {
            currentAppCurrency
                .filter { it != params.appCurrency }
                .collectLatest {
                    initialLoad()
                }
        }

        initialLoad()
        loadRelatedNews()
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

    private fun loadRelatedNews() {
        modelScope.launch(dispatchers.default) {
            getNewsUseCase.getNews(
                limit = RELATED_NEWS_LIMIT,
                newsListConfig = NewsListConfig(
                    language = Locale.getDefault().language,
                    snapshot = null,
                    tokenIds = listOf(params.token.id.value),
                ),
            ).collect { result ->
                result.onRight { articles ->
                    state.update { marketsTokenDetailsUM ->
                        val relatedNews = shortArticleToArticleConfigUMConverter.convert(articles)
                        marketsTokenDetailsUM.copy(
                            relatedNews = marketsTokenDetailsUM.relatedNews.copy(
                                articles = relatedNews,
                                onArticledClicked = { articledId ->
                                    params.onArticleClick(
                                        /* articledId */ articledId,
                                        /* preselectedIds */ relatedNews.map { it.id },
                                    )
                                },
                                onFirstVisible = {
                                    analyticsEventHandler.send(
                                        MarketTokenAnalyticsEvent.TokenNewsViewed(
                                            tokenSymbol = params.token.symbol,
                                        ),
                                    )
                                },
                                onScroll = {
                                    analyticsEventHandler.send(
                                        MarketTokenAnalyticsEvent.TokenNewsCarouselScrolled(
                                            tokenSymbol = params.token.symbol,
                                        ),
                                    )
                                },
                            ),
                        )
                    }
                }.onLeft { throwable ->
                    val (code, message) = if (throwable is ApiResponseError.HttpException) {
                        throwable.code.numericCode to throwable.message.orEmpty()
                    } else {
                        null to ""
                    }
                    analyticsEventHandler.send(
                        MarketTokenAnalyticsEvent.TokenNewsLoadError(
                            tokenSymbol = params.token.symbol,
                            code = code,
                            message = message,
                        ),
                    )
                }
            }
        }
    }

    private fun loadChart(interval: PriceChangeInterval) {
        modelScope.launch {
            state.update { marketsTokenDetailsUM ->
                marketsTokenDetailsUM.copy(
                    chartState = marketsTokenDetailsUM.chartState.copy(
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

            state.update { marketsTokenDetailsUM ->
                marketsTokenDetailsUM.copy(
                    selectedInterval = interval,
                    chartState = marketsTokenDetailsUM.chartState.copy(
                        status = MarketsTokenDetailsUM.ChartState.Status.LOADING,
                    ),
                )
            }

            chart
                .onRight { updateTokenChart(it) }
                .onLeft {
                    state.update { marketsTokenDetailsUM ->
                        marketsTokenDetailsUM.copy(
                            chartState = marketsTokenDetailsUM.chartState.copy(
                                status = MarketsTokenDetailsUM.ChartState.Status.ERROR,
                            ),
                            body = if (marketsTokenDetailsUM.body is MarketsTokenDetailsUM.Body.Error) {
                                MarketsTokenDetailsUM.Body.Nothing
                            } else {
                                marketsTokenDetailsUM.body
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

            updateLook { marketChartLook ->
                marketChartLook.copy(
                    xAxisFormatter = xAxisFormatter,
                    type = state.value.priceChangeType.toChartType(),
                )
            }
        }

        state.update { marketsTokenDetailsUM ->
            marketsTokenDetailsUM.copy(
                chartState = marketsTokenDetailsUM.chartState.copy(
                    status = MarketsTokenDetailsUM.ChartState.Status.DATA,
                ),
                body = if (marketsTokenDetailsUM.body is MarketsTokenDetailsUM.Body.Nothing) {
                    MarketsTokenDetailsUM.Body.Error(onLoadRetryClick = ::onLoadRetryClicked)
                } else {
                    marketsTokenDetailsUM.body
                },
            )
        }
    }

    private fun loadInfo() {
        state.update { marketsTokenDetailsUM ->
            marketsTokenDetailsUM.copy(
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
                    state.update { marketsTokenDetailsUM ->
                        if (marketsTokenDetailsUM.chartState.status == MarketsTokenDetailsUM.ChartState.Status.DATA) {
                            marketsTokenDetailsUM.copy(
                                body = MarketsTokenDetailsUM.Body.Error(
                                    onLoadRetryClick = ::onLoadRetryClicked,
                                ),
                            )
                        } else {
                            marketsTokenDetailsUM.copy(
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

        state.update { marketsTokenDetailsUM ->
            marketsTokenDetailsUM.copy(
                priceText = newInfo.quotes.currentPrice.format {
                    fiat(
                        fiatCurrencySymbol = currentAppCurrency.value.symbol,
                        fiatCurrencyCode = currentAppCurrency.value.code,
                    ).price()
                },
                priceChangePercentText = newInfo.quotes.getFormattedPercentByInterval(
                    interval = marketsTokenDetailsUM.selectedInterval,
                ),
                priceChangeType = percent.percentChangeType(),
                body = MarketsTokenDetailsUM.Body.Content(
                    description = descriptionConverter.convert(newInfo),
                    infoBlocks = infoConverter.convert(newInfo),
                ),
            )
        }

        val isAllWalletsIsHot = getUserWalletsUseCase.invokeSync().all { it is UserWallet.Hot }

        val networks = newInfo.networks?.filter { network ->
            BlockchainUtils.isSupportedNetworkId(
                blockchainId = network.networkId,
                excludedBlockchains = excludedBlockchains,
                hotExcludedBlockchains = hotWalletExcludedBlockchains,
                hasOnlyHotWallets = isAllWalletsIsHot,
            )
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

        state.update { marketsTokenDetailsUM ->
            marketsTokenDetailsUM.copy(
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

    private fun onShouldShowPriceSubtitleChange(shouldShow: Boolean) {
        state.update { marketsTokenDetailsUM ->
            marketsTokenDetailsUM.copy(shouldShowPriceSubtitle = shouldShow)
        }
    }

    @Suppress("MagicNumber")
    private fun onMarkerPointSelected(markerTimestamp: BigDecimal?, price: BigDecimal?) {
        val currentState = state.value

        val dateTimeText = markerTimestamp?.let { bigDecimal ->
            MarketsDateTimeFormatters.formatDateByIntervalWithMarker(
                interval = currentState.selectedInterval,
                markerTimestamp = bigDecimal,
            )
        } ?: getDefaultDateTimeString(currentState.selectedInterval)

        val priceText = (price ?: currentQuotes.value.currentPrice).format {
            fiat(
                fiatCurrencySymbol = currentAppCurrency.value.symbol,
                fiatCurrencyCode = currentAppCurrency.value.code,
            ).price()
        }

        val percent = price?.let { bigDecimal ->
            getChangePercentBetween(
                previousPrice = bigDecimal,
                currentPrice = currentQuotes.value.currentPrice,
            )
        } ?: currentQuotes.value.getPercentByInterval(currentState.selectedInterval)

        val percentText = percent?.format { percent() }.orEmpty()

        state.update { stateToUpdate ->
            stateToUpdate.copy(
                isMarkerSet = markerTimestamp != null,
                dateTimeText = dateTimeText,
                priceText = priceText,
                priceChangePercentText = percentText,
                priceChangeType = percent.percentChangeType(),
            )
        }

        chartDataProducer.runTransaction {
            updateLook { marketChartLook ->
                marketChartLook.copy(
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

    private fun onShareClick() {
        val tokenId = params.token.id.value
        val shareUrl = "$CRYPTOCURRENCIES_BASE_URL$tokenId"
        shareManager.shareText(shareUrl)
        analyticsEventHandler.send(analyticsEventBuilder.shareClicked())
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
                ifLeft = { _ ->
                    ExchangesBottomSheetContent.Error(onRetryClick = { onListedOnClick(exchangesCount) })
                },
                ifRight = { list ->
                    ExchangesBottomSheetContent.Content(
                        exchangeItems = ExchangeItemStateConverter.convertList(list).toImmutableList(),
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
        const val RELATED_NEWS_LIMIT = 10
        const val CRYPTOCURRENCIES_BASE_URL = "https://tangem.com/en/cryptocurrencies/"
    }
}