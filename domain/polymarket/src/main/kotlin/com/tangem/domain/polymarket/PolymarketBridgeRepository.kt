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

    /**
     * @param depositWalletAddress the wallet pUSD leaves, never the owner address
     * @param recipientAddress the user's own address on the destination chain
     * @return the bridge's `evm` address, whatever the destination chain is
     */
    suspend fun createWithdrawAddress(
        walletId: String,
        depositWalletAddress: String,
        toChainId: String,
        toTokenAddress: String,
        recipientAddress: String,
    ): Either<PolymarketBridgeError, String>
}