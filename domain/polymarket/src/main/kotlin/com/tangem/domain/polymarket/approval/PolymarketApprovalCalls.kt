package com.tangem.domain.polymarket.approval

import com.tangem.domain.polymarket.model.PolymarketApprovalCall

/**
 * The 6 canonical onboarding allowance calls, in the strict order the relayer expects (kb/07 Appendix D).
 * Each call's [PolymarketApprovalCall.data] is ABI-encoded from [PolymarketContracts] on each `build()` call, so the
 * spender address is never duplicated as a literal hex blob.
 */
object PolymarketApprovalCalls {

    private const val APPROVE_SELECTOR = "0x095ea7b3"
    private const val SET_APPROVAL_FOR_ALL_SELECTOR = "0xa22cb465"
    private const val MAX_UINT256 = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
    private const val BOOL_TRUE = "0000000000000000000000000000000000000000000000000000000000000001"
    private const val WORD_HEX_LENGTH = 64

    fun build(): List<PolymarketApprovalCall> = listOf(
        approve(spender = PolymarketContracts.CTF_EXCHANGE),
        setApprovalForAll(operator = PolymarketContracts.CTF_EXCHANGE),
        approve(spender = PolymarketContracts.NEG_RISK_CTF_EXCHANGE),
        setApprovalForAll(operator = PolymarketContracts.NEG_RISK_CTF_EXCHANGE),
        approve(spender = PolymarketContracts.NEG_RISK_ADAPTER),
        setApprovalForAll(operator = PolymarketContracts.NEG_RISK_ADAPTER),
    )

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