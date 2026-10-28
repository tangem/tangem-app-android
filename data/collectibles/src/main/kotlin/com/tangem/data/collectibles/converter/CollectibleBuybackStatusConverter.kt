package com.tangem.data.collectibles.converter

import com.tangem.domain.collectibles.models.CollectibleBuybackStatus
import com.tangem.grow.datasource.collectibles.models.response.BuybackStatusResponse.BuybackStatusDTO
import com.tangem.utils.converter.Converter

internal object CollectibleBuybackStatusConverter : Converter<BuybackStatusDTO, CollectibleBuybackStatus> {

    override fun convert(value: BuybackStatusDTO): CollectibleBuybackStatus = when (value) {
        BuybackStatusDTO.CREATED -> CollectibleBuybackStatus.CREATED
        BuybackStatusDTO.WAITING_TX_HASH -> CollectibleBuybackStatus.WAITING_TX_HASH
        BuybackStatusDTO.BUYBACK_TX_SENT -> CollectibleBuybackStatus.BUYBACK_TX_SENT
        BuybackStatusDTO.CONFIRMING -> CollectibleBuybackStatus.CONFIRMING
        BuybackStatusDTO.CONFIRMED -> CollectibleBuybackStatus.CONFIRMED
        BuybackStatusDTO.EXPIRED -> CollectibleBuybackStatus.EXPIRED
        BuybackStatusDTO.UNKNOWN -> CollectibleBuybackStatus.UNKNOWN
    }
}