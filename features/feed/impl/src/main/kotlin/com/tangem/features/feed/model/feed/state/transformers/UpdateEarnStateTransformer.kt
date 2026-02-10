package com.tangem.features.feed.model.feed.state.transformers

import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.models.earn.EarnRewardType
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.domain.models.earn.EarnTopToken
import com.tangem.domain.models.earn.EarnType
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.earn.state.EarnListItemUM
import com.tangem.features.feed.ui.earn.state.EarnListUM
import com.tangem.features.feed.ui.feed.state.FeedListUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

internal class UpdateEarnStateTransformer(
    private val isEarnEnabled: Boolean,
    private val onItemClick: (EarnTokenWithCurrency) -> Unit,
    private val onRetryClick: () -> Unit,
    private val earnResult: EarnTopToken?,
) : FeedListUMTransformer {

    private val earnTokenWithCurrencyConverter = EarnTokenWithCurrencyConverter(onItemClick = onItemClick)

    override fun transform(prevState: FeedListUM): FeedListUM {
        if (!isEarnEnabled) return handleEmptyState(prevState)
        return when (earnResult) {
            null -> {
                handleEmptyState(prevState)
            }
            else -> {
                earnResult.fold(
                    ifLeft = {
                        handleErrorState(prevState)
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
        return currentState.copy(earnListUM = null)
    }

    private fun handleErrorState(currentState: FeedListUM): FeedListUM {
        return currentState.copy(earnListUM = EarnListUM.Error(onRetryClick))
    }

    private fun handleDataState(
        currentState: FeedListUM,
        earnTokensWithCurrency: List<EarnTokenWithCurrency>,
    ): FeedListUM {
        return currentState.copy(
            earnListUM = EarnListUM.Content(
                items = earnTokensWithCurrency
                    .sortedWith(
                        compareByDescending<EarnTokenWithCurrency> {
                            it.earnToken.apy.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        }.thenBy { it.earnToken.tokenName },
                    )
                    .map { earnToken ->
                        earnTokenWithCurrencyConverter.convert(earnToken)
                    }
                    .toPersistentList(),
            ),
        )
    }

    private class EarnTokenWithCurrencyConverter(
        private val onItemClick: (EarnTokenWithCurrency) -> Unit,
    ) : Converter<EarnTokenWithCurrency, EarnListItemUM> {
        override fun convert(value: EarnTokenWithCurrency): EarnListItemUM {
            return EarnListItemUM(
                network = TextReference.Str(value.networkName),
                symbol = TextReference.Str(value.earnToken.tokenSymbol),
                tokenName = TextReference.Str(value.earnToken.tokenName),
                currencyIconState = CryptoCurrencyToIconStateConverter().convert(value.cryptoCurrency),
                earnValue = when (value.earnToken.rewardType) {
                    EarnRewardType.APR -> TextReference.Res(
                        id = R.string.staking_apr_earn_badge,
                        formatArgs = wrappedList(convertPercent(value.earnToken.apy)),
                    )
                    EarnRewardType.APY -> TextReference.Res(
                        id = R.string.yield_module_earn_badge,
                        formatArgs = wrappedList(convertPercent(value.earnToken.apy)),
                    )
                },
                earnType = when (value.earnToken.type) {
                    EarnType.STAKING -> TextReference.Res(R.string.common_staking)
                    EarnType.YIELD -> TextReference.Res(R.string.common_yield_mode)
                },
                onItemClick = { onItemClick(value) },
            )
        }

        private fun convertPercent(value: String): TextReference {
            val percent = BigDecimal(value).format { percent(withPercentSign = false) }
            return TextReference.Str(percent)
        }
    }
}