package com.tangem.domain.collectibles.models

import java.math.BigDecimal
import java.time.Instant

/** @property amount the buyback price in USD; the wire format is cents */
data class CollectibleBuybackOffer(
    val amount: BigDecimal,
    val expiresAt: Instant?,
)

/**
 * @property fromAddress the owner key that holds the won card; Collector Crypt's `playerAddress`
 * @property nftAddress the mint address of the card being sold back
 * @property toAddress the user's USDC vault, Collector Crypt's `altRecipient`: the payout lands where the spins
 *   are paid from, so it shows in the collectibles balance
 */
data class GenerateBuybackTxParams(
    val fromAddress: String,
    val nftAddress: String,
    val toAddress: String,
)

/**
 * @property amount the payout in whole units of the received token
 * @property transaction base64 wire bytes as the backend sent them, signed on the device as is
 */
data class CollectibleBuybackTx(
    val buybackId: String,
    val memo: String,
    val amount: BigDecimal,
    val transaction: String,
)

/** Lifecycle of a buyback as the backend reports it. */
enum class CollectibleBuybackStatus {
    CREATED,
    WAITING_TX_HASH,
    BUYBACK_TX_SENT,
    CONFIRMING,
    CONFIRMED,
    EXPIRED,

    /** A value this app does not know yet; the status poll keeps going. */
    UNKNOWN,
}