package com.tangem.features.swap.v2.impl.common.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.express.models.ExpressProvider
import java.math.BigDecimal

@Immutable
internal sealed class SwapQuoteUM {

    abstract val provider: ExpressProvider?

    data object Empty : SwapQuoteUM() {
        override val provider = null
    }

    data object Loading : SwapQuoteUM() {
        override val provider = null
    }

    data class Error(
        override val provider: ExpressProvider,
        val expressError: ExpressError,
    ) : SwapQuoteUM()

    data class Allowance(
        override val provider: ExpressProvider,
        val allowanceContract: String,
    ) : SwapQuoteUM()

    data class Content(
        override val provider: ExpressProvider,
        val quoteAmount: BigDecimal,
        val quoteAmountValue: TextReference,
        val diffPercent: DifferencePercent,
        val rate: TextReference,
    ) : SwapQuoteUM() {
        sealed class DifferencePercent {
            data object Empty : DifferencePercent()
            data object Best : DifferencePercent()
            data class Diff(
                val percent: TextReference,
            ) : DifferencePercent()
        }
    }
}