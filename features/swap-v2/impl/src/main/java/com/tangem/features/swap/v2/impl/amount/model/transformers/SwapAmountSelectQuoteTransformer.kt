package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.common.ui.amountScreen.converters.field.AmountFieldChangeTransformer
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountType
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.model.SwapAmountQuoteUtils.calculatePriceImpact
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.utils.extensions.orZero
import com.tangem.utils.transformer.Transformer

internal class SwapAmountSelectQuoteTransformer(
    private val quoteUM: SwapQuoteUM,
    private val primaryCryptoCurrencyStatus: CryptoCurrencyStatus,
    private val secondaryCryptoCurrencyStatus: CryptoCurrencyStatus,
    private val secondaryMaximumAmountBoundary: EnterAmountBoundary,
    private val secondaryMinimumAmountBoundary: EnterAmountBoundary,
) : Transformer<SwapAmountUM> {
    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        if (prevState !is SwapAmountUM.Content) return prevState

        return prevState.copy(
            isPrimaryButtonEnabled = true,
            selectedQuote = quoteUM,
            secondaryAmount = if (
                prevState.selectedAmountType == SwapAmountType.From && prevState.swapDirection == SwapDirection.Direct
            ) {
                val secondaryAmountField = prevState.secondaryAmount as? SwapAmountFieldUM.Content
                val fromAmount = (prevState.primaryAmount.amountField as? AmountState.Data)
                    ?.amountTextField?.cryptoAmount?.value.orZero()
                val toAmount = (quoteUM as? SwapQuoteUM.Content)?.quoteAmount
                val priceImpact = calculatePriceImpact(
                    swapDirection = prevState.swapDirection,
                    fromTokenAmount = fromAmount,
                    toTokenAmount = toAmount.orZero(),
                    primaryCryptoCurrencyStatus = primaryCryptoCurrencyStatus,
                    secondaryCryptoCurrencyStatus = secondaryCryptoCurrencyStatus,
                )

                secondaryAmountField?.copy(
                    priceImpact = priceImpact,
                    amountField = AmountFieldChangeTransformer(
                        cryptoCurrencyStatus = secondaryCryptoCurrencyStatus,
                        maxEnterAmount = secondaryMaximumAmountBoundary,
                        minimumTransactionAmount = secondaryMinimumAmountBoundary,
                        value = toAmount?.parseBigDecimal(secondaryCryptoCurrencyStatus.currency.decimals)
                            .orEmpty(),
                    ).transform(secondaryAmountField.amountField),
                ) ?: prevState.secondaryAmount
            } else {
                prevState.secondaryAmount
            },
        )
    }
}
