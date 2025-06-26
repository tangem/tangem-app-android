package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.common.ui.amountScreen.converters.AmountCurrencyTransformer
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.model.SwapAmountQuoteUtils.updateAmount
import com.tangem.utils.transformer.Transformer

internal class SwapAmountChangeCurrencyTransformer(
    private val primaryCryptoCurrencyStatus: CryptoCurrencyStatus,
    private val secondaryCryptoCurrencyStatus: CryptoCurrencyStatus,
    private val isFiatSelected: Boolean,
) : Transformer<SwapAmountUM> {
    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        return prevState.updateAmount(
            onPrimaryAmount = {
                copy(
                    amountField = AmountCurrencyTransformer(
                        cryptoCurrencyStatus = primaryCryptoCurrencyStatus,
                        value = isFiatSelected,
                    ).transform(prevState.primaryAmount.amountField),
                )
            },
            onSecondaryAmount = {
                copy(
                    amountField = AmountCurrencyTransformer(
                        cryptoCurrencyStatus = secondaryCryptoCurrencyStatus,
                        value = isFiatSelected,
                    ).transform(prevState.secondaryAmount.amountField),
                )
            },
        )
    }
}
