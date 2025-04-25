package com.tangem.datasource.local.nft.converter

import com.tangem.domain.nft.models.NFTAsset
import com.tangem.utils.converter.TwoWayConverter
import com.tangem.blockchain.nft.models.NFTAsset as SdkNFTAsset

object NFTSdkAssetIdentifierConverter : TwoWayConverter<SdkNFTAsset.Identifier, NFTAsset.Identifier> {
    override fun convert(value: SdkNFTAsset.Identifier): NFTAsset.Identifier = when (value) {
        is SdkNFTAsset.Identifier.EVM -> NFTAsset.Identifier.EVM(
            tokenId = value.tokenId,
            tokenAddress = value.tokenAddress,
            contractType = NFTAsset.Identifier.EVM.ContractType.valueOf(value.contractType.name),
        )
        is SdkNFTAsset.Identifier.TON -> NFTAsset.Identifier.TON(
            tokenAddress = value.tokenAddress,
        )
        is SdkNFTAsset.Identifier.Solana -> NFTAsset.Identifier.Solana(
            tokenAddress = value.tokenAddress,
            cnft = value.cnft,
        )
        is SdkNFTAsset.Identifier.Unknown -> NFTAsset.Identifier.Unknown
    }

    override fun convertBack(value: NFTAsset.Identifier): SdkNFTAsset.Identifier = when (value) {
        is NFTAsset.Identifier.EVM -> SdkNFTAsset.Identifier.EVM(
            tokenId = value.tokenId,
            tokenAddress = value.tokenAddress,
            contractType = SdkNFTAsset.Identifier.EVM.ContractType.valueOf(value.contractType.name),
        )
        is NFTAsset.Identifier.TON -> SdkNFTAsset.Identifier.TON(
            tokenAddress = value.tokenAddress,
        )
        is NFTAsset.Identifier.Solana -> SdkNFTAsset.Identifier.Solana(
            tokenAddress = value.tokenAddress,
            cnft = value.cnft,
        )
        is NFTAsset.Identifier.Unknown -> SdkNFTAsset.Identifier.Unknown
    }
}