package com.tangem.domain.wallets.models

/**
 * Network and currency in which a promo campaign pays out its reward.
 *
 * @property networkId backend network id, e.g. `stellar`
 * @property currencySymbol currency ticker, e.g. `XLM`
 */
data class PromoPayout(
    val networkId: String,
    val currencySymbol: String,
)