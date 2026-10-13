package com.tangem.domain.swap.models

import com.tangem.domain.express.models.ExpressRateType

/** A swap an AI agent asked the wallet to make, as Express holds it. */
data class ExchangeIntent(
    val id: String,
    /** The wallet the intent was raised for, as Express stores it. */
    val walletId: String,
    val clientId: String?,
    val sessionId: String?,
    /** An exchange was sent from the intent. Best effort: `false` does not prove nothing was sent. */
    val isHandled: Boolean,
    /** Express sends the amount of one side only; this is the side [amount] belongs to. */
    val amountType: SwapAmountType,
    /** In minor units of the token on the [amountType] side. */
    val amount: String,
    val from: Asset,
    val to: Asset,
    val providerId: String,
    val rateType: ExpressRateType,
    /** Memo or tag of the destination address. */
    val toExtraId: String?,
) {

    data class Asset(
        val network: String,
        /** `"0"` is the network's native coin. */
        val contractAddress: String,
        val decimals: Int,
        /** The sending (from) or receiving (to) wallet address. */
        val address: String?,
    )
}