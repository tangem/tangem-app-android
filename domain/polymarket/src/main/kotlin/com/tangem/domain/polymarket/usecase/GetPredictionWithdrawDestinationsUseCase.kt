package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.polymarket.PolymarketBridgeRepository
import com.tangem.domain.polymarket.model.PolymarketBridgeError

class GetPredictionWithdrawDestinationsUseCase(
    private val bridgeRepository: PolymarketBridgeRepository,
) {

    /**
     * @param portfolio the currencies the wallet already holds; native coins are never matched
     * @return the ids of [portfolio] tokens the bridge delivers to, matched on (chain id, contract address).
     *  An empty set is the case the caller must handle by offering to add USDC on Polygon.
     */
    suspend operator fun invoke(
        portfolio: List<CryptoCurrency>,
    ): Either<PolymarketBridgeError, Set<CryptoCurrency.ID>> = either {
        val supported = bridgeRepository.getSupportedAssets().bind()
            .map { it.chainId to it.tokenAddress.lowercase() }
            .toSet()

        portfolio.filterIsInstance<CryptoCurrency.Token>()
            .filter { token ->
                val chainId = Blockchain.fromNetworkId(token.network.rawId)?.getChainId()?.toString()
                chainId != null && supported.contains(chainId to token.contractAddress.lowercase())
            }
            .map { it.id }
            .toSet()
    }
}