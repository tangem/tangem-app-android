package com.tangem.data.swap.converter.transaction

import com.tangem.data.swap.models.SavedSwapStatus
import com.tangem.data.swap.models.SwapStatusDTO
import com.tangem.domain.swap.models.SwapStatus
import com.tangem.domain.swap.models.SwapStatusModel
import com.tangem.utils.converter.TwoWayConverter

internal class SavedSwapStatusConverter : TwoWayConverter<SwapStatusDTO, SwapStatusModel> {

    override fun convert(value: SwapStatusDTO) = SwapStatusModel(
        providerId = value.providerId,
        status = SwapStatus.entries.firstOrNull {
            it.name.lowercase() == value.status?.name?.lowercase()
        },
        txId = value.txExternalId,
        txExternalUrl = value.txExternalUrl,
        txExternalId = value.txExternalId,
        refundNetwork = value.refundNetwork,
        refundContractAddress = value.refundContractAddress,
        createdAt = value.createdAt,
        averageDuration = value.averageDuration,
    )

    override fun convertBack(value: SwapStatusModel) = SwapStatusDTO(
        providerId = value.providerId,
        status = SavedSwapStatus.entries.firstOrNull {
            it.name.lowercase() == value.status?.name?.lowercase()
        },
        txId = value.txExternalId,
        txExternalUrl = value.txExternalUrl,
        txExternalId = value.txExternalId,
        refundNetwork = value.refundNetwork,
        refundContractAddress = value.refundContractAddress,
        createdAt = value.createdAt,
        averageDuration = value.averageDuration,
    )
}