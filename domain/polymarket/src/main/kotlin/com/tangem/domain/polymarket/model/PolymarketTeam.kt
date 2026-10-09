package com.tangem.domain.polymarket.model

/** A team or an individual player; [ordering] is the provider's side designation, e.g. `home`. */
data class PolymarketTeam(
    val name: String,
    val abbreviation: String?,
    val alias: String?,
    val logoUrl: String?,
    val ordering: String?,
)