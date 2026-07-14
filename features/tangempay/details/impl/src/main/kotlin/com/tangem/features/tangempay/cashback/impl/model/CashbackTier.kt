package com.tangem.features.tangempay.cashback.impl.model

import com.tangem.domain.models.account.TangemPayTariffPlan

/** Cashback program tier, mapped once from the domain and shared by the rate tile and the details sheet. */
internal data class CashbackTier(
    val planType: TangemPayTariffPlan.Type,
    val rate: Int?,
    val label: String,
    val scope: String,
    val minPurchase: String?,
    val monthlyCap: String?,
)