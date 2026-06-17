package com.tangem.datasource.local.visa

import com.tangem.domain.models.wallet.UserWalletId

/**
 * Local store for additional-card issuance order ids.
 *
 * The backend `customer/me` response does not surface a card while it is still being issued, so the
 * order id of an in-flight additional-card issuance is persisted here (per wallet) to render an
 * "issuing" placeholder card until the order reaches a terminal state and the real card appears.
 */
interface TangemPayIssueCardStore {

    suspend fun addIssueOrderId(userWalletId: UserWalletId, orderId: String)

    suspend fun getIssueOrderIds(userWalletId: UserWalletId): List<String>

    suspend fun removeIssueOrderId(userWalletId: UserWalletId, orderId: String)
}