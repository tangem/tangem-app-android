package com.tangem.data.collectibles.converter

import com.tangem.domain.collectibles.models.CollectibleTxStatus
import com.tangem.utils.converter.Converter

/** A state this app does not know yet is [CollectibleTxStatus.UNKNOWN], not a failure. */
internal object CollectibleTxStatusConverter : Converter<String, CollectibleTxStatus> {

    private val byWireValue = mapOf(
        "created" to CollectibleTxStatus.CREATED,
        "waiting-tx-hash" to CollectibleTxStatus.WAITING_TX_HASH,
        "purchase-tx-hash" to CollectibleTxStatus.PURCHASE_TX_HASH,
        "waiting-for-payment" to CollectibleTxStatus.WAITING_FOR_PAYMENT,
        "pack-purchased" to CollectibleTxStatus.PACK_PURCHASED,
        "pack-opening-error" to CollectibleTxStatus.PACK_OPENING_ERROR,
        "pack-opened" to CollectibleTxStatus.PACK_OPENED,
        "nft-received" to CollectibleTxStatus.NFT_RECEIVED,
        "auto-buybacked" to CollectibleTxStatus.AUTO_BUYBACKED,
        "manually-buybacked" to CollectibleTxStatus.MANUALLY_BUYBACKED,
        "expired" to CollectibleTxStatus.EXPIRED,
        "refunded" to CollectibleTxStatus.REFUNDED,
        "paused" to CollectibleTxStatus.PAUSED,
    )

    override fun convert(value: String): CollectibleTxStatus = byWireValue[value] ?: CollectibleTxStatus.UNKNOWN
}