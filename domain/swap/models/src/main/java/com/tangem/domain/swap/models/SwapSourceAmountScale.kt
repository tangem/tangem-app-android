package com.tangem.domain.swap.models

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Express quotes and builds a swap in the source token's on-chain units, while the app shows and takes amounts
 * already scaled by the mint (Solana Token-2022 `scaledUiAmountConfig`, e.g. SPACEX). A displayed amount sent as is
 * makes the provider's transaction spend [multiplier] times more than the user entered.
 *
 * @property multiplier factor between the displayed and the on-chain amount, `null` when they coincide
 */
data class SwapSourceAmountScale(val multiplier: BigDecimal?) {

    /** Rounds down, so the swap never spends more than entered: the API only accepts whole on-chain units. */
    fun toOnChain(displayedAmount: BigDecimal, decimals: Int): BigDecimal {
        if (multiplier == null) return displayedAmount
        return displayedAmount.divide(multiplier, decimals, RoundingMode.DOWN)
    }

    fun toDisplayed(onChainAmount: BigDecimal): BigDecimal {
        if (multiplier == null) return onChainAmount
        return onChainAmount.multiply(multiplier)
    }
}