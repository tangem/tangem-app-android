package com.tangem.features.nft.entity

/**
 * Trigger on success nft send
 */
interface NFTSendSuccessTrigger {
    suspend fun triggerSuccessNFTSend()
}