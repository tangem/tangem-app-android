package com.tangem.features.swap.v2.api.subcomponents

/**
 * Trigger swap amount change from another component.
 * Different from another triggers because it takes raw string instead of BigDecimal
 */
interface SwapAmountUpdateTrigger {
    suspend fun triggerUpdateAmount(amountValue: String, isEnterInFiatSelected: Boolean)

    suspend fun triggerQuoteReload()
}