package com.tangem.features.tangempay.cashback.impl.model

/** Cashback program tier, mapped once from the domain and shared by the rate tile and the details sheet. */
internal data class CashbackTier(
    val tierId: String,
    val rate: Int?,
    val label: String,
    val scope: String,
    val minPurchase: String?,
    val monthlyCap: String?,
)