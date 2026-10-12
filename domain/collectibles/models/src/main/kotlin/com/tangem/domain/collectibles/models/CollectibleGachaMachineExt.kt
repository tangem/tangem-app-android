package com.tangem.domain.collectibles.models

import java.math.BigDecimal

/** What one spin costs: the backend sends the price and the fee, not their sum. */
val CollectibleGachaMachine.total: BigDecimal
    get() = price + feeAmount