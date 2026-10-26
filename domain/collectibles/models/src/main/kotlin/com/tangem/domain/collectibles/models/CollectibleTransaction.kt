package com.tangem.domain.collectibles.models

/**
 * @property fromAddress the player for the gacha: the vault, which pays for the spin
 * @property machineId the machine's external id, e.g. `pokemon_50`
 * @property toAddress the pack is opened to this address instead of [fromAddress]; the vault's owner on Day 1
 */
data class GeneratePurchaseTxParams(
    val fromAddress: String,
    val machineId: String,
    val toAddress: String,
    val collectibleTurboMode: CollectibleTurboMode? = null,
)

/**
 * @property buybackAddress auto-sell proceeds are sent here instead of the transaction's `fromAddress`
 */
data class CollectibleTurboMode(
    val isEnabled: Boolean,
    val buybackAddress: String? = null,
)

/**
 * What the backend issued for one spin; the transaction itself is built and signed on the device.
 *
 * @property txId the backend's id of this purchase, the key of every later call
 * @property memo `<slug>-<uuid>`, the gacha's key for this spin, without the `:open` suffix
 * @property amount the pack price in base units of USDC, passed to `purchase_pack` as is
 * @property feeAmount Tangem's fee in base units of USDC, charged on top of [amount]
 */
data class CollectiblePurchaseTx(
    val txId: String,
    val memo: String,
    val amount: Long,
    val feeAmount: Long,
)

/** @property pack the card won, once the backend has it */
data class CollectibleTxState(
    val status: CollectibleTxStatus,
    val pack: OpenedCollectiblePack?,
)

/** Lifecycle of a Collectible Transaction as the backend reports it. */
enum class CollectibleTxStatus {
    CREATED,
    WAITING_TX_HASH,
    PURCHASE_TX_HASH,
    WAITING_FOR_PAYMENT,
    PACK_PURCHASED,
    PACK_OPENING_ERROR,
    PACK_OPENED,
    NFT_RECEIVED,
    AUTO_BUYBACKED,
    MANUALLY_BUYBACKED,
    EXPIRED,
    REFUNDED,
    PAUSED,

    /** A value this app does not know yet; not waited on, the opening tells what it means. */
    UNKNOWN,
    ;

    /** The payment is not settled yet, so the status is still worth polling. */
    val isPaymentInProgress: Boolean
        get() = this == CREATED ||
            this == WAITING_TX_HASH ||
            this == PURCHASE_TX_HASH ||
            this == WAITING_FOR_PAYMENT
}

/**
 * The card a spin won.
 *
 * @property insuredValue USD * 100
 */
data class OpenedCollectiblePack(
    val txId: String,
    val nftAddress: String,
    val imageFrontUrl: String?,
    val imageBackUrl: String?,
    val rarity: CollectibleRarity?,
    val gradingCompany: String?,
    val grade: String?,
    val gradingId: String?,
    val year: String?,
    val set: String?,
    val serialNumber: String?,
    val insuredValue: Long?,
)

/** Refusals that need more than a generic error; anything else surfaces as the original exception. */
sealed class CollectibleException(message: String?) : Exception(message) {

    /** The machine has too many packs currently open. */
    class LowInventory : CollectibleException("Low inventory")
}