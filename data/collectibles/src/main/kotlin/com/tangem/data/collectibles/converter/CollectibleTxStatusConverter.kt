package com.tangem.data.collectibles.converter

import com.tangem.data.collectibles.error.CollectiblesErrorCodes
import com.tangem.domain.collectibles.models.CollectibleTxStatus
import com.tangem.grow.datasource.collectibles.models.response.CollectibleTxResponse
import com.tangem.grow.datasource.collectibles.models.response.CollectibleTxResponse.CollectibleTxStatusDTO
import com.tangem.utils.converter.Converter

/** A `pack-opening-error` is a refund when the backend says so with its refund code, a failed opening otherwise. */
internal object CollectibleTxStatusConverter : Converter<CollectibleTxResponse, CollectibleTxStatus> {

    override fun convert(value: CollectibleTxResponse): CollectibleTxStatus {
        val status = convertStatus(value.status)
        val isRefund = status == CollectibleTxStatus.PACK_OPENING_ERROR &&
            value.packOpeningErrorCode?.toIntOrNull() == CollectiblesErrorCodes.REFUND_IN_PROGRESS

        return if (isRefund) CollectibleTxStatus.REFUNDING else status
    }

    @Suppress("CyclomaticComplexMethod")
    private fun convertStatus(value: CollectibleTxStatusDTO): CollectibleTxStatus = when (value) {
        CollectibleTxStatusDTO.CREATED -> CollectibleTxStatus.CREATED
        CollectibleTxStatusDTO.WAITING_TX_HASH -> CollectibleTxStatus.WAITING_TX_HASH
        CollectibleTxStatusDTO.PURCHASE_TX_SENT -> CollectibleTxStatus.PURCHASE_TX_SENT
        CollectibleTxStatusDTO.WAITING_FOR_PAYMENT -> CollectibleTxStatus.WAITING_FOR_PAYMENT
        CollectibleTxStatusDTO.PACK_PURCHASED -> CollectibleTxStatus.PACK_PURCHASED
        CollectibleTxStatusDTO.PACK_OPENING_ERROR -> CollectibleTxStatus.PACK_OPENING_ERROR
        CollectibleTxStatusDTO.PACK_OPENED -> CollectibleTxStatus.PACK_OPENED
        CollectibleTxStatusDTO.NFT_RECEIVED -> CollectibleTxStatus.NFT_RECEIVED
        CollectibleTxStatusDTO.AUTO_BUYBACKED -> CollectibleTxStatus.AUTO_BUYBACKED
        CollectibleTxStatusDTO.MANUALLY_BUYBACKED -> CollectibleTxStatus.MANUALLY_BUYBACKED
        CollectibleTxStatusDTO.EXPIRED -> CollectibleTxStatus.EXPIRED
        CollectibleTxStatusDTO.REFUNDED -> CollectibleTxStatus.REFUNDED
        CollectibleTxStatusDTO.PAUSED -> CollectibleTxStatus.PAUSED
        CollectibleTxStatusDTO.UNKNOWN -> CollectibleTxStatus.UNKNOWN
    }
}