package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.model.converter.SwapAmountUpdateSubtitleConverter
import com.tangem.utils.transformer.Transformer

internal class SwapAmountChangeAmountTypeTransformer(
    private val selectedAmountType: SwapAmountType,
    private val swapRateType: ExpressRateType,
    private val isBalanceHidden: Boolean,
) : Transformer<SwapAmountUM> {

    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        if (prevState !is SwapAmountUM.Content) return prevState

        val subtitleConverter = SwapAmountUpdateSubtitleConverter(
            selectedAmountType = selectedAmountType,
            isBalanceHidden = isBalanceHidden,
        )

        val newPrimaryAmount = (prevState.primaryAmount as? SwapAmountFieldUM.Content)?.let { field ->
            subtitleConverter.updateSubtitles(
                field = field,
                cryptoCurrencyStatus = prevState.primaryCryptoCurrencyStatus,
                isAmountEmpty = true,
            )
        } ?: prevState.primaryAmount

        val newSecondaryAmount = if (prevState.secondaryCryptoCurrencyStatus != null) {
            (prevState.secondaryAmount as? SwapAmountFieldUM.Content)?.let { field ->
                subtitleConverter.updateSubtitles(
                    field = field,
                    cryptoCurrencyStatus = prevState.secondaryCryptoCurrencyStatus,
                    isAmountEmpty = true,
                )
            } ?: prevState.secondaryAmount
        } else {
            prevState.secondaryAmount
        }

        return prevState.copy(
            selectedAmountType = selectedAmountType,
            swapRateType = swapRateType,
            primaryAmount = newPrimaryAmount,
            secondaryAmount = newSecondaryAmount,
        )
    }
}