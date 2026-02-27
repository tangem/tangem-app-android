package com.tangem.features.feed.model.feed.state.transformers

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.models.earn.EarnError
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.domain.models.earn.EarnTopToken
import com.tangem.features.feed.model.converter.EarnTokenWithCurrencyToListItemUMConverter
import com.tangem.features.feed.model.feed.analytics.FeedAnalyticsEvent
import com.tangem.features.feed.ui.earn.state.EarnListUM
import com.tangem.features.feed.ui.feed.state.FeedListUM
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

internal class UpdateEarnStateTransformer(
    private val isEarnEnabled: Boolean,
    private val onItemClick: (EarnTokenWithCurrency) -> Unit,
    private val onRetryClick: () -> Unit,
    private val earnResult: EarnTopToken?,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : FeedListUMTransformer {

    private val earnTokenWithCurrencyConverter = EarnTokenWithCurrencyToListItemUMConverter(onItemClick = onItemClick)

    override fun transform(prevState: FeedListUM): FeedListUM {
        if (!isEarnEnabled) return handleEmptyState(prevState)
        return when (earnResult) {
            null -> {
                handleEmptyState(prevState)
            }
            else -> {
                earnResult.fold(
                    ifLeft = { earnError ->
                        handleErrorState(
                            currentState = prevState,
                            result = earnError,
                        )
                    },
                    ifRight = { earnTokensWithCurrency ->
                        handleDataState(
                            currentState = prevState,
                            earnTokensWithCurrency = earnTokensWithCurrency,
                        )
                    },
                )
            }
        }
    }

    private fun handleEmptyState(currentState: FeedListUM): FeedListUM {
        return currentState.copy(earnListUM = EarnListUM.Empty)
    }

    private fun handleErrorState(currentState: FeedListUM, result: EarnError): FeedListUM {
        if (currentState.earnListUM != EarnListUM.Loading) return currentState
        val (code, message) = when (result) {
            is EarnError.HttpError -> result.code to result.message
            is EarnError.NotHttpError -> null to ""
        }
        analyticsEventHandler.send(
            FeedAnalyticsEvent.EarnLoadError(
                code = code,
                message = message,
            ),
        )
        return currentState.copy(earnListUM = EarnListUM.Error(onRetryClick))
    }

    private fun handleDataState(
        currentState: FeedListUM,
        earnTokensWithCurrency: List<EarnTokenWithCurrency>,
    ): FeedListUM {
        if (earnTokensWithCurrency.isEmpty()) return currentState.copy(earnListUM = EarnListUM.Empty)

        val newItems = earnTokensWithCurrency
            .sortedWith(
                compareByDescending<EarnTokenWithCurrency> {
                    it.earnToken.apy.toBigDecimalOrNull() ?: BigDecimal.ZERO
                }.thenBy { it.earnToken.tokenName },
            )
            .map(earnTokenWithCurrencyConverter::convert)
            .toPersistentList()

        val newEarnListUM = when (val earnState = currentState.earnListUM) {
            is EarnListUM.Content -> earnState.copy(items = newItems)
            else -> EarnListUM.Content(items = newItems)
        }

        return currentState.copy(
            earnListUM = newEarnListUM,
        )
    }
}