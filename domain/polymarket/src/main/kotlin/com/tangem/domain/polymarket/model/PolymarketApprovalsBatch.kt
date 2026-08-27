package com.tangem.domain.polymarket.model

/**
 * The fully-signed 6-approval batch relayed via `POST /wallet/approvals`. All values are exactly the
 * ones the owner signed with the card — the BFF forwards them to the relayer as-is.
 */
data class PolymarketApprovalsBatch(
    val ownerAddress: String,
    val depositWalletAddress: String,
    val nonce: String,
    val deadline: String,
    val calls: List<PolymarketApprovalCall>,
    val signature: String,
)

/** A single allowance call inside a [PolymarketApprovalsBatch]. */
data class PolymarketApprovalCall(
    val target: String,
    val value: String,
    val data: String,
)