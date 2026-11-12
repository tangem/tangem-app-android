package com.tangem.domain.yield.supply.models

/** What the Yield Module needs before it can take a native deposit. */
enum class YieldModulePreparation {
    Deploy,
    InitToken,
    ReactivateToken,
}