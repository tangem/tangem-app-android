package com.tangem.domain.staking.model.ethpool

import com.tangem.domain.models.serialization.SerializedBigDecimal
import org.joda.time.DateTime

/**
 * P2P.org rewards history entry
 * Historical reward information for account
 */
data class P2PEthPoolReward(
    val date: DateTime,
    val apy: SerializedBigDecimal,
    val balance: SerializedBigDecimal,
    val rewards: SerializedBigDecimal,
)