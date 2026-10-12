package com.tangem.domain.polymarket.model

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.polymarket.approval.PolymarketContracts

/** @property chainId the chain as the bridge names it in `/supported-assets` */
data class PolymarketDepositAsset(
    val blockchain: Blockchain,
    val chainId: String,
    val contractAddress: String,
    val decimals: Int,
) {

    val family: PolymarketChainFamily? get() = PolymarketChainFamily.of(chainId)

    fun isSameContract(address: String): Boolean = when (family) {
        PolymarketChainFamily.EVM -> contractAddress.equals(address, ignoreCase = true)
        else -> contractAddress == address
    }

    fun isAssetOf(currency: CryptoCurrency): Boolean = currency is CryptoCurrency.Token &&
        Blockchain.fromNetworkId(currency.network.rawId) == blockchain &&
        isSameContract(currency.contractAddress)

    companion object {

        val POLYGON_USDC = PolymarketDepositAsset(
            blockchain = Blockchain.Polygon,
            chainId = PolymarketContracts.CHAIN_ID.toString(),
            contractAddress = PolymarketContracts.USDC_TOKEN_ADDRESS,
            decimals = 6,
        )

        val CATALOG: List<PolymarketDepositAsset> = listOf(
            POLYGON_USDC,
            PolymarketDepositAsset(
                blockchain = Blockchain.Ethereum,
                chainId = "1",
                contractAddress = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48",
                decimals = 6,
            ),
            PolymarketDepositAsset(
                blockchain = Blockchain.Base,
                chainId = "8453",
                contractAddress = "0x833589fcd6edb6e08f4c7c32d4f71b54bda02913",
                decimals = 6,
            ),
            PolymarketDepositAsset(
                blockchain = Blockchain.Arbitrum,
                chainId = "42161",
                contractAddress = "0xaf88d065e77c8cc2239327c5edb3a432268e5831",
                decimals = 6,
            ),
            PolymarketDepositAsset(
                blockchain = Blockchain.Optimism,
                chainId = "10",
                contractAddress = "0x0b2c639c533813f4aa9d7837caf62653d097ff85",
                decimals = 6,
            ),
            PolymarketDepositAsset(
                blockchain = Blockchain.BSC,
                chainId = "56",
                contractAddress = "0x8ac76a51cc950d9822d68b83fe1ad97b32cd580d",
                decimals = 18,
            ),
            PolymarketDepositAsset(
                blockchain = Blockchain.Monad,
                chainId = "143",
                contractAddress = "0x754704bc059f8c67012fed69bc8a327a5aafb603",
                decimals = 6,
            ),
            PolymarketDepositAsset(
                blockchain = Blockchain.Hyperliquid,
                chainId = "999",
                contractAddress = "0xb88339cb7199b77e23db6e890353e22632ba630f",
                decimals = 6,
            ),
            PolymarketDepositAsset(
                blockchain = Blockchain.Solana,
                chainId = "1151111081099710",
                contractAddress = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
                decimals = 6,
            ),
        )
    }
}