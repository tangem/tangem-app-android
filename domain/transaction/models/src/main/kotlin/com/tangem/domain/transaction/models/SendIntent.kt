package com.tangem.domain.transaction.models

import java.math.BigDecimal

/** A transfer an AI agent asked the wallet to make, as the Transaction Service holds it. */
data class SendIntent(
    val id: String,
    /** The agent that raised the intent; the service may not know it. */
    val clientId: String?,
    val sessionId: String,
    /**

     * counts, so a status the app does not know yet stops the offer as well.
     */
    val isHandled: Boolean,
    /** Backend id of the network, e.g. `polygon-pos`. */
    val networkId: String,
    /** Backend id of the coin or token to send, e.g. `tether`. */
    val tokenId: String,
    /** The response names no wallet: this address is what tells the wallet and the account the intent is for. */
    val fromAddress: String,
    val toAddress: String,
    /** In whole units of the token, unlike the minor units of an exchange intent. */
    val amount: BigDecimal,
    /** Memo or tag of the destination address. */
    val memo: String?,
    /** What the agent said about the transfer. */
    val comment: String?,
)