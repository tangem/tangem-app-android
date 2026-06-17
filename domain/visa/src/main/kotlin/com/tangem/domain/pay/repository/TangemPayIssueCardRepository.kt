package com.tangem.domain.pay.repository

import com.tangem.domain.models.wallet.UserWalletId

/**
 * Tracks in-flight additional-card issuance orders locally.
 *
 * The backend does not return a card while it is still being issued, so the issuance order id is
 * stored locally to surface an "issuing" placeholder card until the order reaches a terminal state.
 */
interface TangemPayIssueCardRepository {

    suspend fun storeIssueOrderId(userWalletId: UserWalletId, orderId: String)

    suspend fun getIssueOrderIds(userWalletId: UserWalletId): List<String>

    suspend fun removeIssueOrderId(userWalletId: UserWalletId, orderId: String)
}