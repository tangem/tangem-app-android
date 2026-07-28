package com.tangem.features.tangempay.cashback.impl.model

import java.util.Locale

internal object CashbackRates {

    private val byTier = mapOf(
        "basic" to 1,
        "plus" to 2,
    )

    fun forTier(tier: String): Int? = byTier[tier.lowercase(Locale.ROOT)]
}