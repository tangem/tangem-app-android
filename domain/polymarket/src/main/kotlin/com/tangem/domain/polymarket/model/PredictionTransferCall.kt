package com.tangem.domain.polymarket.model

import java.math.BigInteger

/**
 * A pUSD `transfer` read back out of the calldata that will be signed, never the values it was meant to
 * carry. Comparing it against the intent is what catches a packing bug before the card signs.
 */
data class PredictionTransferCall(
    val recipient: String,
    val amountBaseUnits: BigInteger,
)

interface PredictionTransferCallCodec {

    fun build(recipient: String, amountBaseUnits: BigInteger): PolymarketApprovalCall

    /** @return null when the call is not the 68-byte pUSD `transfer` this codec produces. */
    fun decode(call: PolymarketApprovalCall): PredictionTransferCall?
}