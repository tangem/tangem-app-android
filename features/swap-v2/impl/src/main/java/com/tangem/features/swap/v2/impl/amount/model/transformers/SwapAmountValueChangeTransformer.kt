package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.common.ui.amountScreen.converters.field.AmountFieldChangeTransformer
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.model.SwapAmountQuoteUtils.updateAmount
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.utils.transformer.Transformer

internal class SwapAmountValueChangeTransformer(
    private val primaryMaximumAmountBoundary: EnterAmountBoundary,
    private val primaryMinimumAmountBoundary: EnterAmountBoundary,
    private val secondaryMaximumAmountBoundary: EnterAmountBoundary?,
    private val secondaryMinimumAmountBoundary: EnterAmountBoundary?,
    private val value: String,
) : Transformer<SwapAmountUM> {
    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        if (prevState !is SwapAmountUM.Content) return prevState

        val updatedState = prevState.updateAmount(
            onPrimaryAmount = { primaryStatus ->
                copy(
                    amountField = AmountFieldChangeTransformer(
                        cryptoCurrencyStatus = primaryStatus,
                        maxEnterAmount = primaryMaximumAmountBoundary,
                        minimumTransactionAmount = primaryMinimumAmountBoundary,
                        value = value,
                    ).transform(prevState.primaryAmount.amountField),
                )
            },
            onSecondaryAmount = { secondaryStatus ->
                if (secondaryMaximumAmountBoundary != null) {
                    copy(
                        amountField = AmountFieldChangeTransformer(
                            cryptoCurrencyStatus = secondaryStatus,
                            maxEnterAmount = secondaryMaximumAmountBoundary,
                            minimumTransactionAmount = secondaryMinimumAmountBoundary,
                            value = value,
                        ).transform(prevState.secondaryAmount.amountField),
                    )
                } else {
                    this
                }
            },
        )

        val contentState = updatedState as? SwapAmountUM.Content ?: return updatedState

        val newState = if (value.isEmpty()) {
            contentState.clearOppositeField(prevState)
        } else {
            contentState
        }

        return newState.copy(
            isPrimaryButtonEnabled = false,
            selectedQuote = if (value.isEmpty()) {
                SwapQuoteUM.Empty
            } else {
                SwapQuoteUM.Loading
            },
        )
    }

    private fun SwapAmountUM.Content.clearOppositeField(prevState: SwapAmountUM.Content): SwapAmountUM.Content {
        val isPrimaryFieldEdited =
            selectedAmountType == SwapAmountType.From && swapDirection == SwapDirection.Direct

        return if (isPrimaryFieldEdited) {
            val secondaryStatus = secondaryCryptoCurrencyStatus ?: return this
            val secondaryContent = secondaryAmount as? SwapAmountFieldUM.Content ?: return this
            copy(
                secondaryAmount = secondaryContent.copy(
                    amountField = AmountFieldChangeTransformer(
                        cryptoCurrencyStatus = secondaryStatus,
                        maxEnterAmount = secondaryMaximumAmountBoundary ?: return this,
                        minimumTransactionAmount = secondaryMinimumAmountBoundary,
                        value = "",
                    ).transform(prevState.secondaryAmount.amountField),
                ),
            )
        } else {
            val primaryContent = primaryAmount as? SwapAmountFieldUM.Content ?: return this
            copy(
                primaryAmount = primaryContent.copy(
                    amountField = AmountFieldChangeTransformer(
                        cryptoCurrencyStatus = primaryCryptoCurrencyStatus,
                        maxEnterAmount = primaryMaximumAmountBoundary,
                        minimumTransactionAmount = primaryMinimumAmountBoundary,
                        value = "",
                    ).transform(prevState.primaryAmount.amountField),
                ),
            )
        }
    }
}