package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.common.ui.amountScreen.converters.field.AmountFieldChangeTransformer
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountType
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.model.SwapAmountQuoteUtils.calculatePriceImpact
import com.tangem.features.swap.v2.impl.amount.model.converter.SwapAmountErrorConverter
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.features.swap.v2.impl.common.isRestrictedByFCA
import com.tangem.utils.extensions.orZero
import com.tangem.utils.transformer.Transformer

internal class SwapAmountSelectQuoteTransformer(
    private val quoteUM: SwapQuoteUM,
    private val secondaryMaximumAmountBoundary: EnterAmountBoundary?,
    private val secondaryMinimumAmountBoundary: EnterAmountBoundary?,
    private val needApplyFCARestrictions: Boolean,
) : Transformer<SwapAmountUM> {
    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        if (prevState !is SwapAmountUM.Content) return prevState

        val providerErrorConverter = SwapAmountErrorConverter(
            cryptoCurrency = prevState.primaryCryptoCurrencyStatus.currency,
        )

        return prevState.copy(
            isPrimaryButtonEnabled = quoteUM is SwapQuoteUM.Content,
            selectedQuote = quoteUM,
            showFCAWarning = needApplyFCARestrictions && quoteUM.provider?.isRestrictedByFCA() == true,
            primaryAmount = if (prevState.selectedAmountType == SwapAmountType.From) {
                val swapAmountField = prevState.primaryAmount as? SwapAmountFieldUM.Content
                val amountField = swapAmountField?.amountField as? AmountState.Data

                val amountError = (quoteUM as? SwapQuoteUM.Error)?.expressError?.let(providerErrorConverter::convert)

                swapAmountField?.copy(
                    amountField = amountField?.copy(
                        amountTextField = amountField.amountTextField.copy(
                            error = amountError ?: TextReference.EMPTY,
                            isError = amountError != null,
                        ),
                    ) ?: swapAmountField.amountField,
                ) ?: prevState.primaryAmount
            } else {
                prevState.primaryAmount
            },
            secondaryAmount = if (prevState.selectedAmountType == SwapAmountType.From &&
                prevState.secondaryCryptoCurrencyStatus != null && secondaryMaximumAmountBoundary != null
            ) {
                val secondaryAmountField = prevState.secondaryAmount as? SwapAmountFieldUM.Content
                val fromAmount = (prevState.primaryAmount.amountField as? AmountState.Data)
                    ?.amountTextField?.cryptoAmount?.value.orZero()
                val toAmount = (quoteUM as? SwapQuoteUM.Content)?.quoteAmount
                val priceImpact = calculatePriceImpact(
                    swapDirection = prevState.swapDirection,
                    fromTokenAmount = fromAmount,
                    toTokenAmount = toAmount.orZero(),
                    primaryCryptoCurrencyStatus = prevState.primaryCryptoCurrencyStatus,
                    secondaryCryptoCurrencyStatus = prevState.secondaryCryptoCurrencyStatus,
                )

                secondaryAmountField?.copy(
                    priceImpact = priceImpact,
                    amountField = AmountFieldChangeTransformer(
                        cryptoCurrencyStatus = prevState.secondaryCryptoCurrencyStatus,
                        maxEnterAmount = secondaryMaximumAmountBoundary,
                        minimumTransactionAmount = secondaryMinimumAmountBoundary,
                        value = toAmount?.parseBigDecimal(prevState.secondaryCryptoCurrencyStatus.currency.decimals)
                            .orEmpty(),
                    ).transform(secondaryAmountField.amountField),
                ) ?: prevState.secondaryAmount
            } else {
                prevState.secondaryAmount
            },
        )
    }
}