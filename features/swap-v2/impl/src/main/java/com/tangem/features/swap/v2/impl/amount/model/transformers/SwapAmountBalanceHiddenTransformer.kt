package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.model.converter.SwapAmountUpdateSubtitleConverter
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.utils.transformer.Transformer

internal class SwapAmountBalanceHiddenTransformer(
    private val isBalanceHidden: Boolean,
) : Transformer<SwapAmountUM> {

    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        val content = prevState as? SwapAmountUM.Content ?: return prevState

        val quoteContent = content.selectedQuote as? SwapQuoteUM.Content

        val subtitleConverter = SwapAmountUpdateSubtitleConverter(
            selectedAmountType = content.selectedAmountType,
            isBalanceHidden = isBalanceHidden,
        )

        val updatedPrimary = (content.primaryAmount as? SwapAmountFieldUM.Content)?.let { primary ->
            val isAmountEmpty = (primary.amountField as? AmountState.Data)
                ?.amountTextField?.cryptoAmount?.value == null
            subtitleConverter.updateSubtitles(
                field = primary,
                cryptoCurrencyStatus = content.primaryCryptoCurrencyStatus,
                isAmountEmpty = isAmountEmpty,
                displayAmount = quoteContent?.fromAmount,
            )
        } ?: content.primaryAmount

        val updatedSecondary = if (content.secondaryCryptoCurrencyStatus != null) {
            (content.secondaryAmount as? SwapAmountFieldUM.Content)?.let { secondary ->
                val isAmountEmpty = (secondary.amountField as? AmountState.Data)
                    ?.amountTextField?.cryptoAmount?.value == null
                subtitleConverter.updateSubtitles(
                    field = secondary,
                    cryptoCurrencyStatus = content.secondaryCryptoCurrencyStatus,
                    isAmountEmpty = isAmountEmpty,
                    displayAmount = quoteContent?.toAmount,
                )
            } ?: content.secondaryAmount
        } else {
            content.secondaryAmount
        }

        return content.copy(
            primaryAmount = updatedPrimary,
            secondaryAmount = updatedSecondary,
        )
    }
}