package com.tangem.data.collectibles.converter

import com.tangem.domain.collectibles.models.CollectibleRarity
import com.tangem.domain.collectibles.models.OpenedCollectiblePack
import com.tangem.grow.datasource.collectibles.models.response.CollectibleTxResponse
import com.tangem.utils.converter.Converter

internal object OpenedPackConverter : Converter<OpenedPackConverter.Input, OpenedCollectiblePack> {

    override fun convert(value: Input): OpenedCollectiblePack = with(value.nft) {
        OpenedCollectiblePack(
            txId = value.txId,
            nftAddress = nftAddress,
            imageFrontUrl = imageFront,
            imageBackUrl = imageBack,
            rarity = CollectibleRarity.entries.firstOrNull { it.name.equals(other = rarity, ignoreCase = true) },
            gradingCompany = gradingCompany,
            gradingId = gradingId,
            year = year,
            set = set,
            serialNumber = serialNumber,
            insuredValue = insuredValue,
        )
    }

    data class Input(val txId: String, val nft: CollectibleTxResponse.NftWonDTO)
}