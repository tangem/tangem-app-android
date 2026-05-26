package com.tangem.features.yield.supply.impl.promo.entity

import com.tangem.core.ui.extensions.TextReference

data class YieldSupplyPromoUM(
    val tosLink: String,
    val policyLink: String,
    val boostTermsLink: String,
    val title: TextReference,
    val subtitle: TextReference,
    val tokenSymbol: String,
    val isBoostAvailable: Boolean = false,
    val baseApy: String? = null,
    val boostedApy: String? = null,
)