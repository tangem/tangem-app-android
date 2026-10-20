package com.tangem.data.polymarket.signing

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.data.polymarket.builder.PolymarketTypedDataBuilder
import com.tangem.domain.polymarket.signing.PolymarketApprovalsPayload
import com.tangem.domain.polymarket.signing.PolymarketClobAuthData
import com.tangem.domain.polymarket.signing.PolymarketTypedPayload
import javax.inject.Inject

internal class PolymarketDigestFactory @Inject constructor() {

    fun digest(payload: PolymarketTypedPayload, ownerAddress: String): ByteArray = when (payload) {
        is PolymarketTypedPayload.ClobAuth -> clobAuth(ownerAddress, payload.data)
        is PolymarketTypedPayload.RelayedBatch -> relayedBatch(payload.batch)
    }

    fun clobAuth(ownerAddress: String, data: PolymarketClobAuthData): ByteArray = EthereumUtils.makeTypedDataHash(
        PolymarketTypedDataBuilder.buildClobAuth(
            address = ownerAddress,
            timestamp = data.timestamp,
            nonce = data.nonce,
        ),
    )

    fun relayedBatch(batch: PolymarketApprovalsPayload): ByteArray = EthereumUtils.makeTypedDataHash(
        PolymarketTypedDataBuilder.buildApprovalsBatch(
            depositWallet = batch.depositWalletAddress,
            nonce = batch.nonce,
            deadline = batch.deadline,
            calls = batch.calls,
        ),
    )
}