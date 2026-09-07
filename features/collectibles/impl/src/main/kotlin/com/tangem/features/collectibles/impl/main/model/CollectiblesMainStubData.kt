package com.tangem.features.collectibles.impl.main.model

import java.math.BigDecimal

/** Account balance as the header shows it. */
internal data class CollectiblesBalance(
    val fiat: BigDecimal,
    val crypto: String,
)

/**
 * Stand-in for the catalogue the backend will serve.
 *
 * Image URLs are empty on purpose: the cards render their placeholder until there is a service to ask.
 */
internal object CollectiblesMainStubData {

    val BALANCE = CollectiblesBalance(fiat = BigDecimal("2700.52"), crypto = "2,500.52 USDC")
}