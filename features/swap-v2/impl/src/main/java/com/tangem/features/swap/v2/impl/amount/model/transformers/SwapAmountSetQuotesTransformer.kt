package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.express.models.ExpressError
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM.Content.DifferencePercent
import com.tangem.utils.StringsSigns
import com.tangem.utils.extensions.isPositive
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

internal class SwapAmountSetQuotesTransformer(
    private val quotes: List<SwapQuoteUM>,
    private val secondaryMaximumAmountBoundary: EnterAmountBoundary?,
    private val secondaryMinimumAmountBoundary: EnterAmountBoundary?,
    private val isSilentReload: Boolean,
) : Transformer<SwapAmountUM> {
    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        if (prevState !is SwapAmountUM.Content) return prevState

        val sortedQuotes = quotes.sortedWith(SwapQuotesComparator)
        val bestQuote = findBestQuote(quotes) ?: SwapQuoteUM.Empty
        val selectedQuote = if (isSilentReload) {
            prevState.selectedQuote
        } else {
            (bestQuote as? SwapQuoteUM.Content)?.copy(diffPercent = DifferencePercent.Best) ?: bestQuote
        }

        val selectQuoteTransformer = SwapAmountSelectQuoteTransformer(
            quoteUM = selectedQuote,
            secondaryMaximumAmountBoundary = secondaryMaximumAmountBoundary,
            secondaryMinimumAmountBoundary = secondaryMinimumAmountBoundary,
        )

        val updatedState = selectQuoteTransformer.transform(prevState = prevState)
        if (updatedState !is SwapAmountUM.Content) return prevState

        return updatedState.copy(
            isPrimaryButtonEnabled = updatedState.isPrimaryButtonEnabled && quotes.isNotEmpty(),
            swapQuotes = getQuotesWithDiff(sortedQuotes, bestQuote),
        )
    }

    private fun getQuotesWithDiff(sortedQuotes: List<SwapQuoteUM>, bestQuote: SwapQuoteUM): ImmutableList<SwapQuoteUM> {
        return sortedQuotes.sortedWith(SwapQuotesComparator)
            .map { quote ->
                if (quote is SwapQuoteUM.Content && bestQuote is SwapQuoteUM.Content) {
                    if (quote.provider.providerId == bestQuote.provider.providerId) {
                        quote.copy(diffPercent = DifferencePercent.Best)
                    } else {
                        // current / selected - 1
                        val percent = quote.quoteAmount / bestQuote.quoteAmount - BigDecimal.ONE
                        quote.copy(
                            diffPercent = DifferencePercent.Diff(
                                isPositive = percent.isPositive(),
                                percent = stringReference(
                                    if (percent.isPositive()) {
                                        "${StringsSigns.PLUS}${percent.format { percent() }}"
                                    } else {
                                        "${StringsSigns.DASH_SIGN}${percent.format { percent() }}"
                                    },
                                ),
                            ),
                        )
                    }
                } else {
                    quote
                }
            }.toPersistentList()
    }

    private fun findBestQuote(quotes: List<SwapQuoteUM>): SwapQuoteUM? {
        return quotes
            .sortedWith(SwapQuotesComparator)
            .firstOrNull()
    }

    private object SwapQuotesComparator : Comparator<SwapQuoteUM> {
        override fun compare(p0: SwapQuoteUM?, p1: SwapQuoteUM?): Int {
            return when {
                p0 is SwapQuoteUM.Content && p1 !is SwapQuoteUM.Content -> -1
                p0 !is SwapQuoteUM.Content && p1 is SwapQuoteUM.Content -> 1
                p0 is SwapQuoteUM.Error && p1 is SwapQuoteUM.Error -> compareErrorQuote(p0 = p0, p1 = p1)
                p0 is SwapQuoteUM.Content && p1 is SwapQuoteUM.Content -> p1.quoteAmount.compareTo(p0.quoteAmount)
                else -> 0
            }
        }

        private fun compareErrorQuote(p0: SwapQuoteUM.Error, p1: SwapQuoteUM.Error): Int = when {
            p0.expressError is ExpressError.AmountError && p1.expressError !is ExpressError.AmountError -> -1
            p0.expressError !is ExpressError.AmountError && p1.expressError is ExpressError.AmountError -> 1
            p0.expressError is ExpressError.AmountError && p1.expressError is ExpressError.AmountError -> {
                p0.expressError.amount.compareTo(p1.expressError.amount)
            }
            else -> 0
        }
    }
}