package com.tangem.domain.polymarket.approval

import com.tangem.domain.polymarket.model.PolymarketApprovalCall

/**
 * The 17 onboarding allowance calls the BFF's approvals validator accepts: 8 ERC-20 `approve` grants on the
 * collateral, 7 ERC-1155 `setApprovalForAll` grants on the conditional tokens and 2 on the V2 position manager.
 *
 * The set is defined by the backend, not by us — a batch it does not recognise is rejected with
 * `invalid approvals batch`, whatever the relayer would have accepted. It is **not pinned**: it was 6 calls
 * with a different spender until 2026-08-11 and 13 until the V2 exchange release, so treat a `400` here as
 * "the set moved", not as a client bug.
 *
 * The validator compares the decoded `{target, selector, spender}` triples as a **set**, so the order below
 * is not its requirement. The first 13 follow the order Polymarket's own client wrote on chain (80 consecutive
 * deposit-wallet approval transactions on Polygon used exactly this sequence); the four V2 grants follow them.
 *
 * Each call's [PolymarketApprovalCall.data] is ABI-encoded from [PolymarketContracts] on every `build()`, so
 * a spender address is never duplicated as a literal hex blob.
 */
object PolymarketApprovalCalls {

    private const val APPROVE_SELECTOR = "0x095ea7b3"
    private const val SET_APPROVAL_FOR_ALL_SELECTOR = "0xa22cb465"
    private const val MAX_UINT256 = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
    private const val BOOL_TRUE = "0000000000000000000000000000000000000000000000000000000000000001"
    private const val WORD_HEX_LENGTH = 64

    fun build(): List<PolymarketApprovalCall> = listOf(
        approve(PolymarketContracts.CONDITIONAL_TOKENS),
        approve(PolymarketContracts.CTF_EXCHANGE),
        setApprovalForAll(PolymarketContracts.CONDITIONAL_TOKENS, PolymarketContracts.CTF_EXCHANGE),
        approve(PolymarketContracts.NEG_RISK_CTF_EXCHANGE),
        approve(PolymarketContracts.NEG_RISK_ADAPTER),
        setApprovalForAll(PolymarketContracts.CONDITIONAL_TOKENS, PolymarketContracts.NEG_RISK_CTF_EXCHANGE),
        setApprovalForAll(PolymarketContracts.CONDITIONAL_TOKENS, PolymarketContracts.NEG_RISK_ADAPTER),
        approve(PolymarketContracts.CTF_COLLATERAL_ADAPTER),
        approve(PolymarketContracts.NEG_RISK_CTF_COLLATERAL_ADAPTER),
        setApprovalForAll(PolymarketContracts.CONDITIONAL_TOKENS, PolymarketContracts.CTF_COLLATERAL_ADAPTER),
        setApprovalForAll(PolymarketContracts.CONDITIONAL_TOKENS, PolymarketContracts.NEG_RISK_CTF_COLLATERAL_ADAPTER),
        approve(PolymarketContracts.EXCHANGE_V3),
        approve(PolymarketContracts.ROUTER_V3),
        setApprovalForAll(PolymarketContracts.POSITION_MANAGER, PolymarketContracts.EXCHANGE_V3),
        setApprovalForAll(PolymarketContracts.POSITION_MANAGER, PolymarketContracts.ROUTER_V3),
        setApprovalForAll(PolymarketContracts.CONDITIONAL_TOKENS, PolymarketContracts.BINARY_MODULE),
        setApprovalForAll(PolymarketContracts.CONDITIONAL_TOKENS, PolymarketContracts.NEG_RISK_MODULE),
    )

    private fun approve(spender: String) = PolymarketApprovalCall(
        target = PolymarketContracts.COLLATERAL,
        value = "0",
        data = APPROVE_SELECTOR + leftPad32(spender) + MAX_UINT256,
    )

    private fun setApprovalForAll(token: String, operator: String) = PolymarketApprovalCall(
        target = token,
        value = "0",
        data = SET_APPROVAL_FOR_ALL_SELECTOR + leftPad32(operator) + BOOL_TRUE,
    )

    private fun leftPad32(address: String): String =
        address.removePrefix("0x").lowercase().padStart(WORD_HEX_LENGTH, '0')
}