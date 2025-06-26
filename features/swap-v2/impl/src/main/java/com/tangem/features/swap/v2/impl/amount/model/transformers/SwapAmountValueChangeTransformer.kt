package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.common.ui.amountScreen.converters.field.AmountFieldChangeTransformer
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.model.SwapAmountQuoteUtils.updateAmount
import com.tangem.utils.transformer.Transformer

@Suppress("LongParameterList")
internal class SwapAmountValueChangeTransformer(
    private val primaryCryptoCurrencyStatus: CryptoCurrencyStatus,
    private val secondaryCryptoCurrencyStatus: CryptoCurrencyStatus,
    private val primaryMaximumAmountBoundary: EnterAmountBoundary,
    private val secondaryMaximumAmountBoundary: EnterAmountBoundary,
    private val primaryMinimumAmountBoundary: EnterAmountBoundary,
    private val secondaryMinimumAmountBoundary: EnterAmountBoundary,
    private val value: String,
) : Transformer<SwapAmountUM> {
    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        if (prevState !is SwapAmountUM.Content) return prevState

        return prevState
            .updateAmount(
                onPrimaryAmount = {
                    copy(
                        amountField = AmountFieldChangeTransformer(
                            cryptoCurrencyStatus = primaryCryptoCurrencyStatus,
                            maxEnterAmount = primaryMaximumAmountBoundary,
                            minimumTransactionAmount = primaryMinimumAmountBoundary,
                            value = value,
                        ).transform(prevState.primaryAmount.amountField),
                    )
                },
                onSecondaryAmount = {
                    copy(
                        amountField = AmountFieldChangeTransformer(
                            cryptoCurrencyStatus = secondaryCryptoCurrencyStatus,
                            maxEnterAmount = secondaryMaximumAmountBoundary,
                            minimumTransactionAmount = secondaryMinimumAmountBoundary,
                            value = value,
                        ).transform(prevState.secondaryAmount.amountField),
                    )
                },
            )
    }
}
