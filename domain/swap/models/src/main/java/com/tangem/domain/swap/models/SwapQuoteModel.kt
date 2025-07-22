package com.tangem.domain.swap.models

import com.tangem.domain.express.models.ExpressProvider
import java.math.BigDecimal

/**
 * Quote model holds data about current amounts of swaps and fees
 *
 * @property provider swap provider
 * @property toTokenAmount amount of token you want to receive
 * @property allowanceContract whether swap occurs via third token
 */
data class SwapQuoteModel(
    val provider: ExpressProvider,
    val toTokenAmount: BigDecimal,
    val allowanceContract: String?,
)