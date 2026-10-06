package com.tangem.feature.swap.domain.models.domain

import com.tangem.feature.swap.domain.models.SwapAmount
import java.math.BigDecimal

/**
 * Quote model holds data about current amounts of exchange and fees
 *
 * @property toTokenAmount amount of token you want to receive
 * @property allowanceContract spender address for ERC-20 allowance, null when not applicable
 * @property txType expected execution flow returned by the express provider on the quote;
 *     null for legacy responses that don't yet carry this field
 * @property isRestricted token is region-restricted for this provider — quote is displayable but not executable
 * @property slippage slippage percent the provider applied to this quote, null when the response has none
 */
data class QuoteModel(
    val toTokenAmount: SwapAmount,
    val allowanceContract: String?,
    val txType: ExpressTxType?,
    val isRestricted: Boolean = false,
    val slippage: BigDecimal? = null,
)