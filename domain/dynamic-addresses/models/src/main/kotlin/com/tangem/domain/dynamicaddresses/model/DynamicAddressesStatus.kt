package com.tangem.domain.dynamicaddresses.model

import java.math.BigDecimal

enum class DynamicAddressesStatus {
    ENABLED,

    DISABLED,

    /** Enabled on backend, but local XPUB setup required (cross-device sync) */
    ENABLED_REQUIRES_SETUP,
}

data class UsedAddress(
    val address: String,
    val path: String,
    val balance: BigDecimal,
)