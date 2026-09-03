package com.tangem.domain.polymarket

import arrow.core.Either
import com.tangem.domain.polymarket.model.PolymarketBridgeAddresses
import com.tangem.domain.polymarket.model.PolymarketBridgeAsset
import com.tangem.domain.polymarket.model.PolymarketBridgeError

interface PolymarketBridgeRepository {

    suspend fun getSupportedAssets(): Either<PolymarketBridgeError, List<PolymarketBridgeAsset>>

    /** @param depositWalletAddress the wallet funds are credited to, never the owner address. */
    suspend fun createDepositAddresses(
        walletId: String,
        depositWalletAddress: String,
    ): Either<PolymarketBridgeError, PolymarketBridgeAddresses>
}