package com.tangem.features.swap.v2.impl.amount.model.converter

import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.domain.swap.models.SwapQuoteModel
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM.Content.DifferencePercent
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class SwapQuoteUMConverter(
    private val swapDirection: SwapDirection,
    private val allowanceContract: String?,
    private val isApprovalNeeded: Boolean,
    private val primaryCurrency: CryptoCurrency,
    private val secondaryCurrency: CryptoCurrency,
) : Converter<SwapQuoteUMConverter.Data, SwapQuoteUM> {
    override fun convert(value: Data): SwapQuoteUM {
        val (quote, provider) = value

        return if (allowanceContract != null) {
            if (isApprovalNeeded) {
                SwapQuoteUM.Allowance(
                    provider = provider,
                    allowanceContract = allowanceContract,
                )
            } else {
                SwapQuoteUM.Content(
                    provider = provider,
                    quoteAmount = quote.toTokenAmount,
                    diffPercent = DifferencePercent.Empty,
                    quoteAmountValue = stringReference(
                        quote.toTokenAmount.toQuoteValue(),
                    ),
                )
            }
        } else {
            SwapQuoteUM.Content(
                provider = provider,
                quoteAmount = quote.toTokenAmount,
                diffPercent = DifferencePercent.Empty,
                quoteAmountValue = stringReference(
                    quote.toTokenAmount.toQuoteValue(),
                ),
            )
        }
    }

    private fun BigDecimal.toQuoteValue() = format {
        crypto(
            when (swapDirection) {
                SwapDirection.Direct -> secondaryCurrency
                SwapDirection.Reverse -> primaryCurrency
            },
        )
    }

    data class Data(
        val quote: SwapQuoteModel,
        val provider: ExpressProvider,
    )
}