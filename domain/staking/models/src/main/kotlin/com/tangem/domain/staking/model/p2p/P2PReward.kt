package com.tangem.domain.staking.model.p2p

import com.tangem.domain.models.serialization.SerializedBigDecimal
import org.joda.time.DateTime

/**
 * P2P.org rewards history entry
 * Historical reward information for account
 */
data class P2PReward(
    val date: DateTime,
    val apy: SerializedBigDecimal,
    val balance: SerializedBigDecimal,
    val rewards: SerializedBigDecimal,
)