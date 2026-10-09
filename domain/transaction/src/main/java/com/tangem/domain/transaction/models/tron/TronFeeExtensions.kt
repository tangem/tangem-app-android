package com.tangem.domain.transaction.models.tron

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee

/** A fee with no estimated energy is not treated as covered: a token transfer always needs energy. */
fun TransactionFee.isCoveredByOwnTronEnergy(): Boolean {
    val tronFee = normal as? Fee.Tron ?: return false
    return tronFee.feeEnergy > 0 && tronFee.remainingEnergy >= tronFee.feeEnergy
}