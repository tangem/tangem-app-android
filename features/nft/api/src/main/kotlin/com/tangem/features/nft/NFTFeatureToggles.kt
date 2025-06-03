package com.tangem.features.nft

interface NFTFeatureToggles {
    val isNFTEnabled: Boolean
    val isNFTEVMEnabled: Boolean
    val isNFTSolanaEnabled: Boolean
    val isNFTMediaContentEnabled: Boolean
}