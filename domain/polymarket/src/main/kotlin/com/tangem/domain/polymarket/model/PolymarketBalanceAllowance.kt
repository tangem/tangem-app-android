package com.tangem.domain.polymarket.model

import java.math.BigDecimal

/**
 * Collateral balance and allowance of a deposit wallet, as the CLOB currently sees them.
 *
 * @property balance   spendable collateral
 * @property allowance collateral the exchange is allowed to move; the approvals batch grants an effectively
 *  unbounded amount, so a healthy wallet reports a very large number here. `null` when the CLOB did not
 *  report one at all, which is not the same as an allowance of zero — zero means the exchange may move
 *  nothing, so a caller gating on the approval must treat the two apart.
 */
data class PolymarketBalanceAllowance(
    val balance: BigDecimal,
    val allowance: BigDecimal?,
)