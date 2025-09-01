package com.tangem.features.swap.v2.impl.common

import com.tangem.domain.express.models.ExpressProvider

private val FCA_RESTRICTED_PROVIDER_IDS = setOf(
    "changelly",
    "changenow",
    "okx-cross-chain",
    "okx-on-chain",
    "simpleswap",
)

fun ExpressProvider.isRestrictedByFCA() = FCA_RESTRICTED_PROVIDER_IDS.contains(providerId)