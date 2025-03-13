package com.tangem.datasource.local.nft.converter

import com.tangem.domain.nft.models.NFTCollection
import com.tangem.utils.converter.TwoWayConverter
import com.tangem.blockchain.nft.models.NFTCollection as SdkNFTCollection

object NFTSdkCollectionIdentifierConverter : TwoWayConverter<SdkNFTCollection.Identifier, NFTCollection.Identifier> {
    override fun convert(value: SdkNFTCollection.Identifier): NFTCollection.Identifier = when (value) {
        is SdkNFTCollection.Identifier.EVM -> NFTCollection.Identifier.EVM(
            tokenAddress = value.tokenAddress,
        )
        is SdkNFTCollection.Identifier.TON -> NFTCollection.Identifier.TON(
            contractAddress = value.contractAddress,
        )
        is SdkNFTCollection.Identifier.Unknown -> NFTCollection.Identifier.Unknown
    }

    override fun convertBack(value: NFTCollection.Identifier): SdkNFTCollection.Identifier = when (value) {
        is NFTCollection.Identifier.EVM -> SdkNFTCollection.Identifier.EVM(
            tokenAddress = value.tokenAddress,
        )
        is NFTCollection.Identifier.TON -> SdkNFTCollection.Identifier.TON(
            contractAddress = value.contractAddress,
        )
        is NFTCollection.Identifier.Unknown -> SdkNFTCollection.Identifier.Unknown
    }
}