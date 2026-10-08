package com.tangem.features.yield.supply.impl.promo.v2.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

/**
 * Contents of the incentivised promo screen.
 *
 * @property incentiveChip reward APR the running campaign pays on top of the base rate, absent without one
 */
@Immutable
internal data class YieldSupplyPromoV2UM(
    val title: TextReference,
    val rate: TextReference,
    val incentiveChip: TextReference?,
    val tokenSymbol: String,
    val tosLink: String,
    val policyLink: String,
)