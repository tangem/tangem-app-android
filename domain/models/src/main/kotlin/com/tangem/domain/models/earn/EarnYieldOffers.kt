package com.tangem.domain.models.earn

/** [best] is the first row of the Earn list: the backend sorts it by APY, descending. */
data class EarnYieldOffers(
    val total: Int,
    val best: EarnToken?,
)