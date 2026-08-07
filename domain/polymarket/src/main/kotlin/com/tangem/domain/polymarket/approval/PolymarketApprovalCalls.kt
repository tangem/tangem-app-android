package com.tangem.domain.polymarket.approval

import com.tangem.domain.polymarket.model.PolymarketApprovalCall

/**
 * The 6 onboarding allowance calls the BFF's approvals validator accepts: three spenders, each granted the
 * right to move both the wallet's collateral (ERC-20 `approve`) and its outcome shares (ERC-1155
 * `setApprovalForAll`). The collateral grants come first as a group, then the share grants, in the same
 * spender order.
 *
 * The set is defined by the backend, not by us — a batch it does not recognise is rejected with
 * `invalid approvals batch`, whatever the relayer would have accepted. Each call's
 * [PolymarketApprovalCall.data] is ABI-encoded from [PolymarketContracts] on every `build()`, so a spender
 * address is never duplicated as a literal hex blob.
 */
object PolymarketApprovalCalls {

    private const val APPROVE_SELECTOR = "0x095ea7b3"
    private const val SET_APPROVAL_FOR_ALL_SELECTOR = "0xa22cb465"
    private const val MAX_UINT256 = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
    private const val BOOL_TRUE = "0000000000000000000000000000000000000000000000000000000000000001"
    private const val WORD_HEX_LENGTH = 64

    private val SPENDERS = listOf(
        PolymarketContracts.CTF_EXCHANGE,
        PolymarketContracts.NEG_RISK_CTF_EXCHANGE,
        PolymarketContracts.NEG_RISK_CTF_COLLATERAL_ADAPTER,
    )

    fun build(): List<PolymarketApprovalCall> = SPENDERS.map(::approve) + SPENDERS.map(::setApprovalForAll)

    private fun approve(spender: String) = PolymarketApprovalCall(
        target = PolymarketContracts.COLLATERAL,
        value = "0",
        data = APPROVE_SELECTOR + leftPad32(spender) + MAX_UINT256,
    )

    private fun setApprovalForAll(operator: String) = PolymarketApprovalCall(
        target = PolymarketContracts.CONDITIONAL_TOKENS,
        value = "0",
        data = SET_APPROVAL_FOR_ALL_SELECTOR + leftPad32(operator) + BOOL_TRUE,
    )

    private fun leftPad32(address: String): String =
        address.removePrefix("0x").lowercase().padStart(WORD_HEX_LENGTH, '0')
}