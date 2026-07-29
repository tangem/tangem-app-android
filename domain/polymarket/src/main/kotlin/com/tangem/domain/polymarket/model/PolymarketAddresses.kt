package com.tangem.domain.polymarket.model

import com.tangem.domain.models.wallet.UserWalletId

/**
 * The owner EOA and the deposit wallet derived from it, scoped to the wallet they were derived for.
 * The two addresses always travel together so that every consumer signs, deploys and verifies against
 * the same pair, and [userWalletId] prevents a pair derived for one wallet from being reused after the
 * user switches to another.
 */
@ConsistentCopyVisibility
data class PolymarketAddresses internal constructor(
    val ownerAddress: String,
    val depositWalletAddress: String,
    val userWalletId: UserWalletId,
)