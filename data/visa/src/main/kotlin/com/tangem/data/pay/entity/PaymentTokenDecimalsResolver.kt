package com.tangem.data.pay.entity

import com.tangem.blockchain.common.Blockchain

/**
 * Decimals for the payment-account stablecoins (USDC/USDT) per network.
 *
 * The backend `networks[]` payload carries no token decimals yet (client-side table until it does),
 * and decimals differ by network: the launch stablecoins use 6 everywhere except BNB Smart Chain,
 * whose Binance-Peg USDC/USDT are 18-decimal tokens. Once the backend starts sending decimals, this
 * table becomes the fallback for payloads that omit them.
 */
internal object PaymentTokenDecimalsResolver {

    private const val DEFAULT_STABLECOIN_DECIMALS = 6
    private const val BSC_STABLECOIN_DECIMALS = 18

    fun decimalsFor(blockchain: Blockchain): Int = when (blockchain) {
        Blockchain.BSC, Blockchain.BSCTestnet -> BSC_STABLECOIN_DECIMALS
        else -> DEFAULT_STABLECOIN_DECIMALS
    }
}