package com.tangem.lib.crypto

import com.tangem.blockchain.blockchains.xrp.XrpAddressService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.isSupportedInApp
import com.tangem.lib.crypto.converter.XrpTaggedAddressConverter
import com.tangem.lib.crypto.models.XrpTaggedAddress

/**
 * !!!IMPORTANT!!!
 * Methods for working with different blockchains
 * All methods are depend on specific blockchain or check for specific blockchain
 *
 * Temporary solution for domain specific logic for Blockchain.
 * Instead of creating repositories and unnecessary and overkill use cases
 */
object BlockchainUtils {

    private const val XRP_X_ADDRESS = 'X'

    /** Decodes XRP Blockchain address */
    fun decodeRippleXAddress(xAddress: String, networkId: String): XrpTaggedAddress? {
        return if (networkId == Blockchain.XRP.id && xAddress.firstOrNull() == XRP_X_ADDRESS) {
            val decodedAddress = XrpAddressService.decodeXAddress(xAddress)
            return decodedAddress?.let(XrpTaggedAddressConverter()::convert)
        } else {
            null
        }
    }

    /** If current [networkId] is Bitcoin */
    fun isBitcoin(networkId: String): Boolean {
        val blockchain = Blockchain.fromId(networkId)
        return blockchain == Blockchain.Bitcoin || blockchain == Blockchain.BitcoinTestnet
    }

    /** If current [networkId] is Tezos */
    fun isTezos(networkId: String): Boolean {
        val blockchain = Blockchain.fromId(networkId)
        return blockchain == Blockchain.Tezos
    }

    fun isCardano(networkId: String): Boolean {
        val blockchain = Blockchain.fromId(networkId)
        return blockchain == Blockchain.Cardano
    }

    /** If current [networkId] is BeaconChain */
    fun isBeaconChain(networkId: String): Boolean {
        val blockchain = Blockchain.fromId(networkId)
        return blockchain == Blockchain.Binance || blockchain == Blockchain.BinanceTestnet
    }

    fun isSupportedNetworkId(networkId: String): Boolean {
        return Blockchain.fromNetworkId(networkId)?.isSupportedInApp() ?: false
    }

    data class BlockchainInfo(
        val blockchainId: String,
        val name: String,
        val protocolName: String,
    )

    fun getNetworkInfo(networkId: String): BlockchainInfo? {
        val blockchain = Blockchain.fromNetworkId(networkId) ?: return null

        return BlockchainInfo(
            blockchainId = blockchain.id,
            name = getNetworkNameWithoutTestnet(blockchain),
            protocolName = getNetworkStandardName(blockchain),
        )
    }

    private fun getNetworkStandardName(blockchain: Blockchain): String {
        return when (blockchain) {
            Blockchain.Ethereum, Blockchain.EthereumTestnet -> "ERC20"
            Blockchain.BSC, Blockchain.BSCTestnet -> "BEP20"
            Blockchain.Binance, Blockchain.BinanceTestnet -> "BEP2"
            Blockchain.Tron, Blockchain.TronTestnet -> "TRC20"
            Blockchain.TON -> "TON"
            else -> ""
        }
    }

    private fun getNetworkNameWithoutTestnet(blockchain: Blockchain): String {
        return blockchain.getNetworkName().replace(oldValue = " Testnet", newValue = "")
    }
}
