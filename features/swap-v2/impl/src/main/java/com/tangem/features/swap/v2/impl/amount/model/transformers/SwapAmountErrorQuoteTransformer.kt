package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.swap.v2.impl.R
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.model.converter.SwapAmountErrorConverter
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.utils.transformer.Transformer

/**
 * Sets a generic "Something went wrong" error on the selected amount field
 * when all quotes returned errors that cannot be converted to a specific amount error.
 * Returns [prevState] unchanged when the condition is not met.
 */
internal class SwapAmountErrorQuoteTransformer(
    private val quotes: List<SwapQuoteUM>,
) : Transformer<SwapAmountUM> {

    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        if (prevState !is SwapAmountUM.Content) return prevState
        if (!areAllQuotesUnrecoverableErrors(prevState)) return prevState

        val error = resourceReference(R.string.send_with_swap_something_went_wrong)

        val isPrimarySelected = prevState.selectedAmountType == SwapAmountType.From

        val newPrimaryAmount = if (isPrimarySelected) {
            applyError(prevState.primaryAmount, error)
        } else {
            prevState.primaryAmount
        }

        val newSecondaryAmount = if (!isPrimarySelected) {
            applyError(prevState.secondaryAmount, error)
        } else {
            prevState.secondaryAmount
        }

        return prevState.copy(
            isPrimaryButtonEnabled = false,
            primaryAmount = newPrimaryAmount,
            secondaryAmount = newSecondaryAmount,
        )
    }

    private fun areAllQuotesUnrecoverableErrors(state: SwapAmountUM.Content): Boolean {
        if (!quotes.all { it is SwapQuoteUM.Error }) return false

        val secondaryProviderErrorConverter = state.secondaryCryptoCurrencyStatus?.let {
            SwapAmountErrorConverter(cryptoCurrency = it.currency)
        }
        return quotes.all { quote ->
            (quote as? SwapQuoteUM.Error)?.expressError
                ?.let { secondaryProviderErrorConverter?.convert(it) } == null
        }
    }

    private fun applyError(
        field: SwapAmountFieldUM,
        error: com.tangem.core.ui.extensions.TextReference,
    ): SwapAmountFieldUM {
        val content = field as? SwapAmountFieldUM.Content ?: return field
        val amountData = content.amountField as? AmountState.Data ?: return field
        return content.copy(
            amountField = amountData.copy(
                amountTextField = amountData.amountTextField.copy(
                    error = error,
                    isError = true,
                ),
            ),
        )
    }
}