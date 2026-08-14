package com.tangem.features.foryou.impl.tokensummaryblock.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.markets.FetchCoinIndicatorsUseCase
import com.tangem.domain.markets.GetCoinIndicatorsUpdatesUseCase
import com.tangem.features.foryou.TokenSummaryBlockComponent
import com.tangem.features.foryou.impl.components.state.AiInsightUM
import com.tangem.features.foryou.impl.tokensummary.entity.TokenSentimentUM
import com.tangem.features.foryou.impl.tokensummary.model.converter.TokenSentimentConverter
import com.tangem.features.foryou.impl.tokensummaryblock.entity.TokenSummaryBlockUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Model for the embeddable [TokenSummaryBlockComponent].
 *
 * Fetches the coin indicators once (all timeframes at once) and re-derives the sentiment card whenever the parent
 * screen changes the selected period. The period is already capped at month by the parent.
 * AI insight has no data source yet, so it stays [AiInsightUM.Hide].
 */
@Stable
@ModelScoped
internal class TokenSummaryBlockModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val fetchCoinIndicatorsUseCase: FetchCoinIndicatorsUseCase,
    getCoinIndicatorsUpdatesUseCase: GetCoinIndicatorsUpdatesUseCase,
) : Model() {

    private val params = paramsContainer.require<TokenSummaryBlockComponent.Params>()

    val uiState: StateFlow<TokenSummaryBlockUM>
        field = MutableStateFlow(
            TokenSummaryBlockUM(
                sentiment = TokenSentimentUM.Loading,
                aiInsight = AiInsightUM.Hide,
                onClick = ::onClick,
            ),
        )

    init {
        modelScope.launch {
            fetchCoinIndicatorsUseCase(symbols = listOf(params.symbol))
        }

        combine(
            getCoinIndicatorsUpdatesUseCase().map { it[params.symbol.uppercase()] }.distinctUntilChanged(),
            params.selectedPeriod,
            params.isHostLoading.distinctUntilChanged(),
        ) { coinIndicators, period, isHostLoading ->
            uiState.update { state ->
                state.copy(
                    sentiment = when {
                        isHostLoading -> TokenSentimentUM.Loading
                        coinIndicators != null ->
                            TokenSentimentConverter(timeframe = period.timeframe).convert(coinIndicators)
                        else -> TokenSentimentUM.Empty.NoResponse
                    },
                )
            }
        }.launchIn(modelScope)
    }

    private fun onClick() {
        params.callbacks.onClick()
    }
}