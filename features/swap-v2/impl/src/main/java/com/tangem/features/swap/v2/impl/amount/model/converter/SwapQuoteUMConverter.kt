package com.tangem.features.swap.v2.impl.amount.model.converter

import androidx.compose.ui.text.buildAnnotatedString
import com.tangem.core.ui.extensions.annotatedReference
import com.tangem.core.ui.extensions.appendSpace
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.anyDecimals
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.domain.swap.models.SwapQuoteModel
import com.tangem.features.swap.v2.impl.amount.model.SwapAmountQuoteUtils.calculateRate
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM.Content.DifferencePercent
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class SwapQuoteUMConverter(
    private val swapDirection: SwapDirection,
    private val allowanceContract: String?,
    private val isApprovalNeeded: Boolean,
    private val primaryCurrency: CryptoCurrency,
    private val secondaryCurrency: CryptoCurrency,
    private val fromAmount: BigDecimal,
) : Converter<SwapQuoteUMConverter.Data, SwapQuoteUM> {
    override fun convert(value: Data): SwapQuoteUM {
        val (quote, provider) = value

        val rate = calculateRate(
            fromAmount = fromAmount,
            toAmount = quote.toTokenAmount,
            toAmountDecimals = secondaryCurrency.decimals,
        )
        val rateString = buildAnnotatedString {
            append(BigDecimal.ONE.format { crypto(symbol = primaryCurrency.symbol, decimals = 0).anyDecimals() })
            appendSpace()
            append(StringsSigns.APPROXIMATE)
            appendSpace()
            append(rate.format { crypto(secondaryCurrency) })
        }

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
                    rate = annotatedReference(rateString),
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
                rate = annotatedReference(rateString),
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