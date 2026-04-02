package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM.Content.DifferencePercent
import com.tangem.features.swap.v2.impl.common.isRestrictedByFCA
import com.tangem.utils.StringsSigns
import com.tangem.utils.extensions.isPositive
import com.tangem.utils.extensions.isSingleItem
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

@Suppress("LongParameterList")
internal class SwapAmountSetQuotesTransformer(
    private val quotes: List<SwapQuoteUM>,
    private val secondaryMaximumAmountBoundary: EnterAmountBoundary?,
    private val secondaryMinimumAmountBoundary: EnterAmountBoundary?,
    private val isSilentReload: Boolean,
    private val isNeedApplyFcaRestrictions: Boolean,
    private val isBalanceHidden: Boolean,
    private val primaryMaximumAmountBoundary: EnterAmountBoundary? = null,
    private val primaryMinimumAmountBoundary: EnterAmountBoundary? = null,
    private val primaryFiatRateUSD: BigDecimal?,
    private val secondaryFiatRateUSD: BigDecimal?,
) : Transformer<SwapAmountUM> {

    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        if (prevState !is SwapAmountUM.Content) return prevState

        val selectedAmountType = prevState.selectedAmountType
        val comparator = SwapQuotesComparator(selectedAmountType)
        val isSingleProvider = isSingleProvider(quotes)
        val sortedQuotes = quotes.sortedWith(comparator)
        val bestQuote = findBestQuote(quotes, comparator) ?: SwapQuoteUM.Empty

        val quotesWithDiff = getQuotesWithDiff(
            sortedQuotes = sortedQuotes,
            bestQuote = bestQuote,
            isSingleProvider = isSingleProvider,
            selectedAmountType = selectedAmountType,
        )
        val selectedQuote = resolveSelectedQuote(
            prevState = prevState,
            quotesWithDiff = quotesWithDiff,
            bestQuote = bestQuote,
            isSingleProvider = isSingleProvider,
        )

        val updatedState = SwapAmountSelectQuoteTransformer(
            quoteUM = selectedQuote,
            primaryFiatRateUSD = primaryFiatRateUSD,
            secondaryFiatRateUSD = secondaryFiatRateUSD,
            secondaryMaximumAmountBoundary = secondaryMaximumAmountBoundary,
            secondaryMinimumAmountBoundary = secondaryMinimumAmountBoundary,
            isNeedApplyFCARestrictions = isNeedApplyFcaRestrictions &&
                selectedQuote.provider?.isRestrictedByFCA() == true,
            isBalanceHidden = isBalanceHidden,
            primaryMaximumAmountBoundary = primaryMaximumAmountBoundary,
            primaryMinimumAmountBoundary = primaryMinimumAmountBoundary,
        ).transform(prevState = prevState)
        if (updatedState !is SwapAmountUM.Content) return prevState

        val stateWithError = SwapAmountErrorQuoteTransformer(quotes).transform(updatedState)
        if (stateWithError !is SwapAmountUM.Content) return prevState

        return stateWithError.copy(
            isPrimaryButtonEnabled = stateWithError.isPrimaryButtonEnabled && quotesWithDiff.isNotEmpty(),
            swapQuotes = quotesWithDiff,
        )
    }

    private fun resolveSelectedQuote(
        prevState: SwapAmountUM.Content,
        quotesWithDiff: ImmutableList<SwapQuoteUM>,
        bestQuote: SwapQuoteUM,
        isSingleProvider: Boolean,
    ): SwapQuoteUM {
        if (isSilentReload && prevState.selectedQuote !is SwapQuoteUM.Loading) {
            return quotesWithDiff
                .firstOrNull { it.provider?.providerId == prevState.selectedQuote.provider?.providerId }
                ?: prevState.selectedQuote
        }
        return (bestQuote as? SwapQuoteUM.Content)?.copy(
            diffPercent = DifferencePercent.Best,
            isSingleProvider = isSingleProvider,
        ) ?: bestQuote
    }

    private fun isSingleProvider(quotes: List<SwapQuoteUM>): Boolean {
        return quotes.filter { swapQuoteUM ->
            swapQuoteUM is SwapQuoteUM.Content || swapQuoteUM is SwapQuoteUM.Allowance ||
                (swapQuoteUM as? SwapQuoteUM.Error)?.expressError is ExpressError.AmountError
        }.isSingleItem()
    }

    private fun getQuotesWithDiff(
        sortedQuotes: List<SwapQuoteUM>,
        bestQuote: SwapQuoteUM,
        isSingleProvider: Boolean,
        selectedAmountType: SwapAmountType,
    ): ImmutableList<SwapQuoteUM> {
        return sortedQuotes.map { quote ->
            if (quote is SwapQuoteUM.Content && bestQuote is SwapQuoteUM.Content) {
                if (quote.provider.providerId == bestQuote.provider.providerId) {
                    quote.copy(
                        diffPercent = DifferencePercent.Best,
                        isSingleProvider = isSingleProvider,
                    )
                } else {
                    val percent = if (selectedAmountType == SwapAmountType.To) {
                        // Fixed mode: best has lowest fromTokenAmount; compare as (best/current - 1)
                        val bestFrom = bestQuote.fromAmount
                        val quoteFrom = quote.fromAmount
                        if (bestFrom != null && quoteFrom != null && quoteFrom > BigDecimal.ZERO) {
                            bestFrom / quoteFrom - BigDecimal.ONE
                        } else {
                            BigDecimal.ZERO
                        }
                    } else {
                        // Float mode: best has highest toAmount; compare as (current/best - 1)
                        quote.toAmount / bestQuote.toAmount - BigDecimal.ONE
                    }
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

    private fun findBestQuote(quotes: List<SwapQuoteUM>, comparator: Comparator<SwapQuoteUM>): SwapQuoteUM? {
        return quotes.sortedWith(comparator).firstOrNull()
    }

    private class SwapQuotesComparator(private val selectedAmountType: SwapAmountType) : Comparator<SwapQuoteUM> {
        override fun compare(p0: SwapQuoteUM?, p1: SwapQuoteUM?): Int {
            return when {
                p0 is SwapQuoteUM.Content && p1 !is SwapQuoteUM.Content -> -1
                p0 !is SwapQuoteUM.Content && p1 is SwapQuoteUM.Content -> 1
                p0 is SwapQuoteUM.Error && p1 is SwapQuoteUM.Error -> compareErrorQuote(p0 = p0, p1 = p1)
                p0 is SwapQuoteUM.Content && p1 is SwapQuoteUM.Content -> compareContent(p0, p1)
                else -> 0
            }
        }

        private fun compareContent(p0: SwapQuoteUM.Content, p1: SwapQuoteUM.Content): Int {
            return if (selectedAmountType == SwapAmountType.To) {
                // Fixed mode: lower fromTokenAmount = better (ascending)
                val f0 = p0.fromAmount ?: BigDecimal.ZERO
                val f1 = p1.fromAmount ?: BigDecimal.ZERO
                f0.compareTo(f1)
            } else {
                // Float mode: higher toAmount = better (descending)
                p1.toAmount.compareTo(p0.toAmount)
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