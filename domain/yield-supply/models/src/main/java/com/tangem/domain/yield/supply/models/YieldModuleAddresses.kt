package com.tangem.domain.yield.supply.models

/**
 * The only contracts a native transaction may be sent to.
 *
 * @property moduleAddress the user's Yield Module; its calculated address until it is deployed
 */
data class YieldModuleAddresses(
    val moduleAddress: String,
    val factoryAddress: String,
)