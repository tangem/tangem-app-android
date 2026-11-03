package com.tangem.domain.transaction.models

import com.tangem.blockchain.common.TransactionData

/**
 * ERC-20 approve executed in the same gasless batch right before the user's main transaction.
 *
 * @property spenderAddress      contract the allowance is granted to; the main transaction's gas is estimated
 *                               with this allowance overridden, since the approve is not mined yet
 * @property approvalTransaction approve call targeting the token contract
 */
data class GaslessApproval(
    val spenderAddress: String,
    val approvalTransaction: TransactionData.Uncompiled,
)