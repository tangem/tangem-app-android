package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.polymarket.PolymarketBridgeRepository
import com.tangem.domain.polymarket.PolymarketCollateralCurrencyFactory
import com.tangem.domain.polymarket.model.PolymarketBridgeAsset
import com.tangem.domain.polymarket.model.PolymarketBridgeError
import com.tangem.domain.polymarket.model.PredictionWithdrawDestination

class GetPredictionWithdrawDestinationsUseCase(
    private val bridgeRepository: PolymarketBridgeRepository,
    private val collateralCurrencyFactory: PolymarketCollateralCurrencyFactory,
) {

    /**
     * @param portfolio the currencies the wallet already holds
     * @return the [portfolio] currencies that are USDC on Polygon — the only destination phase 1 delivers to —
     *  and that the bridge delivers to, each carrying the chain and asset the bridge named it by. An empty map
     *  means the wallet holds nothing deliverable — the caller's case to handle, not an error.
     */
    suspend operator fun invoke(
        portfolio: List<CryptoCurrency>,
    ): Either<PolymarketBridgeError, Map<CryptoCurrency.ID, PredictionWithdrawDestination>> = either {
        val assets = bridgeRepository.getSupportedAssets().bind().associateBy { it.matchKey() }
        val target = collateralCurrencyFactory.createDepositTarget()

        portfolio.filter { it.isDepositTarget(target) }.mapNotNull { currency ->
            val asset = currency.matchKey()?.let(assets::get) ?: return@mapNotNull null

            currency.id to PredictionWithdrawDestination(
                currencyId = currency.id,
                chainId = asset.chainId,
                tokenAddress = asset.tokenAddress,
            )
        }.toMap()
    }

    private fun CryptoCurrency.isDepositTarget(target: CryptoCurrency.Token): Boolean = this is CryptoCurrency.Token &&
        network.rawId == target.network.rawId &&
        contractAddress.equals(target.contractAddress, ignoreCase = true)

    private fun PolymarketBridgeAsset.matchKey(): MatchKey {
        val chain = chainName.takeIf { it in NON_EVM_CHAIN_NAMES } ?: chainId
        return MatchKey(chain = chain, asset = assetKey(chain = chain, address = tokenAddress))
    }

    private fun CryptoCurrency.matchKey(): MatchKey? {
        val chain = Blockchain.fromNetworkId(network.rawId)?.chainKey() ?: return null
        return when (this) {
            is CryptoCurrency.Coin -> MatchKey(chain = chain, asset = NATIVE_ASSET_KEY)
            is CryptoCurrency.Token -> MatchKey(
                chain = chain,
                asset = assetKey(chain = chain, address = contractAddress),
            )
        }
    }

    private fun Blockchain.chainKey(): String? = when (this) {
        Blockchain.Solana -> SOLANA_CHAIN_NAME
        Blockchain.Bitcoin -> BITCOIN_CHAIN_NAME
        Blockchain.Tron -> TRON_CHAIN_NAME
        else -> getChainId()?.toString()
    }

    private fun assetKey(chain: String, address: String): String = when {
        address.equals(NATIVE_ASSET_ADDRESS, ignoreCase = true) -> NATIVE_ASSET_KEY
        chain in NON_EVM_CHAIN_NAMES -> address
        else -> address.lowercase()
    }

    private data class MatchKey(val chain: String, val asset: String)

    private companion object {
        const val SOLANA_CHAIN_NAME = "Solana"
        const val BITCOIN_CHAIN_NAME = "Bitcoin"
        const val TRON_CHAIN_NAME = "Tron"
        const val NATIVE_ASSET_ADDRESS = "0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE"
        const val NATIVE_ASSET_KEY = "native"

        val NON_EVM_CHAIN_NAMES = setOf(SOLANA_CHAIN_NAME, BITCOIN_CHAIN_NAME, TRON_CHAIN_NAME)
    }
}