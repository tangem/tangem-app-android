package com.tangem.domain.polymarket

import arrow.core.Either
import com.tangem.domain.polymarket.model.PolymarketBridgeAsset
import com.tangem.domain.polymarket.model.PolymarketBridgeError

/**
 * Polymarket's own deposit and withdrawal service. Funding a deposit wallet goes through it exclusively —
 * a transfer sent straight to the deposit wallet is not converted to the trading collateral and never
 * reaches the balance.
 */
interface PolymarketBridgeRepository {

    /**
     * @return what the bridge accepts right now. The list changes, so it is read before every deposit rather
     *  than cached: offering a combination absent from it can lose the funds irrecoverably.
     */
    suspend fun getSupportedAssets(): Either<PolymarketBridgeError, List<PolymarketBridgeAsset>>

    /**
     * @param walletId Tangem wallet id, 64 hex without the `0x` prefix
     * @param depositWalletAddress the wallet the funds are credited to, never the owner address
     * @return the EVM address to send the deposit to
     */
    suspend fun createDepositAddress(
        walletId: String,
        depositWalletAddress: String,
    ): Either<PolymarketBridgeError, String>
}