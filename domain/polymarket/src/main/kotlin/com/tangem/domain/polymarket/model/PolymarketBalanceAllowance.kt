package com.tangem.domain.polymarket.model

import java.math.BigDecimal

/**
 * Collateral balance and allowance of a deposit wallet, as the CLOB currently sees them.
 *
 * @property balance   spendable collateral, in pUSD
 * @property allowance collateral the exchange is allowed to move, in pUSD; the approvals batch grants an
 *  effectively unbounded amount, so a healthy wallet reports a very large number here
 */
data class PolymarketBalanceAllowance(
    val balance: BigDecimal,
    val allowance: BigDecimal,
)