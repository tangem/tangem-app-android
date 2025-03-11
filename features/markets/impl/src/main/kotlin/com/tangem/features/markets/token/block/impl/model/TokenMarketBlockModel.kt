package com.tangem.features.markets.token.block.impl.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.common.ui.charts.state.MarketChartData
import com.tangem.common.ui.charts.state.converter.PriceAndTimePointValuesConverter
import com.tangem.common.ui.charts.state.sorted
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetCurrencyQuotesUseCase
import com.tangem.domain.markets.GetTokenPriceChartUseCase
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.features.markets.token.block.TokenMarketBlockComponent
import com.tangem.features.markets.token.block.impl.ui.state.TokenMarketBlockUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ComponentScoped
internal class TokenMarketBlockModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val getTokenPriceChartUseCase: GetTokenPriceChartUseCase,
    private val getTokenQuotesUseCase: GetCurrencyQuotesUseCase,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
) : Model() {

    private val params = paramsContainer.require<TokenMarketBlockComponent.Params>()

    private val priceAndTimePointValuesConverter = PriceAndTimePointValuesConverter(needToFormatAxis = false)

    private val currentAppCurrency = getSelectedAppCurrencyUseCase()
        .map { maybeAppCurrency ->
            maybeAppCurrency.getOrElse { AppCurrency.Default }
        }.stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppCurrency.Default,
        )

    private var quotesState: QuotesState? = null
    private val quotesUpdateJobHolder = JobHolder()

    val state = MutableStateFlow(
        TokenMarketBlockUM(
            currencySymbol = params.cryptoCurrency.symbol,
            currentPrice = null,
            h24Percent = null,
            priceChangeType = PriceChangeType.NEUTRAL,
            chartData = null,
            onClick = ::navigateToMarketDetails,
        ),
    )

    init {
        startFetching()
    }

    private fun startFetching() {
        modelScope.launch {
            getTokenQuotesUseCase(
                currencyID = params.cryptoCurrency.id,
                interval = PriceChangeInterval.H24,
                refresh = true,
            ).collect {
                it.onSome { res ->
                    quotesState = QuotesState(
                        currentPrice = res.fiatRate,
                        h24Percent = res.priceChange,
                    )

                    state.value = state.value.copy(
                        currentPrice = BigDecimalFormatter.formatFiatPriceUncapped(
                            fiatAmount = res.fiatRate,
                            // TODO get currency from quotes use case [REDACTED_TASK_KEY]
                            fiatCurrencyCode = currentAppCurrency.value.code,
                            // TODO get currency from quotes use case [REDACTED_TASK_KEY]
                            fiatCurrencySymbol = currentAppCurrency.value.symbol,
                        ),
                        h24Percent = res.priceChange.format { percent() },
                        priceChangeType = PriceChangeType.fromBigDecimal(res.priceChange),
                    )
                }
            }
        }.saveIn(quotesUpdateJobHolder)

        val tokenId = params.cryptoCurrency.id.rawCurrencyId ?: return

        modelScope.launch(dispatchers.main) {
            val result = getTokenPriceChartUseCase(
                tokenId = tokenId,
                tokenSymbol = params.cryptoCurrency.symbol,
                interval = PriceChangeInterval.H24,
                appCurrency = currentAppCurrency.value, // TODO get currency from quotes use case [REDACTED_TASK_KEY]
                preview = true,
            )

            result.onRight { res ->
                // wait until quotes are loaded
                state.first { it.currentPrice != null }

                state.update { stateToUpdate ->
                    stateToUpdate.copy(
                        chartData = priceAndTimePointValuesConverter.convert(
                            MarketChartData.Data(
                                y = res.priceY.toImmutableList(),
                                x = res.timeStamps.map { it.toBigDecimal() }.toImmutableList(),
                            ).sorted(),
                        ),
                    )
                }
            }
        }
    }

    private fun navigateToMarketDetails() {
        val quotes = quotesState ?: return
        val tokenId = params.cryptoCurrency.id.rawCurrencyId ?: return

        val tokenParam = TokenMarketParams(
            id = tokenId,
            name = params.cryptoCurrency.name,
            imageUrl = params.cryptoCurrency.iconUrl,
            symbol = params.cryptoCurrency.symbol,
            tokenQuotes = TokenMarketParams.Quotes(
                currentPrice = quotes.currentPrice,
                h24Percent = quotes.h24Percent,
                weekPercent = null,
                monthPercent = null,
            ),
        )

        router.push(
            AppRoute.MarketsTokenDetails(
                token = tokenParam,
                appCurrency = currentAppCurrency.value,
                showPortfolio = false,
                analyticsParams = AppRoute.MarketsTokenDetails.AnalyticsParams(
                    blockchain = params.cryptoCurrency.network.name,
                    source = "Token",
                ),
            ),
        )
    }
}