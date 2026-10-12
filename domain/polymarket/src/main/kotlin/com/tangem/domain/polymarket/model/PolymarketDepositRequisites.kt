package com.tangem.domain.polymarket.model

import com.tangem.domain.polymarket.approval.PolymarketContracts
import java.math.BigDecimal

/**
 * Every way this account can be funded, as the bridge listed them.
 *
 * A network reaches this type only with an address of its own family: the ones whose chain this build does not
 * know, and the ones the bridge issued no address for, are dropped while loading rather than at the screen.
 */
data class PolymarketDepositRequisites(val networks: List<PolymarketDepositNetwork>) {

    /** @return native USDC on Polygon, or `null` when the bridge no longer lists that pair */
    val default: PolymarketDepositTarget? = networks
        .firstOrNull { it.chainId == POLYGON_CHAIN_ID }
        ?.targetFor(tokenAddress = PolymarketContracts.USDC_TOKEN_ADDRESS)

    /**
     * @return the [catalogued][PolymarketDepositAsset.CATALOG] assets the bridge lists with the same contract,
     *  decimals and a known minimum, in catalogue order
     */
    val options: List<PolymarketDepositOption> = PolymarketDepositAsset.CATALOG.mapNotNull { asset ->
        val network = networks.firstOrNull { it.chainId == asset.chainId && it.family == asset.family }
            ?: return@mapNotNull null
        val token = network.tokens.firstOrNull { token ->
            asset.isSameContract(token.address) && token.decimals == asset.decimals && token.minCheckoutUsd != null
        } ?: return@mapNotNull null

        PolymarketDepositOption(asset = asset, target = PolymarketDepositTarget(network = network, token = token))
    }

    private companion object {
        val POLYGON_CHAIN_ID: String = PolymarketContracts.CHAIN_ID.toString()
    }
}

data class PolymarketDepositNetwork(
    val chainId: String,
    val chainName: String,
    val family: PolymarketChainFamily,
    val address: String,
    val tokens: List<PolymarketDepositToken>,
) {

    fun targetFor(tokenAddress: String): PolymarketDepositTarget? = tokens
        .firstOrNull { it.address.equals(tokenAddress, ignoreCase = true) }
        ?.let { PolymarketDepositTarget(network = this, token = it) }
}

data class PolymarketDepositToken(
    val symbol: String,
    val address: String,
    val decimals: Int,
    val minCheckoutUsd: BigDecimal?,
)

data class PolymarketDepositTarget(
    val network: PolymarketDepositNetwork,
    val token: PolymarketDepositToken,
) {

    val address: String get() = network.address
}

data class PolymarketDepositOption(
    val asset: PolymarketDepositAsset,
    val target: PolymarketDepositTarget,
)