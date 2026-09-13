package com.tangem.features.tangempay.cashback.impl.model

import java.math.BigDecimal

internal data class CashbackCard(
    val cardType: String,
    val title: String?,
    val rate: BigDecimal,
    val minPurchase: String?,
)