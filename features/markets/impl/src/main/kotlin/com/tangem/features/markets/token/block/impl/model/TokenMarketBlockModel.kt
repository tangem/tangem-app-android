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
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.*
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
            currencySymbol = params.tokenSymbol,
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
                currencyID = params.cryptoCurrencyID,
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
                            // TODO get currency from quotes use case AND-8022
                            fiatCurrencyCode = currentAppCurrency.value.code,
                            // TODO get currency from quotes use case AND-8022
                            fiatCurrencySymbol = currentAppCurrency.value.symbol,
                        ),
                        h24Percent = BigDecimalFormatter.formatPercent(
                            percent = res.priceChange,
                            useAbsoluteValue = true,
                        ),
                        priceChangeType = PriceChangeType.fromBigDecimal(res.priceChange),
                    )
                }
            }
        }.saveIn(quotesUpdateJobHolder)

        modelScope.launch(dispatchers.main) {
            val result = getTokenPriceChartUseCase(
                tokenId = params.tokenId,
                interval = PriceChangeInterval.H24,
                appCurrency = currentAppCurrency.value, // TODO get currency from quotes use case AND-8022
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

        val tokenParam = TokenMarketParams(
            id = params.tokenId,
            name = params.tokenSymbol,
            imageUrl = params.tokenImageUrl,
            symbol = params.tokenSymbol,
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
            ),
        )
    }
}
