package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.common.ui.amountScreen.converters.field.AmountFieldChangeTransformer
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.swap.v2.impl.R
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountType
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.model.SwapAmountQuoteUtils.calculatePriceImpact
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM.Content.DifferencePercent
import com.tangem.utils.StringsSigns
import com.tangem.utils.extensions.isPositive
import com.tangem.utils.extensions.orZero
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

internal class SwapAmountSetQuotesTransformer(
    private val quotes: List<SwapQuoteUM>,
    private val primaryCryptoCurrencyStatus: CryptoCurrencyStatus,
    private val secondaryCryptoCurrencyStatus: CryptoCurrencyStatus,
    private val secondaryMaximumAmountBoundary: EnterAmountBoundary,
    private val secondaryMinimumAmountBoundary: EnterAmountBoundary,
) : Transformer<SwapAmountUM> {
    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        if (prevState !is SwapAmountUM.Content) return prevState

        val fromAmount = when (prevState.swapDirection) {
            SwapDirection.Direct -> prevState.primaryAmount.amountField
            SwapDirection.Reverse -> prevState.secondaryAmount.amountField
        } as? AmountState.Data
        val fromAmountValue = fromAmount?.amountTextField?.cryptoAmount?.value.orZero()

        val sortedQuotes = quotes.sortedWith(SwapQuotesComparator)
        val bestQuote = findBestQuote(quotes) ?: SwapQuoteUM.Empty

        return prevState.copy(
            isPrimaryButtonEnabled = bestQuote is SwapQuoteUM.Content,
            swapQuotes = getQuotesWithDiff(sortedQuotes, bestQuote),
            selectedQuote = bestQuote,
            primaryAmount = if (
                prevState.selectedAmountType == SwapAmountType.From && prevState.swapDirection == SwapDirection.Direct
            ) {
                val swapAmountField = prevState.primaryAmount as? SwapAmountFieldUM.Content
                val amountField = swapAmountField?.amountField as? AmountState.Data

                val amountError = (bestQuote as? SwapQuoteUM.Error)?.expressError.getAmountError()

                if (amountField?.amountTextField?.isError == true) {
                    prevState.primaryAmount
                } else {
                    swapAmountField?.copy(
                        amountField = amountField?.copy(
                            amountTextField = amountField.amountTextField.copy(
                                error = amountError ?: TextReference.EMPTY,
                                isError = amountError != null,
                            ),
                        ) ?: swapAmountField.amountField,
                    ) ?: prevState.primaryAmount
                }
            } else {
                prevState.primaryAmount
            },
            secondaryAmount = if (
                prevState.selectedAmountType == SwapAmountType.From && prevState.swapDirection == SwapDirection.Direct
            ) {
                val amountField = prevState.secondaryAmount as? SwapAmountFieldUM.Content
                val toAmount = (bestQuote as? SwapQuoteUM.Content)?.quoteAmount

                val priceImpact = calculatePriceImpact(
                    swapDirection = prevState.swapDirection,
                    fromTokenAmount = fromAmountValue,
                    toTokenAmount = toAmount.orZero(),
                    primaryCryptoCurrencyStatus = primaryCryptoCurrencyStatus,
                    secondaryCryptoCurrencyStatus = secondaryCryptoCurrencyStatus,
                )

                amountField?.copy(
                    priceImpact = priceImpact,
                    amountField = AmountFieldChangeTransformer(
                        cryptoCurrencyStatus = secondaryCryptoCurrencyStatus,
                        maxEnterAmount = secondaryMaximumAmountBoundary,
                        minimumTransactionAmount = secondaryMinimumAmountBoundary,
                        value = toAmount?.parseBigDecimal(secondaryCryptoCurrencyStatus.currency.decimals).orEmpty(),
                    ).transform(amountField.amountField),
                ) ?: prevState.secondaryAmount
            } else {
                prevState.secondaryAmount
            },
        )
    }

    private fun ExpressError?.getAmountError(): TextReference? = when (this) {
        is ExpressError.AmountError.TooSmallError -> resourceReference(
            R.string.express_provider_min_amount,
            wrappedList(amount.format { crypto(cryptoCurrency = primaryCryptoCurrencyStatus.currency) }),
        )
        is ExpressError.AmountError.TooBigError -> resourceReference(
            R.string.express_provider_max_amount,
            wrappedList(amount.format { crypto(cryptoCurrency = primaryCryptoCurrencyStatus.currency) }),
        )
        else -> null
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