package com.tangem.domain.collectibles.converter

import com.tangem.domain.collectibles.models.CollectibleActiveTx
import com.tangem.domain.collectibles.models.CollectiblePendingPack
import com.tangem.domain.collectibles.models.CollectibleTxStatus
import com.tangem.utils.converter.Converter

/** `null` for a purchase that needs no banner, and for a pack to open whose machine is unknown. */
object CollectiblePendingPackConverter : Converter<CollectibleActiveTx, CollectiblePendingPack?> {

    override fun convert(value: CollectibleActiveTx): CollectiblePendingPack? = when {
        value.status.isPaymentInProgress -> CollectiblePendingPack.PaymentInProgress(txId = value.txId)
        value.status == CollectibleTxStatus.PACK_PURCHASED -> value.machineId?.let { machineId ->
            CollectiblePendingPack.ReadyToOpen(
                txId = value.txId,
                machineId = machineId,
                isTurboModeEnabled = value.isTurboModeEnabled,
            )
        }
        value.status == CollectibleTxStatus.PACK_OPENING_ERROR -> CollectiblePendingPack.OpenFailed(txId = value.txId)
        value.status == CollectibleTxStatus.REFUNDING -> CollectiblePendingPack.Refunding(txId = value.txId)
        else -> null
    }
}