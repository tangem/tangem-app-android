package com.tangem.features.nft.entity

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Listens to success nft send trigger
 */
interface NFTSendSuccessListener {
    val nftSendSuccessFlow: Flow<Unit>
}

@Singleton
internal class DefaultNFTSendSuccessTrigger @Inject constructor() : NFTSendSuccessTrigger, NFTSendSuccessListener {

    override val nftSendSuccessFlow: SharedFlow<Unit>
        field = MutableSharedFlow<Unit>()

    override suspend fun triggerSuccessNFTSend() {
        nftSendSuccessFlow.emit(Unit)
    }
}